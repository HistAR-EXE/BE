package com.histar.be.quest.narrative.model;

public record NarrativeSource(
        String sourceId,
        String version,
        String locale,
        String citation,
        String url,
        String path) {}
