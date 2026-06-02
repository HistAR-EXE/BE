package com.histar.be.userbadge.repository;

import com.histar.be.userbadge.entity.UserBadge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);

    List<UserBadge> findByUserId(UUID userId);
}
