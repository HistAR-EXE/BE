package com.histar.be.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record B2b2cInquiryStatusUpdateRequest(
        @NotBlank @Pattern(regexp = "NEW|CONTACTED|QUALIFIED|WON|LOST") String status, String adminNotes) {}
