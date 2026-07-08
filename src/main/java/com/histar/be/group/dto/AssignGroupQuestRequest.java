package com.histar.be.group.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record AssignGroupQuestRequest(@NotNull UUID questId) {}
