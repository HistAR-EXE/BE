package com.histar.be.auth.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.EmailNotVerifiedException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerifiedGuard {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public void assertEmailVerified(UUID userId) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));
        if (!Boolean.TRUE.equals(profile.getEmailVerified())) {
            throw new EmailNotVerifiedException();
        }
    }
}
