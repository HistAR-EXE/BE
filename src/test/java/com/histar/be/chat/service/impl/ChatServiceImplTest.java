package com.histar.be.chat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.character.entity.CharacterEntity;
import com.histar.be.character.service.CharacterService;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.service.ChatLlmClient;
import com.histar.be.chat.service.PlayerStoryContextService;
import com.histar.be.chat.service.RagAiChatClient;
import com.histar.be.chat.service.RagChatResponse;
import com.histar.be.conversation.entity.Conversation;
import com.histar.be.conversation.repository.ConversationRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private CharacterService characterService;
    @Mock
    private LocationService locationService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChatLlmClient chatLlmClient;
    @Mock
    private RagAiChatClient ragAiChatClient;
    @Mock
    private ChatRateLimiter chatRateLimiter;
    @Mock
    private UsageQuotaService usageQuotaService;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private UserArtifactRepository userArtifactRepository;
    @Mock
    private UserDiscoveryRepository userDiscoveryRepository;
    @Mock
    private PlayerStoryContextService playerStoryContextService;
    @Mock
    private EmailVerifiedGuard emailVerifiedGuard;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void sendOrchestrated_stripsSourcesForFreeUser() {
        UUID userId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        CharacterEntity character = CharacterEntity.builder()
                .id(characterId)
                .name("Chị Năm")
                .locationId(locationId)
                .personaPrompt("persona")
                .build();
        Location location = Location.builder()
                .id(locationId)
                .name("Củ Chi")
                .sources("Bảo tàng Chứng tích Chiến tranh")
                .build();
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .userId(userId)
                .characterId(characterId)
                .build();

        when(characterService.findById(characterId)).thenReturn(character);
        when(locationService.findById(locationId)).thenReturn(location);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        when(userArtifactRepository.findByUserId(userId)).thenReturn(List.of());
        when(userDiscoveryRepository.findByUserId(userId)).thenReturn(List.of());
        when(playerStoryContextService.build(userId, locationId)).thenReturn(Map.of());
        when(ragAiChatClient.generateWithSources(
                        any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(), any()))
                .thenReturn(new RagChatResponse(
                        "reply",
                        List.of(new ChatSource("Nguồn A", "excerpt", "https://example.com"))));
        when(usageQuotaService.shouldIncludeChatSources(userId)).thenReturn(false);

        var response = chatService.sendOrchestrated(
                userId, new ChatMessageRequest(characterId, "hello", conversationId));

        assertThat(response.sources()).isEmpty();
        verify(usageQuotaService).shouldIncludeChatSources(userId);
    }

    @Test
    void sendOrchestrated_keepsSourcesForPremiumUser() {
        UUID userId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        CharacterEntity character = CharacterEntity.builder()
                .id(characterId)
                .name("Chị Năm")
                .locationId(locationId)
                .personaPrompt("persona")
                .build();
        Location location = Location.builder().id(locationId).name("Củ Chi").build();
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .userId(userId)
                .characterId(characterId)
                .build();
        List<ChatSource> sources = List.of(new ChatSource("Nguồn A", "excerpt", "https://example.com"));

        when(characterService.findById(characterId)).thenReturn(character);
        when(locationService.findById(locationId)).thenReturn(location);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        when(userArtifactRepository.findByUserId(userId)).thenReturn(List.of());
        when(userDiscoveryRepository.findByUserId(userId)).thenReturn(List.of());
        when(playerStoryContextService.build(userId, locationId)).thenReturn(Map.of());
        when(ragAiChatClient.generateWithSources(
                        any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(), any()))
                .thenReturn(new RagChatResponse("reply", sources));
        when(usageQuotaService.shouldIncludeChatSources(userId)).thenReturn(true);

        var response = chatService.sendOrchestrated(
                userId, new ChatMessageRequest(characterId, "hello", conversationId));

        assertThat(response.sources()).hasSize(1);
    }
}
