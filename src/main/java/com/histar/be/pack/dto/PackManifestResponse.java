package com.histar.be.pack.dto;

import java.time.Instant;
import java.util.List;

public record PackManifestResponse(String tier, String siteCode, Instant generatedAt, List<PackAssetDto> assets) {}
