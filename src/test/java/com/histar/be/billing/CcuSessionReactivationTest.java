package com.histar.be.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.histar.be.billing.entity.OrgActiveSession;
import com.histar.be.billing.entity.OrgActiveSession.OrgActiveSessionId;
import com.histar.be.billing.repository.OrgActiveSessionRepository;
import com.histar.be.billing.service.CcuSessionService;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CcuSessionReactivationTest {
    @Autowired CcuSessionService service;
    @Autowired OrgActiveSessionRepository sessions;
    @Autowired OrganizationRepository organizations;
    @Autowired OrganizationMemberRepository members;
    @Autowired ProfileRepository profiles;
    @Autowired EntityManager entityManager;

    @Test
    void expiredSessionIsReactivatedWithoutDeletingItsManagedRow() {
        Instant before = Instant.now();
        Organization org = organizations.saveAndFlush(Organization.builder()
                .name("CCU reactivation fixture").planType("MICRO").status("ACTIVE").maxCcu(1).build());
        Profile user = profiles.saveAndFlush(Profile.builder().email("ccu-" + UUID.randomUUID() + "@test.invalid")
                .displayName("CCU fixture").orgId(org.getId()).emailVerified(true).build());
        members.saveAndFlush(OrganizationMember.builder().organizationId(org.getId()).userId(user.getId())
                .orgRole("teacher").invitedAt(before).acceptedAt(before).build());
        sessions.saveAndFlush(OrgActiveSession.builder().orgId(org.getId()).userId(user.getId())
                .lastSeenAt(before.minus(10, ChronoUnit.MINUTES)).build());
        UUID stalePeer = UUID.randomUUID();
        sessions.saveAndFlush(OrgActiveSession.builder().orgId(org.getId()).userId(stalePeer)
                .lastSeenAt(before.minus(10, ChronoUnit.MINUTES)).build());
        Organization otherOrg = organizations.saveAndFlush(Organization.builder()
                .name("Other CCU organization").planType("MICRO").status("ACTIVE").maxCcu(1).build());
        sessions.saveAndFlush(OrgActiveSession.builder().orgId(otherOrg.getId()).userId(user.getId())
                .lastSeenAt(before.minus(10, ChronoUnit.MINUTES)).build());
        entityManager.clear();

        assertThatCode(() -> { service.assertCcuAvailable(user.getId()); entityManager.flush(); })
                .doesNotThrowAnyException();
        entityManager.clear();
        assertThat(sessions.findById(new OrgActiveSessionId(org.getId(), user.getId())).orElseThrow().getLastSeenAt())
                .isAfterOrEqualTo(before);
        assertThat(service.getCurrentCcu(org.getId())).isEqualTo(1);
        assertThat(sessions.findById(new OrgActiveSessionId(org.getId(), stalePeer))).isEmpty();
        assertThat(sessions.findById(new OrgActiveSessionId(otherOrg.getId(), user.getId()))).isPresent();
    }
}
