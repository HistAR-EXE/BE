package com.histar.be.checkin.entity;

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
@Table(name = "checkins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private UUID locationId;
    private Double latitude;
    private Double longitude;
    private Instant createdAt;

    @Column(name = "station_code", length = 64)
    private String stationCode;

    @Column(name = "presence_score")
    private Integer presenceScore;

    @Column(name = "presence_method", length = 16)
    private String presenceMethod;

    @Column(name = "client_uuid")
    private UUID clientUuid;
}
