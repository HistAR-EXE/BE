package com.histar.be.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateB2b2cInquiryRequest(
        @NotBlank @Size(max = 255) String siteName,
        @NotBlank @Size(max = 255) String contactName,
        @NotBlank @Email String contactEmail,
        @Size(max = 64) String contactPhone,
        /** CRM package bucket, e.g. EDU_K12, MUSEUM, ONE_TIME, OPEX */
        @NotBlank @Size(min = 2, max = 32) String packageType,
        @Size(max = 4000) String message,
        @Size(max = 4000) String adminNotes,
        @Size(max = 64) String interestSiteCode,
        @Pattern(regexp = "NEW|CONTACTED|QUALIFIED|WON|LOST") String status) {}
