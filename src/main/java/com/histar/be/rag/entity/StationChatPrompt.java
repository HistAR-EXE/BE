package com.histar.be.rag.entity;

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
@Table(name = "station_chat_prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationChatPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "site_code", nullable = false, length = 64)
    private String siteCode;

    @Column(name = "station_code", nullable = false, length = 32)
    private String stationCode;

    @Column(nullable = false, length = 64)
    private String persona;

    @Column(name = "chip_label", nullable = false, length = 80)
    private String chipLabel;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
