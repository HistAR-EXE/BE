package com.histar.be.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.histar.be.character.entity.CharacterEntity;
import com.histar.be.character.repository.CharacterRepository;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.service.RagAiChatClient;
import com.histar.be.chat.service.RagChatResponse;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ContextConfiguration(classes = ChatSourcesResolutionTest.RagStubConfiguration.class)
class ChatSourcesResolutionTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private RagAiChatClient ragAiChatClient;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID userId;
    private UUID characterId;

    @BeforeEach
    void setUp() {
        Mockito.reset(ragAiChatClient);
        Profile profile = profileRepository.save(Profile.builder()
                .email("chat-src-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Chat Source Tester")
                .provider("local")
                .role(UserRole.USER.name())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();

        Location location = locationRepository.save(Location.builder()
                .name("Chat Source Location")
                .latitude(11.0)
                .longitude(106.0)
                .city("Test")
                .sources("Fallback Source A; Fallback Source B")
                .createdAt(Instant.now())
                .build());

        CharacterEntity character = characterRepository.save(CharacterEntity.builder()
                .locationId(location.getId())
                .name("Test Guide")
                .era("2026")
                .personaPrompt("You are a guide.")
                .build());
        characterId = character.getId();
    }

    @Test
    void sendOrchestrated_prefersRagSourcesOverMetadata() {
        when(ragAiChatClient.generateWithSources(
                        anyString(),
                        anyString(),
                        anyMap(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        anyInt(),
                        anyLong(),
                        anyLong(),
                        any()))
                .thenReturn(new RagChatResponse(
                        "Reply from RAG",
                        List.of(new ChatSource("Chroma Doc", "Excerpt from vector store", null))));

        ChatResponse response = chatService.sendOrchestrated(
                userId, new ChatMessageRequest(characterId, "Xin chào", null));

        assertThat(response.reply()).isEqualTo("Reply from RAG");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).title()).isEqualTo("Chroma Doc");
    }

    @Test
    void sendOrchestrated_fallsBackToLocationMetadataWhenRagEmpty() {
        when(ragAiChatClient.generateWithSources(
                        anyString(),
                        anyString(),
                        anyMap(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        anyInt(),
                        anyLong(),
                        anyLong(),
                        any()))
                .thenReturn(new RagChatResponse("Reply only", List.of()));

        ChatResponse response = chatService.sendOrchestrated(
                userId, new ChatMessageRequest(characterId, "Hello", null));

        assertThat(response.sources()).isNotEmpty();
        assertThat(response.sources().get(0).title()).contains("Fallback");
    }

    @TestConfiguration
    static class RagStubConfiguration {
        @Bean
        @Primary
        RagAiChatClient ragAiChatClient() {
            return Mockito.mock(RagAiChatClient.class);
        }
    }
}
