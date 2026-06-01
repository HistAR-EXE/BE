package com.histar.be.badge.service;

import com.histar.be.badge.entity.Badge;
import java.util.List;
import java.util.UUID;


public interface BadgeService {

    List<Badge> findAll();

    Badge findById(UUID id);

    Badge save(Badge entity);

    void deleteById(UUID id);

    long count();
}
