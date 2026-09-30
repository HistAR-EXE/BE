package com.histar.be.stations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "station_content_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    private StationBlockType blockType;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "meta_json", columnDefinition = "TEXT")
    private String metaJson;
}
