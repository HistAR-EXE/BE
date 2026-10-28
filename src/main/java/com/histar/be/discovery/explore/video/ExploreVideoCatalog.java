package com.histar.be.discovery.explore.video;

import com.histar.be.common.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Answers stay in a server-only resource, never serialized into public catalog DTOs. */
@Component
public class ExploreVideoCatalog {
    public static final List<String> IDS = List.of("tunnel-digging", "liberation-gate", "communication-station", "guerrilla-house", "women-house", "war-artifacts", "ho-chi-minh-space");
    public static final int XP = 30;
    public record Entry(String id, String title, double durationSeconds, String question,
                        List<String> options, int correctOption, String explanation) {
        public String key() { return "video:" + id; }
    }
    private final Map<String,Entry> entries;
    public ExploreVideoCatalog() {
        try (var stream = new ClassPathResource("content/cu-chi-video-quizzes.json").getInputStream()) {
            Entry[] data = new ObjectMapper().readValue(stream, Entry[].class);
            var map = new LinkedHashMap<String,Entry>();
            for (var item : data) {
                if (!IDS.contains(item.id()) || item.durationSeconds() <= 0 || item.options().size() != 3
                        || item.correctOption() < 0 || item.correctOption() > 2 || map.put(item.id(),item) != null)
                    throw new IllegalStateException("Invalid reviewed video catalog");
            }
            if (map.size() != IDS.size()) throw new IllegalStateException("Seven reviewed videos are required");
            entries = Collections.unmodifiableMap(map);
        } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot load reviewed Cu Chi video catalog",ex); }
    }
    public List<Entry> all() { return IDS.stream().map(entries::get).toList(); }
    public Entry get(String id) {
        var video=entries.get(id);
        if (video == null) throw new BusinessRuleException("Video không thuộc thư viện Củ Chi.");
        return video;
    }
    public static int xp(String key) {
        return IDS.stream().anyMatch(id -> ("video:"+id).equals(key)) ? XP : 0;
    }
}
