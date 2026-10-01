package com.histar.be.badge.user.service;

import com.histar.be.badge.user.entity.UserBadge;
import java.util.List;
import java.util.UUID;

public interface UserBadgeService {

    List<UserBadge> findAll();

    UserBadge findById(UUID id);

    UserBadge save(UserBadge entity);

    void deleteById(UUID id);

    long count();
}
