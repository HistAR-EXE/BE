package com.histar.be.mobile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.response.ApiResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Presentation-only catalog for mobile. Existing FE endpoints and rewards remain unchanged. */
@RestController
@RequestMapping("/api/mobile/v1")
@RequiredArgsConstructor
public class MobileExploreContentController {
    private static final UUID CU_CHI = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final ObjectMapper mapper;

    @GetMapping("/locations/{locationId}/content/{section}")
    public ApiResponse<Map<String, Object>> content(@PathVariable UUID locationId, @PathVariable String section) {
        if (!CU_CHI.equals(locationId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!section.equals("explore") && !section.equals("diorama") && !section.equals("time-portal")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try (var stream = new ClassPathResource("mobile-content/cu-chi-" + section + ".json").getInputStream()) {
            return ApiResponse.ok(mapper.readValue(stream, new TypeReference<Map<String, Object>>() {}));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mobile content unavailable", exception);
        }
    }
}
