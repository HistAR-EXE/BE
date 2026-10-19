package com.histar.be.quest.v2;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.quest.v2.dto.QuestV2Card;
import com.histar.be.quest.v2.dto.QuestV2Detail;
import com.histar.be.quest.v2.dto.QuestV2StartResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nhiệm vụ v2 — tách Online / Tại di tích, BE tính trạng thái.
 * Spec: docs/superpowers/specs/2026-10-19-quest-tracks-v2-design.md
 */
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class QuestV2Controller {

    private final QuestStateService questStateService;
    private final CurrentUserAccessor currentUserAccessor;

    /** Danh sách nhiệm vụ của di tích. Không đăng nhập vẫn xem được (không có tiến độ). */
    @GetMapping("/quests")
    public ApiResponse<List<QuestV2Card>> list(
            @RequestParam UUID locationId,
            @RequestParam(required = false) String track) {
        return ApiResponse.ok(questStateService.list(locationId, track, currentUserAccessor.getUserId().orElse(null)));
    }

    @GetMapping("/quests/{id}")
    public ApiResponse<QuestV2Detail> detail(@PathVariable("id") UUID questId) {
        return ApiResponse.ok(questStateService.detail(questId, currentUserAccessor.getUserId().orElse(null)));
    }

    @PostMapping("/quests/{id}/start")
    public ApiResponse<QuestV2StartResponse> start(@PathVariable("id") UUID questId) {
        return ApiResponse.ok(questStateService.start(questId, requireUser()));
    }

    /** Hoàn thành các nhiệm vụ đang làm đã đủ bước bắt buộc. FE gọi khi mở trang Nhiệm vụ. */
    @PostMapping("/me/quests/reconcile")
    public ApiResponse<List<QuestCompletedDto>> reconcile(@RequestParam UUID locationId) {
        return ApiResponse.ok(questStateService.reconcile(locationId, requireUser()));
    }

    private UUID requireUser() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
