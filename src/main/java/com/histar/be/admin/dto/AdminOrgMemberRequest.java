package com.histar.be.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminOrgMemberRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 120) String displayName,
        Boolean sendCredentialsEmail) {}
