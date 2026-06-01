package com.histar.be.userbadge.repository;

import com.histar.be.userbadge.entity.UserBadge;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
}
