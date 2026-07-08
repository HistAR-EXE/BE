package com.histar.be.profile.service;

import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TierAccessService {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public boolean hasPremiumAccess(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return false;
        }
        if (profile.getOrgId() != null) {
            return true;
        }
        UserRole role = UserRole.fromStored(profile.getRole());
        if (role == UserRole.ADMIN || role == UserRole.TEACHER) {
            return true;
        }
        return UserTier.PREMIUM == UserTier.fromStored(profile.getTier());
    }
}
