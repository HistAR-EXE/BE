package com.histar.be.rag.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.rag.dto.ChatPromptResponse;
import com.histar.be.rag.repository.StationChatPromptRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteCode}/stations/{stationCode}/chat-prompts")
@RequiredArgsConstructor
public class StationChatPromptController {

    private final StationChatPromptRepository repository;

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatPromptResponse>> list(
            @PathVariable String siteCode,
            @PathVariable String stationCode,
            @RequestParam(defaultValue = "chi-nam") String persona) {
        return ApiResponse.ok(repository
                .findBySiteCodeAndStationCodeAndPersonaOrderBySortOrderAsc(siteCode, stationCode, persona)
                .stream()
                .map(ChatPromptResponse::from)
                .toList());
    }
}
