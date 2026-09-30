package com.histar.be.events.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.events.dto.EventBatchRequest;
import com.histar.be.events.dto.EventBatchResponse;
import com.histar.be.events.service.EventsService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventsController {

    private final EventsService eventsService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/batch")
    public ApiResponse<EventBatchResponse> batch(@RequestBody @Valid EventBatchRequest request) {
        UUID userId = currentUserAccessor.getUserId().orElse(null);
        return ApiResponse.ok(eventsService.ingestBatch(request, userId));
    }
}
