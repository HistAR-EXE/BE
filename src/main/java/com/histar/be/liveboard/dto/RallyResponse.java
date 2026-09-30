package com.histar.be.liveboard.dto;

import java.time.Instant;
import java.util.UUID;

/** Payload of the SSE {@code gather} event and the response of {@code POST .../rally}. */
public record RallyResponse(UUID organizationId, Instant rallyAt) {}
