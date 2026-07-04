package com.histar.be.discovery.dto;

import java.util.List;
import java.util.UUID;

public record VisitedLocationsResponse(List<UUID> visitedLocationIds, int visitedCount) {}
