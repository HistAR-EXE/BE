package com.histar.be.story.entity;

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
@Table(name = "story_chapters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "site_code", nullable = false)
    @Builder.Default
    private String siteCode = "cu-chi";

    @Column(name = "chapter_number", nullable = false)
    private int chapterNumber;

    @Column(name = "station_code", nullable = false)
    private String stationCode;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "requires_premium", nullable = false)
    @Builder.Default
    private boolean requiresPremium = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
