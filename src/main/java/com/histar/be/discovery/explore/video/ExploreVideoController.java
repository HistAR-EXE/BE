package com.histar.be.discovery.explore.video;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.common.exception.AuthException;
import com.histar.be.discovery.explore.ExploreService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/me/explore/videos") @RequiredArgsConstructor
public class ExploreVideoController {
    private final ExploreVideoService videos;
    private final CurrentUserAccessor users;
    private UUID userId(){return users.getUserId().orElseThrow(()->new AuthException("Unauthorized"));}
    @GetMapping public ApiResponse<ExploreVideoService.Library> library(){return ApiResponse.ok(videos.library(userId()));}
    @PostMapping("/{id}/session") public ApiResponse<ExploreVideoService.State> start(@PathVariable String id){return ApiResponse.ok(videos.start(userId(),id));}
    @PostMapping("/{id}/heartbeat") public ApiResponse<ExploreVideoService.State> heartbeat(@PathVariable String id,@RequestBody @Valid ExploreVideoService.Heartbeat request){return ApiResponse.ok(videos.heartbeat(userId(),id,request));}
    @PostMapping("/{id}/complete") public ApiResponse<ExploreService.AttemptResponse> complete(@PathVariable String id,@RequestBody @Valid ExploreVideoService.Answer request){return ApiResponse.ok(videos.complete(userId(),id,request));}
}
