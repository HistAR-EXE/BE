package com.histar.be.location.entity;

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
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private Double latitude;
    private Double longitude;
    private String city;

    @Column(name = "formatted_address", columnDefinition = "text")
    private String formattedAddress;

    @Column(name = "google_maps_url", columnDefinition = "text")
    private String googleMapsUrl;

    @Column(columnDefinition = "text")
    private String coverImage;

    @Column(columnDefinition = "numeric(2,1)")
    private Double rating;

    @Column(name = "is_ar_available")
    private Boolean isArAvailable;

    @Column(columnDefinition = "text")
    private String sources;

    @Column(name = "knowledge_context", columnDefinition = "text")
    private String knowledgeContext;

    @Column(name = "unlock_prerequisite_quest_id")
    private UUID unlockPrerequisiteQuestId;

    @Column(name = "unlock_narrative", columnDefinition = "text")
    private String unlockNarrative;

    private Instant createdAt;
}
