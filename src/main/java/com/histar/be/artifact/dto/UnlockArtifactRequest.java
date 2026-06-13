package com.histar.be.artifact.dto;

import jakarta.validation.constraints.NotBlank;

public record UnlockArtifactRequest(@NotBlank String unlockKey) {}
