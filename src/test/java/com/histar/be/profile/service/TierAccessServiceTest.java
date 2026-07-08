package com.histar.be.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TierAccessServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private TierAccessService tierAccessService;

    @Test
    void freeUser_noPremium() {
        UUID id = UUID.randomUUID();
        when(profileRepository.findById(id))
                .thenReturn(Optional.of(Profile.builder()
                        .id(id)
                        .role(UserRole.USER.name())
                        .tier(UserTier.FREE.name())
                        .build()));
        assertThat(tierAccessService.hasPremiumAccess(id)).isFalse();
    }

    @Test
    void premiumOrOrg_hasAccess() {
        UUID premiumId = UUID.randomUUID();
        when(profileRepository.findById(premiumId))
                .thenReturn(Optional.of(Profile.builder()
                        .id(premiumId)
                        .role(UserRole.USER.name())
                        .tier(UserTier.PREMIUM.name())
                        .build()));
        assertThat(tierAccessService.hasPremiumAccess(premiumId)).isTrue();

        UUID orgId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(profileRepository.findById(memberId))
                .thenReturn(Optional.of(Profile.builder()
                        .id(memberId)
                        .orgId(orgId)
                        .role(UserRole.ORG_MEMBER.name())
                        .tier(UserTier.FREE.name())
                        .build()));
        assertThat(tierAccessService.hasPremiumAccess(memberId)).isTrue();
    }
}
