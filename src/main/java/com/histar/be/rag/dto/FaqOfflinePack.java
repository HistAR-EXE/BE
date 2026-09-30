package com.histar.be.rag.dto;

import java.time.Instant;
import java.util.List;

/** Body of {@code faq_offline.json} cached by the full offline pack. */
public record FaqOfflinePack(String siteCode, Instant generatedAt, List<Item> items) {

    public record Item(String stationCode, String question, String answer, String sourceRefs) {}
}
