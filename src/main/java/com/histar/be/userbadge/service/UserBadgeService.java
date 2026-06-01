package com.histar.be.userbadge.service;

import com.histar.be.userbadge.entity.UserBadge;
import java.util.List;
import java.util.UUID;


public interface UserBadgeService {

    List<UserBadge> findAll();

    UserBadge findById(UUID id);

    UserBadge save(UserBadge entity);

    void deleteById(UUID id);

    long count();
}
