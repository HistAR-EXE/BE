package com.histar.be.mobile;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MobileExploreContentControllerTest {
    private final MobileExploreContentController controller = new MobileExploreContentController(new ObjectMapper());

    @Test
    void exposesReviewedFePresentationWithoutAnswerKeys() {
        var data = controller.content(UUID.fromString("11111111-1111-1111-1111-111111111111"), "explore").getData();
        assertEquals(4, ((List<?>) data.get("depthLayers")).size());
        assertEquals(3, ((List<?>) data.get("secretCiphers")).size());
        assertEquals(4, ((List<?>) data.get("investigationClues")).size());
        assertEquals(6, ((List<?>) data.get("storyDossiers")).size());
        assertEquals(9, ((List<?>) data.get("badges")).size());
        for (var item : (List<?>) data.get("depthLayers")) {
            var quiz = (Map<?, ?>) ((Map<?, ?>) item).get("quiz");
            assertFalse(quiz.containsKey("correctOption"));
        }
    }

    @Test
    void rejectsUnknownLocation() {
        assertThrows(ResponseStatusException.class, () -> controller.content(UUID.randomUUID(), "explore"));
        assertThrows(ResponseStatusException.class, () -> controller.content(UUID.fromString("11111111-1111-1111-1111-111111111111"), "../secret"));
    }

    @Test
    void includesDioramaAndPortalNarrativesWithoutClientSideQuizKey() {
        var id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var diorama = controller.content(id, "diorama").getData();
        assertFalse(((List<?>) diorama.get("events")).isEmpty());
        var portal = controller.content(id, "time-portal").getData();
        assertEquals(4, ((Map<?, ?>) portal.get("stories")).size());
        for (var story : ((Map<?, ?>) portal.get("stories")).values()) {
            assertFalse(((Map<?, ?>) ((Map<?, ?>) story).get("quiz")).containsKey("answer"));
        }
    }
}
