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
@Table(name = "faq_offline")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqOffline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "site_code", nullable = false, length = 64)
    private String siteCode;

    @Column(name = "station_code", length = 32)
    private String stationCode;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "source_refs", columnDefinition = "TEXT")
    private String sourceRefs;
}
