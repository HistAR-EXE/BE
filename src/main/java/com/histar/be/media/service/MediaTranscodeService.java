package com.histar.be.media.service;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.media.dto.MediaTranscodeJobResponse;
import com.histar.be.media.entity.MediaTranscodeJob;
import com.histar.be.media.repository.MediaTranscodeJobRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * C2b: queues WebM -> MP4 transcode jobs for Time Portal clips.
 *
 * <p>Render (free tier) usually has no ffmpeg. In that case the job is marked {@code FAILED_NO_FFMPEG}
 * (or completed as-is when {@code media.transcode.passthrough-when-no-ffmpeg=true}) and the client
 * falls back to sharing the WebM it recorded locally (Plan B).
 */
@Service
@Slf4j
public class MediaTranscodeService {

    private static final byte[] EBML_MAGIC = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};

    private final MediaTranscodeJobRepository jobRepository;
    private final MediaStorageService mediaStorageService;
    private final String ffmpegPath;
    private final boolean passthroughWhenNoFfmpeg;
    private final long maxBytes;
    private final Path workDir;

    private volatile Boolean ffmpegAvailable;

    public MediaTranscodeService(
            MediaTranscodeJobRepository jobRepository,
            MediaStorageService mediaStorageService,
            @Value("${media.transcode.ffmpeg-path:ffmpeg}") String ffmpegPath,
            @Value("${media.transcode.passthrough-when-no-ffmpeg:false}") boolean passthroughWhenNoFfmpeg,
            @Value("${media.transcode.max-bytes:8388608}") long maxBytes) {
        this.jobRepository = jobRepository;
        this.mediaStorageService = mediaStorageService;
        this.ffmpegPath = ffmpegPath;
        this.passthroughWhenNoFfmpeg = passthroughWhenNoFfmpeg;
        this.maxBytes = maxBytes;
        this.workDir = Path.of(System.getProperty("java.io.tmpdir"), "histar-transcode");
    }

    @Transactional
    public MediaTranscodeJobResponse enqueue(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Thiếu file WebM.");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleException("File clip quá lớn (tối đa " + (maxBytes / 1024 / 1024) + "MB).");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessRuleException("Không đọc được file clip.");
        }
        if (!looksLikeWebm(file.getContentType(), file.getOriginalFilename(), data)) {
            throw new BusinessRuleException("Chỉ chấp nhận video WebM.");
        }

        Instant now = Instant.now();
        MediaTranscodeJob job = jobRepository.save(MediaTranscodeJob.builder()
                .userId(userId)
                .status(MediaTranscodeJob.STATUS_PENDING)
                .inputSizeBytes((long) data.length)
                .createdAt(now)
                .updatedAt(now)
                .build());

        try {
            Files.createDirectories(workDir);
            Files.write(inputPath(job.getId()), data);
        } catch (IOException ex) {
            log.warn("Transcode job {}: cannot stage input on disk: {}", job.getId(), ex.getMessage());
        }
        try {
            job.setInputUrl(mediaStorageService.uploadImage(
                    data, "video/webm", "portal-clips/" + job.getId() + ".webm"));
        } catch (RuntimeException ex) {
            log.warn("Transcode job {}: input upload skipped: {}", job.getId(), ex.getMessage());
        }
        return MediaTranscodeJobResponse.from(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public MediaTranscodeJobResponse get(UUID jobId, UUID userId) {
        MediaTranscodeJob job = jobRepository
                .findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcode job not found: " + jobId));
        if (job.getUserId() != null && !job.getUserId().equals(userId) && !isAdmin()) {
            throw new AuthException("Forbidden");
        }
        return MediaTranscodeJobResponse.from(job);
    }

    /** Called by the scheduled processor. */
    @Transactional
    public int processPending() {
        List<MediaTranscodeJob> pending = jobRepository.findTop5ByStatusOrderByCreatedAtAsc(
                MediaTranscodeJob.STATUS_PENDING);
        for (MediaTranscodeJob job : pending) {
            process(job);
        }
        return pending.size();
    }

    private void process(MediaTranscodeJob job) {
        job.setAttempts(job.getAttempts() + 1);
        Path input = inputPath(job.getId());
        try {
            if (!isFfmpegAvailable()) {
                if (passthroughWhenNoFfmpeg && job.getInputUrl() != null) {
                    job.setOutputFormat("webm");
                    job.setOutputUrl(job.getInputUrl());
                    finish(job, MediaTranscodeJob.STATUS_COMPLETED, null);
                } else {
                    finish(job, MediaTranscodeJob.STATUS_FAILED_NO_FFMPEG,
                            "ffmpeg không có trên máy chủ; hãy chia sẻ file WebM trực tiếp từ thiết bị.");
                }
                return;
            }
            if (!Files.exists(input)) {
                finish(job, MediaTranscodeJob.STATUS_FAILED, "Input clip đã hết hạn trên máy chủ.");
                return;
            }
            Path output = workDir.resolve(job.getId() + ".mp4");
            Process process = new ProcessBuilder(
                            ffmpegPath, "-y", "-i", input.toString(), "-c:v", "libx264", "-pix_fmt", "yuv420p",
                            "-movflags", "+faststart", "-an", output.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(90, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                finish(job, MediaTranscodeJob.STATUS_FAILED, "ffmpeg timeout.");
            } else if (process.exitValue() != 0 || !Files.exists(output)) {
                finish(job, MediaTranscodeJob.STATUS_FAILED, "ffmpeg exit code " + process.exitValue());
            } else {
                job.setOutputFormat("mp4");
                job.setOutputUrl(mediaStorageService.uploadImage(
                        Files.readAllBytes(output), "video/mp4", "portal-clips/" + job.getId() + ".mp4"));
                finish(job, MediaTranscodeJob.STATUS_COMPLETED, null);
            }
            Files.deleteIfExists(output);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            finish(job, MediaTranscodeJob.STATUS_FAILED, "Interrupted.");
        } catch (Exception ex) {
            log.warn("Transcode job {} failed: {}", job.getId(), ex.getMessage());
            finish(job, MediaTranscodeJob.STATUS_FAILED, truncate(ex.getMessage()));
        } finally {
            try {
                if (!MediaTranscodeJob.STATUS_PENDING.equals(job.getStatus())) {
                    Files.deleteIfExists(input);
                }
            } catch (IOException ignored) {
                // temp file cleanup is best effort
            }
        }
    }

    private void finish(MediaTranscodeJob job, String status, String error) {
        Instant now = Instant.now();
        job.setStatus(status);
        job.setErrorMessage(error);
        job.setUpdatedAt(now);
        job.setCompletedAt(now);
        jobRepository.save(job);
    }

    private boolean isFfmpegAvailable() {
        Boolean cached = ffmpegAvailable;
        if (cached != null) {
            return cached;
        }
        boolean ok;
        try {
            Process p = new ProcessBuilder(ffmpegPath, "-version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            ok = p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception ex) {
            ok = false;
        }
        ffmpegAvailable = ok;
        log.info("Media transcode: ffmpeg available={}", ok);
        return ok;
    }

    private Path inputPath(UUID jobId) {
        return workDir.resolve(jobId + ".webm");
    }

    private static boolean looksLikeWebm(String contentType, String filename, byte[] data) {
        boolean typeOk = contentType != null && contentType.toLowerCase().startsWith("video/webm");
        boolean nameOk = filename != null && filename.toLowerCase().endsWith(".webm");
        if (!typeOk && !nameOk) {
            return false;
        }
        if (data.length < EBML_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < EBML_MAGIC.length; i++) {
            if (data[i] != EBML_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static String truncate(String s) {
        if (s == null) {
            return "unknown error";
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
