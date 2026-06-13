package com.histar.be.discovery.binding.dto;

public record DiscoveryBindingResponse(
        String unlockKey,
        String recordKey,
        String engagement,
        String hrefTemplate,
        int sortOrder) {}
