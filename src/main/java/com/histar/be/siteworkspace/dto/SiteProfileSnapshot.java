package com.histar.be.siteworkspace.dto;

public record SiteProfileSnapshot(
        String name, String description, String formattedAddress, String coverImage, String sources) {}
