package com.histar.be.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OrgInviteEmailRequest(
        @NotBlank @Email String studentEmail, String studentName) {}
