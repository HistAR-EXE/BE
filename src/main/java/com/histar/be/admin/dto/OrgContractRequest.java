package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record OrgContractRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 64) String contractCode,
        Integer licenseSeats,
        @Size(max = 32) String planType,
        @Pattern(regexp = "DRAFT|ACTIVE|EXPIRED|CANCELLED") String status,
        LocalDate startsOn,
        LocalDate endsOn,
        @Size(max = 2000) String documentUrl,
        @Size(max = 4000) String notes) {}
