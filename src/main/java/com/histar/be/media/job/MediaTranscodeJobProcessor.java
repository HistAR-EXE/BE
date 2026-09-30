package com.histar.be.media.job;

import com.histar.be.media.service.MediaTranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Stub processor: ffmpeg when present, otherwise FAILED_NO_FFMPEG (Plan B client WebM share). */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaTranscodeJobProcessor {

    private final MediaTranscodeService mediaTranscodeService;

    @Scheduled(fixedDelayString = "${media.transcode.poll-ms:15000}", initialDelay = 20000)
    @SchedulerLock(name = "mediaTranscodeProcessor", lockAtMostFor = "PT5M", lockAtLeastFor = "PT5S")
    public void processPending() {
        int n = mediaTranscodeService.processPending();
        if (n > 0) {
            log.info("MediaTranscodeJobProcessor handled {} job(s)", n);
        }
    }
}
