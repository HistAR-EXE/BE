package com.histar.be.events.entity;

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
@Table(name = "pilot_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PilotEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_uuid", unique = true)
    private UUID clientUuid;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "station_code", length = 32)
    private String stationCode;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
