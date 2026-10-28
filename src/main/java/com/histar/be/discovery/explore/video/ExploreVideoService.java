package com.histar.be.discovery.explore.video;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.explore.*;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.discovery.service.DiscoveryRewardService;
import com.histar.be.organization.service.TrialEntitlementGuard;
import com.histar.be.profile.service.ProfilePointsService;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ExploreVideoService {
    private final ExploreVideoCatalog catalog;
    private final ExploreVideoProgressRepository progress;
    private final ProfilePointsService points;
    private final UserDiscoveryRepository discoveries;
    private final DiscoveryRewardService rewards;
    private final ExploreService explore;
    private final ExploreAttemptRepository attempts;
    private final TrialEntitlementGuard trial;

    public record Video(String id,String title,double durationSeconds,int xp,String question,List<String> options,
                        double watchedSeconds,double resumeSeconds,boolean ready,boolean completed) {}
    public record Library(List<Video> videos) {}
    public record State(UUID sessionId,double watchedSeconds,double resumeSeconds,boolean ready,boolean completed) {}
    public record Heartbeat(@NotNull UUID sessionId,@NotNull Double positionSeconds) {}
    public record Answer(@NotNull @Min(0) @Max(2) Integer answer) {}

    @Transactional(readOnly=true)
    public Library library(UUID userId) {
        var rows=new HashMap<String,ExploreVideoProgress>();
        progress.findByUserId(userId).forEach(p->rows.put(p.getVideoId(),p));
        return new Library(catalog.all().stream().map(v->{
            var state=state(userId,v,rows.get(v.id()));
            return new Video(v.id(),v.title(),v.durationSeconds(),ExploreVideoCatalog.XP,v.question(),v.options(),
                    state.watchedSeconds(),state.resumeSeconds(),state.ready(),state.completed());
        }).toList());
    }
    @Transactional
    public State start(UUID userId,String id) {
        trial.assertStudentCanWrite(userId);
        points.lockAccount(userId);
        var video=catalog.get(id);
        var row=progress.findByUserIdAndVideoId(userId,id).orElseGet(()->ExploreVideoProgress.builder().userId(userId).videoId(id).build());
        row.setSessionId(UUID.randomUUID());
        row.setSessionStartedAt(Instant.now()); row.setSessionStartedSeconds(row.getWatchedSeconds());
        row.setHeartbeatAt(Instant.now()); row.setUpdatedAt(Instant.now());
        progress.save(row);
        return state(userId,video,row);
    }
    @Transactional
    public State heartbeat(UUID userId,String id,Heartbeat request) {
        trial.assertStudentCanWrite(userId);
        points.lockAccount(userId);
        var video=catalog.get(id);
        var row=progress.findByUserIdAndVideoId(userId,id).orElseThrow(()->new BusinessRuleException("Vui lòng mở video trước khi ghi nhận tiến độ."));
        if (!Objects.equals(row.getSessionId(),request.sessionId()) || row.getHeartbeatAt()==null)
            throw new BusinessRuleException("Phiên xem đã được thay thế. Vui lòng mở lại video.");
        Instant now=Instant.now();
        double elapsed=Duration.between(row.getHeartbeatAt(),now).toMillis()/1000.0;
        if (row.getSessionStartedAt()==null || request.positionSeconds()==null) throw new BusinessRuleException("Phiên xem không hợp lệ.");
        double sessionElapsed=Duration.between(row.getSessionStartedAt(),now).toMillis()/1000.0;
        if (request.positionSeconds()-row.getSessionStartedSeconds()>sessionElapsed*1.15+0.001)
            throw new BusinessRuleException("Tiến độ vượt quá thời gian xem. Vui lòng phát tiếp từ vị trí đã lưu.");
        double watched=VideoPlaybackRules.advance(row.getWatchedSeconds(),request.positionSeconds(),elapsed,video.durationSeconds());
        row.setWatchedSeconds(watched); row.setHeartbeatAt(now); row.setUpdatedAt(now);
        progress.save(row);
        return state(userId,video,row);
    }
    @Transactional
    public ExploreService.AttemptResponse complete(UUID userId,String id,Answer request) {
        trial.assertStudentCanWrite(userId);
        points.lockAccount(userId);
        var video=catalog.get(id);
        if (discoveries.existsByUserIdAndLocationIdAndDiscoveryKey(userId,ExploreChallengeCatalog.LOCATION,video.key()))
            return new ExploreService.AttemptResponse(true,true,0,explore.snapshot(userId));
        var row=progress.findByUserIdAndVideoId(userId,id).orElseThrow(()->new BusinessRuleException("Hãy xem ít nhất 90% video trước khi làm thử thách."));
        if (!VideoPlaybackRules.ready(row.getWatchedSeconds(),video.durationSeconds()))
            throw new BusinessRuleException("Hãy xem ít nhất 90% video trước khi làm thử thách.");
        if (attempts.countByUserIdAndSubmittedAtAfter(userId,Instant.now().minusSeconds(60))>=12)
            throw new BusinessRuleException("Bạn thao tác quá nhanh. Vui lòng thử lại sau một phút.");
        if(request.answer()==null || request.answer()<0 || request.answer()>=video.options().size())
            throw new BusinessRuleException("Câu trả lời không hợp lệ.");
        boolean passed=request.answer()==video.correctOption();
        attempts.save(ExploreAttempt.builder().userId(userId).locationId(ExploreChallengeCatalog.LOCATION)
                .challengeKey(video.key()).answer(request.answer()).passed(passed).submittedAt(Instant.now()).build());
        int xp=0;
        if(passed) {
            var result=rewards.record(userId,ExploreChallengeCatalog.LOCATION,video.key(),"explore-video");
            if(!result.recorded()) throw new BusinessRuleException("Chưa thể ghi nhận video. Vui lòng thử lại.");
            xp=result.xpEarned();
        }
        return new ExploreService.AttemptResponse(passed,false,xp,explore.snapshot(userId));
    }
    private State state(UUID userId,ExploreVideoCatalog.Entry video,ExploreVideoProgress row) {
        boolean complete=discoveries.existsByUserIdAndLocationIdAndDiscoveryKey(userId,ExploreChallengeCatalog.LOCATION,video.key());
        double watched=complete ? video.durationSeconds() : row==null ? 0 : row.getWatchedSeconds();
        return new State(row==null ? null : row.getSessionId(),watched,Math.min(watched,video.durationSeconds()),complete || VideoPlaybackRules.ready(watched,video.durationSeconds()),complete);
    }
}
