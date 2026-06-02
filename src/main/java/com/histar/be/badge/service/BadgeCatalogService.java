package com.histar.be.badge.service;

import com.histar.be.badge.dto.UserBadgeResponse;
import java.util.List;
import java.util.UUID;

public interface BadgeCatalogService {

    List<UserBadgeResponse> listForUser(UUID userId);
}
