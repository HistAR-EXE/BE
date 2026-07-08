package com.histar.be.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProvisionStudentRequest(
        @NotBlank @Email String email,
        String displayName,
        Boolean sendCredentialsEmail) {}
