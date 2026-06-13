package com.histar.be.discovery.dto;

import java.util.List;

public record DiscoverySummaryResponse(int discovered, int total, List<String> keys, long version) {}
