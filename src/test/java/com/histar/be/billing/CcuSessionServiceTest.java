package com.histar.be.billing;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.entity.OrgActiveSession;
import com.histar.be.billing.entity.OrgActiveSession.OrgActiveSessionId;
import com.histar.be.billing.repository.OrgActiveSessionRepository;
import com.histar.be.billing.service.CcuSessionService;
import com.histar.be.common.exception.CcuLimitException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CcuSessionServiceTest {

    @Mock
    private OrgActiveSessionRepository orgActiveSessionRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private CcuSessionService ccuSessionService;

    private UUID orgId;
    private UUID activeUserId;
    private UUID newUserId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        activeUserId = UUID.randomUUID();
        newUserId = UUID.randomUUID();
    }

    @Test
    void assertCcuAvailable_throwsWhenOrgAtCapacity() {
        Organization org = Organization.builder()
                .id(orgId)
                .planType("MICRO")
                .maxCcu(1)
                .build();
        Profile newUser = Profile.builder().id(newUserId).orgId(orgId).build();

        when(profileRepository.findById(newUserId)).thenReturn(Optional.of(newUser));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(orgActiveSessionRepository.countActiveByOrgId(eq(orgId), any(Instant.class))).thenReturn(1);
        when(orgActiveSessionRepository.findById(new OrgActiveSessionId(orgId, newUserId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ccuSessionService.assertCcuAvailable(newUserId))
                .isInstanceOf(CcuLimitException.class);
    }

    @Test
    void assertCcuAvailable_allowsReentryForAlreadyActiveUser() {
        Organization org = Organization.builder()
                .id(orgId)
                .planType("MICRO")
                .maxCcu(1)
                .build();
        Profile activeUser = Profile.builder().id(activeUserId).orgId(orgId).build();
        Instant recent = Instant.now();

        when(profileRepository.findById(activeUserId)).thenReturn(Optional.of(activeUser));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(orgActiveSessionRepository.countActiveByOrgId(eq(orgId), any(Instant.class))).thenReturn(1);
        when(orgActiveSessionRepository.findById(new OrgActiveSessionId(orgId, activeUserId)))
                .thenReturn(Optional.of(OrgActiveSession.builder()
                        .orgId(orgId)
                        .userId(activeUserId)
                        .lastSeenAt(recent)
                        .build()));

        assertThatCode(() -> ccuSessionService.assertCcuAvailable(activeUserId))
                .doesNotThrowAnyException();
        verify(orgActiveSessionRepository).save(any(OrgActiveSession.class));
    }

    @Test
    void heartbeat_skipsWhenUserHasNoOrg() {
        UUID soloUserId = UUID.randomUUID();
        when(profileRepository.findById(soloUserId))
                .thenReturn(Optional.of(Profile.builder().id(soloUserId).build()));

        ccuSessionService.heartbeat(soloUserId);

        verify(orgActiveSessionRepository, never()).save(any());
    }
}
