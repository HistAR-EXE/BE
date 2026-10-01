package com.histar.be.badge.service.impl;

import com.histar.be.badge.dto.UserBadgeResponse;
import com.histar.be.badge.entity.Badge;
import com.histar.be.badge.service.BadgeCatalogService;
import com.histar.be.badge.service.BadgeService;
import com.histar.be.badge.user.entity.UserBadge;
import com.histar.be.badge.user.repository.UserBadgeRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeCatalogServiceImpl implements BadgeCatalogService {

    private final BadgeService badgeService;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeResponse> listForUser(UUID userId) {
        Map<UUID, UserBadge> earnedByBadgeId = userBadgeRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, ub -> ub, (a, b) -> a));
        return badgeService.findAll().stream()
                .map(badge -> toResponse(badge, earnedByBadgeId.get(badge.getId())))
                .toList();
    }

    private UserBadgeResponse toResponse(Badge badge, UserBadge earned) {
        return new UserBadgeResponse(
                badge.getId(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl(),
                earned != null,
                earned != null ? earned.getEarnedAt() : null);
    }
}
