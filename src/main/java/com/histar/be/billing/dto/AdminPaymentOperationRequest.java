package com.histar.be.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPaymentOperationRequest(
        @NotBlank @Size(min = 10) String reason) {}
