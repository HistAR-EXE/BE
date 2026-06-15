package com.histar.be.profile.service;

import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileAccessPolicy {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public boolean previewsAllGamificationContent(UUID userId) {
        return profileRepository
                .findById(userId)
                .map(p -> UserRole.fromStored(p.getRole()) == UserRole.ADMIN)
                .orElse(false);
    }
}
