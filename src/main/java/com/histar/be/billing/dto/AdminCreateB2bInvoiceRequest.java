package com.histar.be.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminCreateB2bInvoiceRequest(
        @Size(max = 255) String orgName,
        @NotBlank @Size(max = 32) String planType,
        @Email @Size(max = 255) String contactEmail,
        UUID organizationId,
        @Min(1) @Max(10_000) Integer licenseCount) {}
