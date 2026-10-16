package com.histar.be.artifact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "artifacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artifact {

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

    private UUID locationId;
    private String name;

    @Column(columnDefinition = "text")
    private String imageUrl;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String story;

    private String unlockKey;
    private String reliability;
    private Integer sortOrder;

    @Column(name = "collection_name")
    private String collectionName;

    private String rarity;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "image_source", columnDefinition = "text")
    private String imageSource;

    @Column(name = "image_license", columnDefinition = "text")
    private String imageLicense;

    @Column(name = "fact_verified", nullable = false)
    @Builder.Default
    private Boolean factVerified = false;

    @Column(name = "unlock_minigame_id")
    private UUID unlockMinigameId;
}
