package com.histar.be.discovery;

import static org.junit.jupiter.api.Assertions.*;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.explore.*;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.*;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.*;
import com.histar.be.profile.service.ProfilePointsService;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:explore_integrity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ExploreRewardIntegrationTest {
    @Autowired ExploreService explore;
    @Autowired com.histar.be.discovery.explore.video.ExploreVideoService videos;
    @Autowired com.histar.be.discovery.explore.video.ExploreVideoCatalog videoCatalog;
    @Autowired com.histar.be.discovery.explore.video.ExploreVideoProgressRepository videoProgress;
    @Autowired ProfilePointsService points;
    @Autowired ProfileRepository profiles;
    @Autowired DiscoveryPointRepository catalog;
    @Autowired UserDiscoveryRepository discoveries;
    @Autowired XpLedgerRepository ledger;
    @Autowired ExploreAttemptRepository attempts;
    @Autowired com.histar.be.message.repository.MessageRepository messages;
    @Autowired com.histar.be.conversation.repository.ConversationRepository conversations;
    @Autowired ExploreBadgeContextService badgeContext;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    UUID user;

    @BeforeEach void setup() {
        user = account(0);
        var keys = new HashSet<>(ExploreChallengeCatalog.QUIZZES.keySet());
        keys.addAll(ExploreChallengeCatalog.CLUES);
        keys.add(ExploreChallengeCatalog.CONCLUSION);
        keys.add("feature:diorama");
        com.histar.be.discovery.explore.video.ExploreVideoCatalog.IDS.forEach(id -> keys.add("video:" + id));
        for (String key : keys) {
            if (catalog.findByUnlockKeyAndLocationId(key, ExploreChallengeCatalog.LOCATION).isEmpty()) {
                catalog.save(DiscoveryPoint.builder().locationId(ExploreChallengeCatalog.LOCATION).name(key)
                        .unlockKey(key).mapXPct(BigDecimal.valueOf(50)).mapYPct(BigDecimal.valueOf(50)).sortOrder(1).build());
            }
        }
        for (int i = 0; i < 9; i++) {
            UUID id = ExploreBadgeRules.databaseId(i);
            if (jdbc.queryForObject("select count(*) from badges where id = ?", Integer.class, id) == 0) {
                jdbc.update("insert into badges(id, name, condition_type, condition_value) values(?, ?, 'explore', ?)",
                        id, ExploreBadgeRules.IDS.get(i), i + 1);
            }
        }
    }

    private UUID account(int xp) {
        return profiles.save(Profile.builder().email(UUID.randomUUID() + "@integrity.test")
                .displayName("Integrity test").role("USER").tier("FREE").emailVerified(true)
                .totalPoints(xp).level(1).build()).getId();
    }
    private ExploreService.AttemptResponse submit(UUID who, String key, Integer answer) {
        return explore.submit(who, new ExploreService.AttemptRequest(ExploreChallengeCatalog.LOCATION, key, answer));
    }

    @Test void honorsSummaryMustNotAwardEligibleBadgesOrWriteXp() {
        UUID viewer = account(50);
        long beforeBadges = jdbc.queryForObject("select count(*) from user_badges where user_id = ?", Long.class, viewer);
        long beforeLedger = ledger.count();
        var summary = explore.readSnapshot(viewer);
        assertEquals(50, summary.profile().totalPoints());
        assertEquals(beforeBadges, jdbc.queryForObject("select count(*) from user_badges where user_id = ?", Long.class, viewer));
        assertEquals(beforeLedger, ledger.count());
    }

    @Test void gradeRetryAndReplayPersistOnlyOneReward() {
        assertFalse(submit(user, "depth_quiz:surface", 0).passed());
        assertEquals(0, explore.snapshot(user).profile().totalPoints());
        var correct = submit(user, "depth_quiz:surface", 1);
        assertTrue(correct.passed()); assertEquals(15, correct.xpEarned());
        assertEquals(15, correct.snapshot().profile().totalPoints());
        assertTrue(correct.snapshot().badges().get(0).earned());
        var replay = submit(user, "depth_quiz:surface", 1);
        assertEquals(0, replay.xpEarned()); assertTrue(replay.alreadyCompleted());
        assertEquals(15, replay.snapshot().profile().totalPoints());
        assertEquals(2, attempts.countByUserIdAndSubmittedAtAfter(user, java.time.Instant.EPOCH));
    }

    @Test void conclusionNeedsFourPersistedClues() {
        assertThrows(BusinessRuleException.class, () -> submit(user, ExploreChallengeCatalog.CONCLUSION, null));
        for (String key : ExploreChallengeCatalog.CLUES) submit(user, key, null);
        var result = submit(user, ExploreChallengeCatalog.CONCLUSION, null);
        assertEquals(230, result.snapshot().profile().totalPoints());
        assertTrue(result.snapshot().badges().get(3).earned());
    }

    @Test void snapshotRestoresStateAndDoesNotMixAccounts() {
        submit(user, "cipher:hoang-cam", 1);
        assertTrue(explore.snapshot(user).discovery().keys().contains("cipher:hoang-cam"));
        var other = explore.snapshot(account(0));
        assertEquals(0, other.profile().totalPoints());
        assertTrue(other.discovery().keys().isEmpty());
        assertTrue(other.badges().stream().noneMatch(ExploreService.BadgeProgress::earned));
    }

    @Test void failedTransactionRollsBackCompletionAttemptsAndPoints() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            submit(user, "depth_quiz:surface", 1);
            throw new IllegalStateException("simulated downstream failure");
        }));
        assertEquals(0, explore.snapshot(user).profile().totalPoints());
        assertTrue(explore.snapshot(user).discovery().keys().isEmpty());
        assertEquals(0, attempts.countByUserIdAndSubmittedAtAfter(user, java.time.Instant.EPOCH));
        assertFalse(ledger.existsByUserIdAndEventKey(user, "discovery:" + ExploreChallengeCatalog.LOCATION + ":depth_quiz:surface"));
    }

    private void concurrent(Runnable a, Runnable b) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> { start.await(); a.run(); return null; });
            var second = executor.submit(() -> { start.await(); b.run(); return null; });
            start.countDown(); first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
    }

    @Test void concurrentDifferentChallengesDoNotLosePoints() throws Exception {
        concurrent(() -> submit(user, "depth_quiz:surface", 1), () -> submit(user, "depth_quiz:layer1", 1));
        assertEquals(30, explore.snapshot(user).profile().totalPoints());
    }

    @Test void verifiedVideoCompletionIsConcurrentSafeAndSnapshotIncludesVideoXp() throws Exception {
        String id = "tunnel-digging";
        var video = videoCatalog.get(id);
        videos.start(user, id);
        assertThrows(BusinessRuleException.class, () -> videos.complete(user, id,
                new com.histar.be.discovery.explore.video.ExploreVideoService.Answer(video.correctOption())));
        var progress = videoProgress.findByUserIdAndVideoId(user,id).orElseThrow();
        progress.setWatchedSeconds(video.durationSeconds()); videoProgress.save(progress);
        concurrent(() -> videos.complete(user,id,new com.histar.be.discovery.explore.video.ExploreVideoService.Answer(video.correctOption())),
                () -> videos.complete(user,id,new com.histar.be.discovery.explore.video.ExploreVideoService.Answer(video.correctOption())));
        var result = explore.snapshot(user);
        assertEquals(30,result.profile().totalPoints()); assertEquals(30,result.archiveXp());
        assertTrue(result.discovery().keys().contains("video:"+id));
        assertEquals(1,ledger.findAll().stream().filter(e -> e.getUserId().equals(user) && e.getEventKey().endsWith(":video:"+id)).count());
        assertThrows(BusinessRuleException.class, () -> submit(user,"video:"+id,video.correctOption()));
    }
    @Test void concurrentSameChallengeCannotDoubleAward() throws Exception {
        concurrent(() -> submit(user, "depth_quiz:surface", 1), () -> submit(user, "depth_quiz:surface", 1));
        assertEquals(15, explore.snapshot(user).profile().totalPoints());
    }

    @Test void globalRewardsRetainHistoricalBalanceAndJournalMatchesTotal() throws Exception {
        user = account(125);
        concurrent(() -> points.awardOnce(user, "external:one", 30), () -> points.awardOnce(user, "external:two", 50));
        assertEquals(0, points.awardOnce(user, "external:one", 30));
        var snapshot = explore.snapshot(user);
        assertEquals(205, snapshot.profile().totalPoints());
        int sum = ledger.findAll().stream().filter(e -> e.getUserId().equals(user)).mapToInt(e -> e.getPoints()).sum();
        assertEquals(205, sum);
        assertTrue(snapshot.badges().get(0).earned());
        assertEquals(0, snapshot.archiveXp());
    }

    @Test void rejectsWrongLocationAndThrottlesGuessing() {
        assertThrows(BusinessRuleException.class, () -> explore.submit(user,
                new ExploreService.AttemptRequest(UUID.randomUUID(), "depth_quiz:surface", 1)));
        for (int i = 0; i < 12; i++) assertFalse(submit(user, "depth_quiz:surface", 0).passed());
        assertThrows(BusinessRuleException.class, () -> submit(user, "depth_quiz:surface", 1));
        assertEquals(0, explore.snapshot(user).profile().totalPoints());
    }

    @Test void adminPreviewIsNotAnEarnedAchievement() {
        var admin = profiles.findById(user).orElseThrow(); admin.setRole("ADMIN"); profiles.save(admin);
        assertTrue(explore.snapshot(user).discovery().keys().isEmpty());
        assertTrue(explore.snapshot(user).badges().stream().noneMatch(ExploreService.BadgeProgress::earned));
    }

    @Test void clientSyncedAssistantMessagesCannotProveAiExperience() {
        var conversation = conversations.save(com.histar.be.conversation.entity.Conversation.builder()
                .userId(user).characterId(UUID.randomUUID()).createdAt(java.time.Instant.now()).build());
        var reply = messages.save(com.histar.be.message.entity.Message.builder()
                .conversationId(conversation.getId()).role("assistant").content("Client-supplied transcript")
                .createdAt(java.time.Instant.now()).build());
        assertFalse(badgeContext.context(user, 500).chat());
        reply.setServerGenerated(true); messages.save(reply);
        assertTrue(badgeContext.context(user, 500).chat());
        assertFalse(badgeContext.context(account(500), 500).chat());
    }

    @Test void dioramaExperienceIsDurableWithoutAwardingPointsOrCompletingQuizzes() {
        var request = new ExploreService.ExperienceRequest(ExploreChallengeCatalog.LOCATION, "diorama");
        explore.experience(user, request);
        var restored = explore.experience(user, request);
        assertEquals(0, restored.profile().totalPoints());
        assertTrue(restored.discovery().keys().contains("feature:diorama"));
        assertFalse(restored.discovery().keys().contains("depth_quiz:surface"));
        assertThrows(BusinessRuleException.class, () -> explore.experience(user,
                new ExploreService.ExperienceRequest(ExploreChallengeCatalog.LOCATION, "all_badges")));
    }

    @Test void missingCatalogCannotReportASavedCompletionEvenForAdmin() {
        var admin = profiles.findById(user).orElseThrow();
        admin.setRole("ADMIN"); profiles.save(admin);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var point = catalog.findByUnlockKeyAndLocationId("depth_quiz:surface", ExploreChallengeCatalog.LOCATION).orElseThrow();
            catalog.delete(point); catalog.flush();
            assertThrows(BusinessRuleException.class, () -> submit(user, "depth_quiz:surface", 1));
            status.setRollbackOnly();
        });
        assertTrue(explore.snapshot(user).discovery().keys().isEmpty());
        assertEquals(0, explore.snapshot(user).profile().totalPoints());
    }

    @Test void everyCatalogQuizRejectsWrongOptionsAndAwardsExactlyOnce() {
        for (var entry : ExploreChallengeCatalog.QUIZZES.entrySet()) {
            UUID who = account(0);
            var challenge = entry.getValue();
            for (int answer = 0; answer < challenge.options(); answer++) {
                if (answer == challenge.correctOption()) continue;
                var wrong = submit(who, entry.getKey(), answer);
                assertFalse(wrong.passed(), entry.getKey());
                assertEquals(0, wrong.snapshot().profile().totalPoints(), entry.getKey());
                assertFalse(wrong.snapshot().discovery().keys().contains(entry.getKey()));
            }
            var correct = submit(who, entry.getKey(), challenge.correctOption());
            assertEquals(challenge.xp(), correct.xpEarned(), entry.getKey());
            assertEquals(challenge.xp(), correct.snapshot().profile().totalPoints());
            assertTrue(correct.snapshot().discovery().keys().contains(entry.getKey()));
            assertEquals(0, submit(who, entry.getKey(), challenge.correctOption()).xpEarned());
        }
    }
}
