package com.histar.be.discovery.explore.video;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
@Entity
@Table(name="explore_video_progress", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","video_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExploreVideoProgress {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="video_id",nullable=false,length=64) private String videoId;
    @Column(name="watched_seconds",nullable=false) private double watchedSeconds;
    @Column(name="session_id") private UUID sessionId;
    @Column(name="heartbeat_at") private Instant heartbeatAt;
    @Column(name="session_started_at") private Instant sessionStartedAt;
    @Column(name="session_started_seconds",nullable=false) private double sessionStartedSeconds;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
