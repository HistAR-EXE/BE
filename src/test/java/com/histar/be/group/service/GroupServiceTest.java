package com.histar.be.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.histar.be.billing.service.MultiplayerAccessService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private StudyGroupMemberRepository studyGroupMemberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserQuestProgressRepository userQuestProgressRepository;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private MultiplayerAccessService multiplayerAccessService;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        HistarOrgProperties orgProperties = new HistarOrgProperties();
        orgProperties.getGroup().setMaxGroupsPerUser(20);
        groupService = new GroupService(
                studyGroupRepository,
                studyGroupMemberRepository,
                profileRepository,
                userQuestProgressRepository,
                questRepository,
                orgProperties,
                multiplayerAccessService,
                null);
    }

    @Test
    void joinGroup_rejectsExpiredCode() {
        UUID userId = UUID.randomUUID();
        StudyGroup group = StudyGroup.builder()
                .id(UUID.randomUUID())
                .name("Expired Room")
                .code("ABC123")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.now().minusSeconds(86_400))
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        when(studyGroupRepository.findByCodeIgnoreCase("ABC123")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.joinGroup(userId, "ABC123"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE);
                    assertThat(bre.getMessage()).contains("hết hạn");
                });
    }

    @Test
    void joinGroup_rejectsInvalidLength() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> groupService.joinGroup(userId, "AB"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("6 ký tự");
    }
}
