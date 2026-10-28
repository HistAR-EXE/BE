package com.histar.be.discovery.explore.video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ExploreVideoProgressRepository extends JpaRepository<ExploreVideoProgress,UUID> {
    Optional<ExploreVideoProgress> findByUserIdAndVideoId(UUID userId,String videoId);
    List<ExploreVideoProgress> findByUserId(UUID userId);
}
