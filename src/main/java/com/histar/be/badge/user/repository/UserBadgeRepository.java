package com.histar.be.badge.user.repository;

import com.histar.be.badge.user.entity.UserBadge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    @Query("select count(distinct b.badgeId) from UserBadge b where b.userId = :userId")
    long countDistinctEarnedBadges(@Param("userId") UUID userId);

    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);

    List<UserBadge> findByUserId(UUID userId);
}
