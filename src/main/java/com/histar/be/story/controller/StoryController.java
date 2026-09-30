package com.histar.be.story.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.story.dto.StoryChapterResponse;
import com.histar.be.story.service.StoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteCode}/story")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<List<StoryChapterResponse>> list(@PathVariable String siteCode) {
        return ApiResponse.ok(storyService.listChapters(
                siteCode, currentUserAccessor.getUserId().orElse(null)));
    }
}
