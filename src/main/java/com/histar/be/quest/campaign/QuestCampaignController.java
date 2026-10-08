package com.histar.be.quest.campaign;

import com.histar.be.common.response.ApiResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quest-campaigns")
@RequiredArgsConstructor
public class QuestCampaignController {
    private final QuestCampaignRepository repository;

    @GetMapping("/active")
    public ApiResponse<List<QuestCampaignResponse>> active() {
        return ApiResponse.ok(repository.findActive(Instant.now()).stream().map(QuestCampaignResponse::from).toList());
    }
}
