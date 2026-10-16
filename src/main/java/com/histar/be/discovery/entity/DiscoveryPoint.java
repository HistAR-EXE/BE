package com.histar.be.discovery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "discovery_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveryPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "delete_reason", columnDefinition = "text")
    private String deleteReason;

    @Column(name = "location_id")
    private UUID locationId;

    private String name;

    @Column(name = "map_x_pct")
    private BigDecimal mapXPct;

    @Column(name = "map_y_pct")
    private BigDecimal mapYPct;

    @Column(name = "unlock_key")
    private String unlockKey;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
