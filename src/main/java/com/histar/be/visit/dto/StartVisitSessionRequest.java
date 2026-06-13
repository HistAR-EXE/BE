package com.histar.be.visit.dto;

import java.util.UUID;

public record StartVisitSessionRequest(UUID locationId, String mode) {}
