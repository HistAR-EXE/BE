package com.histar.be.stations.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record StationImportItem(
        @NotBlank String code,
        @NotBlank String name,
        Integer sortOrder,
        Double lat,
        Double lng,
        String questStepKey,
        Boolean active,
        @Valid List<StationBlockRequest> blocks) {}
