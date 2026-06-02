package com.histar.be.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DemoCheckinRequest(@NotNull UUID locationId) {}
