package com.histar.be.panorama.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "panoramas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Panorama {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID locationId;

    @Column(columnDefinition = "text")
    private String imageUrl;

    private String title;

    private String areaSlug;

    private Integer sortOrder;

    private Double defaultYaw;

    private Double defaultPitch;
}
