package com.histar.be.group.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.billing.dto.OrgSubscribeRequest;
import com.histar.be.billing.service.BillingService;
import com.histar.be.group.dto.CreateGroupRequest;
import com.histar.be.group.dto.GroupSummaryResponse;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** CAP-CONC-02: parallel group creation yields unique 6-char codes (no unhandled 500). */
@SpringBootTest
@ActiveProfiles("test")
class GroupCodeConcurrencyTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createGroup_concurrentTeachers_produceUniqueCodes() throws Exception {
        Profile teacher1 = subscribeStandardTeacher("grp-conc-1");
        Profile teacher2 = subscribeStandardTeacher("grp-conc-2");

        int parallel = 2;
        ExecutorService pool = Executors.newFixedThreadPool(parallel);
        CountDownLatch ready = new CountDownLatch(parallel);
        CountDownLatch start = new CountDownLatch(1);
        List<UUID> teacherIds = List.of(teacher1.getId(), teacher2.getId());
        Set<String> codes = java.util.Collections.synchronizedSet(new HashSet<>());
        List<Future<GroupSummaryResponse>> futures = new java.util.ArrayList<>();

        for (UUID teacherId : teacherIds) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                GroupSummaryResponse created =
                        groupService.createGroup(teacherId, new CreateGroupRequest("Race Group " + teacherId));
                codes.add(created.code());
                return created;
            }));
        }

        ready.await();
        start.countDown();
        for (Future<GroupSummaryResponse> f : futures) {
            GroupSummaryResponse res = f.get();
            assertThat(res.code()).hasSize(6);
        }
        pool.shutdown();

        assertThat(codes).hasSize(parallel);
        assertThat(studyGroupRepository.findAll().stream().map(g -> g.getCode().toUpperCase()).distinct().count())
                .isGreaterThanOrEqualTo(parallel);
    }

    private Profile subscribeStandardTeacher(String prefix) {
        Profile teacher = profileRepository.save(Profile.builder()
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
        billingService.subscribeOrg(
                teacher.getId(),
                new OrgSubscribeRequest(prefix + " Org", "STANDARD", teacher.getEmail(), null),
                "DEMO");
        return profileRepository.findById(teacher.getId()).orElseThrow();
    }
}
