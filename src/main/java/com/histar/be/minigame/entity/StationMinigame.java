package com.histar.be.minigame.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "station_minigames")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationMinigame {

    @Id
    private UUID id;

    @Column(name = "site_code", nullable = false)
    @Builder.Default
    private String siteCode = "cu-chi";

    @Column(name = "station_code", nullable = false)
    private String stationCode;

    @Column(name = "game_type", nullable = false)
    private String gameType;

    @Column(nullable = false)
    private String title;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
