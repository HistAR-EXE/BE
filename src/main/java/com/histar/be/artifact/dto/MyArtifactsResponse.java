package com.histar.be.artifact.dto;

import java.util.List;

public record MyArtifactsResponse(List<ArtifactResponse> items, int collected, int total) {}
