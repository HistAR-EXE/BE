package com.histar.be.leaderboard.service;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.leaderboard.dto.*;
import com.histar.be.leaderboard.repository.HonorsFactsRepository;
import com.histar.be.leaderboard.service.impl.LeaderboardServiceImpl;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor
public class HonorsFactsService {
    private final LeaderboardServiceImpl access;
    private final HonorsFactsRepository evidence;
    private final GamificationProperties gamification;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public HonorsPassportResponse passport(UUID viewer) {
        var profile=access.resolveAccess("all",null,viewer,null).profile();
        int points=Math.max(0,profile.getTotalPoints()==null?0:profile.getTotalPoints());
        var level=LevelCalculator.info(points,gamification.parseLevelThresholds(),gamification.parseLevelNames());
        String name=profile.getDisplayName()==null||profile.getDisplayName().isBlank()?"Nhà thám hiểm":profile.getDisplayName();
        return new HonorsPassportResponse(viewer,name,points,level.level(),level.levelName(),evidence.badgeCount(viewer),evidence.heritageBadges(viewer),Instant.now());
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public HonorsCommunityResponse community(String scope,String city,UUID viewer,UUID group) {
        var validated=access.resolveAccess(scope,city,viewer,group);
        var counts=evidence.community(validated.city(),validated.orgId(),group);
        return new HonorsCommunityResponse(validated.scope(),counts.explorers(),counts.completedChallenges(),counts.discoveries(),counts.badges(),Instant.now());
    }
}
