package com.histar.be.hotspot.entity;

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
@Table(name = "hotspots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID panoramaId;
    private Double yaw;
    private Double pitch;
    private String type;
    private String contentRef;
    private String label;
}
