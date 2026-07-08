package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.billing.dto.OrgTrialRequest;
import com.histar.be.billing.service.BillingService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * CAP-CONC-01: five students race for the last trial seat (max 11 = teacher + 10 students).
 * Must NOT use class-level @Transactional — each join runs in its own transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrgMembershipConcurrencyTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private OrgMembershipService orgMembershipService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void joinOrg_concurrentRequests_allowOnlyOneIntoLastSeat() throws Exception {
        Profile teacher = saveVerifiedUser("teacher-conc");
        var trial = billingService.createOrgTrial(teacher.getId(), new OrgTrialRequest("Conc Trial", teacher.getEmail()));
        UUID orgId = trial.organizationId();
        Organization org = organizationRepository.findById(orgId).orElseThrow();
        String inviteCode = "CONC" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        org.setInviteCode(inviteCode);
        org.setInviteCodeExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        organizationRepository.save(org);

        for (int i = 0; i < 9; i++) {
            Profile student = saveVerifiedUser("student-prefill-" + i);
            orgMembershipService.joinOrg(student.getId(), inviteCode);
        }

        int racers = 5;
        List<Profile> racersProfiles = new ArrayList<>();
        for (int i = 0; i < racers; i++) {
            racersProfiles.add(saveVerifiedUser("student-race-" + i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(racers);
        CountDownLatch ready = new CountDownLatch(racers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (Profile racer : racersProfiles) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orgMembershipService.joinOrg(racer.getId(), inviteCode);
                    successCount.incrementAndGet();
                } catch (BusinessRuleException ex) {
                    failCount.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(racers - 1);
        assertThat(organizationMemberRepository.countByOrganizationId(orgId)).isLessThanOrEqualTo(11);
    }

    private Profile saveVerifiedUser(String prefix) {
        return profileRepository.save(Profile.builder()
                .email(prefix + "-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName(prefix)
                .provider("local")
                .role(UserRole.USER.name())
                .tier(UserTier.FREE.name())
                .emailVerified(true)
                .emailVerifiedAt(Instant.now())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
    }
}
