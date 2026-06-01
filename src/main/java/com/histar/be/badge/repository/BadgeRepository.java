package com.histar.be.badge.repository;

import java.util.UUID;

import com.histar.be.badge.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {
}
