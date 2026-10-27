package com.histar.be.billing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.histar.be.billing.entity.UsageQuota;
import com.histar.be.billing.repository.UsageQuotaRepository;
import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.common.exception.QuotaExceededException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrgMonthlyQuotaExhaustTest {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private UsageQuotaService usageQuotaService;

    @Autowired
    private UsageQuotaRepository usageQuotaRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private com.histar.be.organization.repository.OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void assertCanSendChat_throwsWhenOrgMonthlyPoolExhausted() {
        Organization org = organizationRepository.save(Organization.builder()
                .name("Quota Test Org")
                .planType("MICRO")
                .status("ACTIVE")
                .maxAiQueriesPerMonth(5)
                .maxCcu(15)
                .maxVerifiedAccounts(100)
                .createdAt(Instant.now())
                .build());

        Profile student = profileRepository.save(Profile.builder()
                .email("org-quota-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Org Student")
                .provider("local")
                .role(UserRole.ORG_MEMBER.name())
                .tier(UserTier.FREE.name())
                .orgId(org.getId())
                .emailVerified(true)
                .emailVerifiedAt(Instant.now())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());

        organizationMemberRepository.save(com.histar.be.organization.entity.OrganizationMember.builder()
                .organizationId(org.getId()).userId(student.getId()).orgRole("student")
                .invitedAt(Instant.now()).acceptedAt(Instant.now()).build());
        LocalDate vnDay = LocalDate.now(VN_ZONE);
        usageQuotaRepository.save(UsageQuota.builder()
                .organizationId(org.getId())
                .year(vnDay.getYear())
                .month(vnDay.getMonthValue())
                .usedAiQueries(5)
                .build());

        assertThatThrownBy(() -> usageQuotaService.assertCanSendChat(student.getId()))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("hết");
    }
}
