package com.histar.be.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record B2b2cInquiryRequest(
        @NotBlank @Size(max = 255) String siteName,
        @NotBlank @Size(max = 255) String contactName,
        @NotBlank @Email @Size(max = 255) String contactEmail,
        @Size(max = 64) String contactPhone,
        @NotBlank @Pattern(regexp = "ONE_TIME|OPEX|UNDECIDED") String packageType,
        @Size(max = 4000) String message,
        String website,
        String companyUrl,
        /** Optional pilot site code: cu-chi | hoang-thanh-thang-long | dai-noi-hue */
        @Pattern(regexp = "cu-chi|hoang-thanh-thang-long|dai-noi-hue") String interestSiteCode,
        @Size(max = 255) String organizationName,
        @Size(max = 160) String contactRole,
        @Size(max = 255) String siteLocation,
        @Size(max = 4000) String availableMaterials,
        @Size(max = 80) String targetStart) {}
