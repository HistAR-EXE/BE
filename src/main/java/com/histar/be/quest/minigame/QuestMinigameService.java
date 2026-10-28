package com.histar.be.quest.minigame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.observability.ProductionMetrics;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestUnlockGate;
import com.histar.be.quest.service.QuestVisibility;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestMinigameService {

    private final QuestMinigameRepository minigameRepository;
    private final UserGameResultRepository resultRepository;
    private final QuestStepRepository questStepRepository;
    private final DiscoveryService discoveryService;
    private final ProfilePointsService profilePointsService;
    private final QuestCompletionService questCompletionService;
    private final QuestVisibility questVisibility;
    private final QuestUnlockGate questUnlockGate;
    private final ArtifactService artifactService;
    private final ObjectMapper objectMapper;
    private final ProductionMetrics productionMetrics;

    @Transactional(readOnly = true)
    public MinigameViewResponse open(UUID userId, UUID gameId) {
        QuestMinigame game = require(gameId);
        assertPlayable(userId, gameId);
        JsonNode config = readConfig(game);
        String lastLevel = resultRepository.findByUserIdAndGameId(userId, gameId)
                .map(UserGameResult::getLastLevelId)
                .orElse(null);
        JsonNode level = pickLevel(config.path("levels"), lastLevel);
        ObjectNode view = QuestMinigameGrader.publicLevel(game.getEngine(), (ObjectNode) level);
        return MinigameViewResponse.from(game.getId(), game.getEngine(), game.getTitle(), level.path("id").asText(), view, objectMapper);
    }

    @Transactional
    public MinigameAttemptResponse submit(UUID userId, UUID gameId, MinigameAttempt attempt) {
        profilePointsService.lockAccount(userId);
        QuestMinigame game = require(gameId);
        assertPlayable(userId, gameId);
        JsonNode config = readConfig(game);
        JsonNode level = findLevel(config.path("levels"), attempt.levelId());
        QuestMinigameGrader.Grade grade = QuestMinigameGrader.grade(game.getEngine(), config, level, attempt);
        productionMetrics.recordMinigameAttempt(grade.passed(), !grade.resolved());
        if (!grade.resolved()) {
            return new MinigameAttemptResponse(0, false, "", "", false, 0, 0, true, grade.hint());
        }
        resultRepository.upsert(UUID.randomUUID(), userId, gameId, grade.stars(), attempt.levelId());
        UserGameResult saved = resultRepository.findByUserIdAndGameId(userId, gameId).orElseThrow();

        int discoveryXp = 0;
        boolean recorded = false;
        if (grade.stars() >= 1) {
            List<QuestStep> steps = questStepRepository.findByMinigameId(gameId);
            for (QuestStep step : steps) {
                if (step.getQuest() == null || !questVisibility.visible(step.getQuest())) continue;
                try {
                    questUnlockGate.assertUnlocked(userId, step.getQuest());
                } catch (BusinessRuleException locked) {
                    // A shared game may be free through another quest. Do not
                    // record its Premium step until this quest is unlocked.
                    continue;
                }
                UUID locationId = step.getQuest().getLocationId();
                if (discoveryService.record(userId, step.getUnlockKey(), locationId)) {
                    recorded = true;
                    discoveryXp += profilePointsService.awardOnce(userId,
                            "discovery:" + locationId + ":" + step.getUnlockKey(), ProfilePointsService.XP_DISCOVERY);
                    questCompletionService.tryComplete(userId, locationId, CompletionTrigger.DISCOVERY);
                }
            }
        }
        if (grade.stars() >= 3) {
            artifactService.unlockRareByMinigame(userId, gameId);
        }
        return new MinigameAttemptResponse(
                grade.stars(),
                grade.passed(),
                grade.learnFact(),
                grade.learnUrl(),
                recorded,
                discoveryXp,
                saved.getAttempts(),
                false,
                Map.of());
    }

    private void assertPlayable(UUID userId, UUID gameId) {
        List<QuestStep> steps = questStepRepository.findByMinigameId(gameId);
        if (!questVisibility.gameVisible(steps)) {
            throw new ResourceNotFoundException("Minigame not found");
        }
        BusinessRuleException locked = null;
        for (QuestStep step : steps) {
            if (step.getQuest() == null || !questVisibility.visible(step.getQuest())) continue;
            try {
                questUnlockGate.assertUnlocked(userId, step.getQuest());
                return; // Shared mini games are playable when any linked quest is accessible.
            } catch (BusinessRuleException ex) {
                locked = ex;
            }
        }
        if (locked != null) throw locked;
    }

    private QuestMinigame require(UUID gameId) {
        return minigameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Minigame not found"));
    }

    private JsonNode readConfig(QuestMinigame game) {
        try {
            return objectMapper.readTree(game.getConfig());
        } catch (Exception ex) {
            throw new BusinessRuleException("Config mini game không đọc được");
        }
    }

    private JsonNode pickLevel(JsonNode levels, String lastLevelId) {
        if (!levels.isArray() || levels.isEmpty()) {
            throw new BusinessRuleException("Mini game chưa có mức chơi");
        }
        for (JsonNode level : levels) {
            if (!level.path("id").asText().equals(lastLevelId)) {
                return level;
            }
        }
        return levels.get(0);
    }

    private JsonNode findLevel(JsonNode levels, String levelId) {
        if (levelId == null || !levels.isArray()) {
            throw new BusinessRuleException("Thiếu mức chơi");
        }
        for (JsonNode level : levels) {
            if (levelId.equals(level.path("id").asText())) {
                return level;
            }
        }
        throw new BusinessRuleException("Mức chơi không hợp lệ");
    }
}
