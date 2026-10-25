package com.histar.be.fieldtrip.entity;

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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "field_trip_report_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldTripReportSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_trip_id", nullable = false, unique = true)
    private UUID fieldTripId;

    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private Instant generatedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_json", nullable = false, columnDefinition = "jsonb")
    private String reportJson;
}
