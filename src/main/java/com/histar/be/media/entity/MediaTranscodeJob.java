package com.histar.be.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_transcode_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaTranscodeJob {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    /** Plan B: host has no ffmpeg; client should share the recorded WebM directly. */
    public static final String STATUS_FAILED_NO_FFMPEG = "FAILED_NO_FFMPEG";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "input_format", nullable = false, length = 16)
    @Builder.Default
    private String inputFormat = "webm";

    @Column(name = "output_format", nullable = false, length = 16)
    @Builder.Default
    private String outputFormat = "mp4";

    @Column(name = "input_url", columnDefinition = "TEXT")
    private String inputUrl;

    @Column(name = "output_url", columnDefinition = "TEXT")
    private String outputUrl;

    @Column(name = "input_size_bytes")
    private Long inputSizeBytes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
