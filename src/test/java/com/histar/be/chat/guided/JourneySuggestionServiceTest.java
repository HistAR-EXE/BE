package com.histar.be.chat.guided;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JourneySuggestionServiceTest {

    private final JourneySuggestionService service = new JourneySuggestionService();

    @Test
    void suggests_the_next_unvisited_point_from_the_journey_context() {
        List<String> suggestions = service.suggest(Map.of("nextSuggestedPoi", "Hầm bí mật", "currentPoi", "Cổng vào"));

        assertThat(suggestions).anyMatch(suggestion -> suggestion.contains("Hầm bí mật"));
    }
}
