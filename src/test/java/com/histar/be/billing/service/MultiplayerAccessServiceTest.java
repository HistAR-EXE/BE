package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MultiplayerAccessServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private MultiplayerAccessService multiplayerAccessService;

    @Test
    void MICRO_plan_hasNoMultiplayerAccess() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(orgId).build()));
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder().id(orgId).planType("MICRO").build()));

        assertThat(multiplayerAccessService.hasMultiplayerAccess(userId)).isFalse();
        assertThatThrownBy(() -> multiplayerAccessService.assertMultiplayerAccess(userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Standard");
    }

    @Test
    void STANDARD_and_PREMIUM_haveMultiplayerAccess() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(orgId).build()));
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder().id(orgId).planType("STANDARD").build()));
        assertThat(multiplayerAccessService.hasMultiplayerAccess(userId)).isTrue();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder().id(orgId).planType("PREMIUM").build()));
        assertThat(multiplayerAccessService.hasMultiplayerAccess(userId)).isTrue();
    }

    @Test
    void noOrg_blocked() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).build()));
        assertThat(multiplayerAccessService.hasMultiplayerAccess(userId)).isFalse();
    }
}
