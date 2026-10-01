package com.histar.be.hotspot.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hotspot_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotspotContent {

    @Id
    private String contentRef;

    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String imageUrl;

    private String unlockKey;
}
