package com.histar.be.chat.guided;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Produces suggestions only from server-built journey context, never client-supplied GPS data. */
@Component
public class JourneySuggestionService {

    public List<String> suggest(Map<String, Object> journeyContext) {
        if (journeyContext == null || journeyContext.isEmpty()) {
            return List.of("Bạn muốn khám phá câu chuyện nào của di tích?");
        }
        List<String> suggestions = new ArrayList<>();
        Object nextPoi = journeyContext.get("nextSuggestedPoi");
        if (nextPoi instanceof String poi && !poi.isBlank()) {
            suggestions.add("Hãy thử khám phá " + poi + ".");
        }
        Object currentPoi = journeyContext.get("currentPoi");
        if (currentPoi instanceof String poi && !poi.isBlank()) {
            suggestions.add("Bạn muốn biết thêm điều gì về " + poi + "?");
        }
        return suggestions.isEmpty() ? List.of("Bạn muốn khám phá câu chuyện nào của di tích?") : List.copyOf(suggestions);
    }
}
