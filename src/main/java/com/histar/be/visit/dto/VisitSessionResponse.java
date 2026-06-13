package com.histar.be.visit.dto;

import java.util.UUID;

public record VisitSessionResponse(UUID id, UUID locationId, String mode) {}
