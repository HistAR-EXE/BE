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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void newlySavedProfileCanEnrollBeforeTheProvisioningTransactionCommits() {
        Organization org = saveActiveOrganization("new-provisioned-member");
        UUID userId = new TransactionTemplate(transactionManager).execute(status -> {
            Profile pendingInsert = saveVerifiedUser("unflushed-provisioned-student");
            orgMembershipService.enrollUserInOrganization(pendingInsert.getId(), org.getId());
            return pendingInsert.getId();
        });
        assertThat(organizationMemberRepository.findByOrganizationIdAndUserId(org.getId(), userId)).isPresent();
        assertThat(profileRepository.findById(userId).orElseThrow().getOrgId()).isEqualTo(org.getId());
    }

    @Test
    void sameUserCannotJoinTwoOrganizationsEvenWhenBothTransactionsLoadedAnOldProfile() throws Exception {
        Profile student = saveVerifiedUser("cross-org-racer");
        Organization first = saveActiveOrganization("cross-first");
        Organization second = saveActiveOrganization("cross-second");
        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> attempts = new ArrayList<>();
            for (UUID orgId : List.of(first.getId(), second.getId())) {
                attempts.add(pool.submit(() -> {
                    try {
                        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                            // Auth already loads this entity before invitation acceptance. Locking a query
                            // alone does not refresh Hibernate's first-level cached orgId after a wait.
                            assertThat(profileRepository.findById(student.getId()).orElseThrow().getOrgId()).isNull();
                            loaded.countDown();
                            try {
                                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(ex);
                            }
                            orgMembershipService.enrollUserInOrganization(student.getId(), orgId);
                        });
                        successes.incrementAndGet();
                    } catch (BusinessRuleException ex) {
                        rejections.incrementAndGet();
                    }
                }));
            }
            assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> attempt : attempts) attempt.get(30, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
        assertThat(successes.get()).isEqualTo(1);
        assertThat(rejections.get()).isEqualTo(1);
        var actualMemberships = organizationMemberRepository.findByUserId(student.getId());
        assertThat(actualMemberships).hasSize(1);
        assertThat(profileRepository.findById(student.getId()).orElseThrow().getOrgId())
                .isEqualTo(actualMemberships.get(0).getOrganizationId());
    }

    private Organization saveActiveOrganization(String name) {
        return organizationRepository.save(Organization.builder().name(name)
                .slug(name + "-" + UUID.randomUUID()).status("ACTIVE")
                .plan("standard").planType("STANDARD").maxVerifiedAccounts(10).maxCcu(40)
                .createdAt(Instant.now()).build());
    }

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
