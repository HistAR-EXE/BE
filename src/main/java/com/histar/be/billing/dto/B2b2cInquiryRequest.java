package com.histar.be.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record B2b2cInquiryRequest(
        @NotBlank String siteName,
        @NotBlank String contactName,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        @NotBlank @Pattern(regexp = "ONE_TIME|OPEX") String packageType,
        String message,
        String website,
        String companyUrl,
        /** Optional pilot site code: cu-chi | hoang-thanh-thang-long | dai-noi-hue */
        @Pattern(regexp = "cu-chi|hoang-thanh-thang-long|dai-noi-hue") String interestSiteCode) {}
