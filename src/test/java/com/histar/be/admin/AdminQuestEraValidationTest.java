package com.histar.be.admin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.histar.be.admin.dto.AdminQuestRequest;
import com.histar.be.admin.service.AdminContentService;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.location.service.LocationEraValidationService;
import com.histar.be.quest.repository.QuestRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminQuestEraValidationTest {

    @Mock
    private DiscoveryPointRepository discoveryPointRepository;

    @Mock
    private ArtifactRepository artifactRepository;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private LocationEraValidationService locationEraValidationService;

    @InjectMocks
    private AdminContentService adminContentService;

    @Test
    void createQuest_locationWithFewerThanThreeEras_rejected() {
        UUID locationId = UUID.randomUUID();
        doThrow(new BusinessRuleException("Mỗi di tích cần ít nhất 3 thời kỳ (era)"))
                .when(locationEraValidationService)
                .ensureMinimumThreeEras(locationId);

        AdminQuestRequest request = new AdminQuestRequest(
                locationId,
                "Should Fail",
                "desc",
                "story",
                10,
                0,
                "discovery",
                false,
                1,
                null,
                "key:a");

        assertThrows(BusinessRuleException.class, () -> adminContentService.createQuest(request));
        verify(questRepository, never()).save(any());
    }
}
