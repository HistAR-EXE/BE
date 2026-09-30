package com.histar.be.media.dto;

import com.histar.be.media.entity.MediaTranscodeJob;
import java.time.Instant;
import java.util.UUID;

/**
 * @param clientShareWebm true when server-side conversion is unavailable/failed and the client should
 *     share the locally recorded WebM (Plan B).
 */
public record MediaTranscodeJobResponse(
        UUID id,
        String status,
        String inputFormat,
        String outputFormat,
        String inputUrl,
        String outputUrl,
        String errorMessage,
        boolean clientShareWebm,
        Instant createdAt,
        Instant updatedAt) {

    public static MediaTranscodeJobResponse from(MediaTranscodeJob job) {
        boolean failed = job.getStatus() != null && job.getStatus().startsWith("FAILED");
        return new MediaTranscodeJobResponse(
                job.getId(),
                job.getStatus(),
                job.getInputFormat(),
                job.getOutputFormat(),
                job.getInputUrl(),
                job.getOutputUrl(),
                job.getErrorMessage(),
                failed,
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
