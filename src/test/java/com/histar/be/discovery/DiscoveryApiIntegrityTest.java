package com.histar.be.discovery;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.discovery.controller.DiscoveryController;
import com.histar.be.discovery.dto.RecordDiscoveryRequest;
import com.histar.be.discovery.service.DirectDiscoveryPolicy;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.quest.repository.QuestStepRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.histar.be.organization.service.TrialEntitlementGuard;

@ExtendWith(MockitoExtension.class)
class DiscoveryApiIntegrityTest {
    @Mock CurrentUserAccessor users;
    @Mock DiscoveryService discoveries;
    @Mock TrialEntitlementGuard trialEntitlementGuard;
    // Không có bước "xem hiện vật" nào trong DB giả → mọi khóa artifact: đều bị từ chối.
    @Spy DirectDiscoveryPolicy directDiscoveryPolicy = new DirectDiscoveryPolicy(mock(QuestStepRepository.class));
    @InjectMocks DiscoveryController controller;

    @ParameterizedTest
    @ValueSource(strings = {"depth_quiz:surface", "cipher:hoang-cam", "dossier:architecture",
            "clue:ev-01", "investigation:cu-chi-conclusion", "minigame:xep-tang", "feature:ai_chat",
            "dialogue:anh-ba", "bonus:tour:sa-ban", "artifact:cuoc-chim"})
    void rawCompletionKeyCannotEarnRewards(String key) {
        when(users.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        assertThrows(BusinessRuleException.class, () -> controller.record(
                new RecordDiscoveryRequest(key, "online", UUID.randomUUID())));
        verifyNoInteractions(discoveries);
    }
}
