package com.histar.be.chat.service.impl;

import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.character.entity.CharacterEntity;
import com.histar.be.character.service.CharacterService;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.chat.dto.ChatContextResponse;
import com.histar.be.chat.dto.ChatHistoryTurn;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.dto.ChatSyncRequest;
import com.histar.be.chat.dto.MessageResponse;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.service.ChatLlmClient;
import com.histar.be.chat.service.PersonaMapper;
import com.histar.be.chat.service.PlayerStoryContextService;
import com.histar.be.chat.service.RagAiChatClient;
import com.histar.be.chat.service.RagChatResponse;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.conversation.entity.Conversation;
import com.histar.be.conversation.repository.ConversationRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.message.entity.Message;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.rag.dto.RagChatAnswer;
import com.histar.be.rag.dto.RagChatRequest;
import com.histar.be.rag.service.RagChatService;
import java.util.Arrays;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final int MAX_HISTORY_TURNS = 6;

    private static final String OUT_OF_SCOPE_REPLY =
            "Mình chưa có thông tin chính xác về điều này. Bạn nên tham khảo tài liệu chính thức từ Ban quản lý di tích.";

    private final CharacterService characterService;
    private final LocationService locationService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatLlmClient chatLlmClient;
    private final RagAiChatClient ragAiChatClient;
    private final ChatRateLimiter chatRateLimiter;
    private final UsageQuotaService usageQuotaService;
    private final ProfileRepository profileRepository;
    private final UserArtifactRepository userArtifactRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final PlayerStoryContextService playerStoryContextService;
    private final EmailVerifiedGuard emailVerifiedGuard;
    private final RagChatService ragChatService;

    private void assertEmailVerified(UUID userId) {
        emailVerifiedGuard.assertEmailVerified(userId);
    }

    @Override
    @Transactional
    public ChatResponse chat(UUID userId, ChatRequest request) {
        if (userId == null) {
            throw new AuthException("Unauthorized");
        }
        assertEmailVerified(userId);
        chatRateLimiter.checkAndIncrement(userId);

        CharacterEntity character = characterService.findById(request.characterId());
        Conversation conversation = resolveConversation(userId, request, character.getId());

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("user")
                .content(request.message())
                .createdAt(Instant.now())
                .build());

        Location location = resolveLocation(character.getLocationId());
        String prompt = buildPrompt(character.getPersonaPrompt(), location, conversation.getId());
        String reply = chatLlmClient.generate(prompt);

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("assistant")
                .content(reply)
                .createdAt(Instant.now())
                .build());

        chatRateLimiter.recordSuccess(userId);
        String siteCode = location.getSiteCode();
        return new ChatResponse(
                reply, conversation.getId(), filterSourcesForUser(userId, siteCode, resolveSourcesList(location)));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatContextResponse getContext(UUID userId, UUID characterId, UUID conversationId) {
        if (userId == null) {
            throw new AuthException("Unauthorized");
        }
        CharacterEntity character = characterService.findById(characterId);
        Location location = resolveLocation(character.getLocationId());

        Conversation conversation = resolveContextConversation(userId, characterId, conversationId);
        List<ChatHistoryTurn> history = conversation != null
                ? loadHistoryTurns(conversation.getId())
                : Collections.emptyList();

        return new ChatContextResponse(
                conversation != null ? conversation.getId() : null,
                PersonaMapper.resolvePersonaKey(character.getName()),
                Collections.emptyMap(),
                location.getKnowledgeContext() != null ? location.getKnowledgeContext() : "",
                resolveCiteSources(location),
                character.getLocationId(),
                history,
                playerStoryContextService.build(userId, character.getLocationId()));
    }

    @Override
    @Transactional
    public ChatResponse sync(UUID userId, ChatSyncRequest request) {
        if (userId == null) {
            throw new AuthException("Unauthorized");
        }
        assertEmailVerified(userId);
        chatRateLimiter.checkAndIncrement(userId);

        CharacterEntity character = characterService.findById(request.characterId());
        ChatRequest convRequest = new ChatRequest(request.characterId(), request.userMessage(), request.conversationId());
        Conversation conversation = resolveConversation(userId, convRequest, character.getId());

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("user")
                .content(request.userMessage())
                .createdAt(Instant.now())
                .build());

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("assistant")
                .content(request.assistantReply())
                .createdAt(Instant.now())
                .build());

        chatRateLimiter.recordSuccess(userId);
        log.info("chat sync userId={} characterId={} conversationId={}", userId, request.characterId(), conversation.getId());
        return new ChatResponse(request.assistantReply(), conversation.getId(), List.of());
    }

    @Override
    @Transactional
    public ChatResponse sendOrchestrated(UUID userId, ChatMessageRequest request) {
        if (userId == null) {
            throw new AuthException("Unauthorized");
        }
        assertEmailVerified(userId);
        chatRateLimiter.checkAndIncrement(userId);

        CharacterEntity character = characterService.findById(request.characterId());
        Location location = resolveLocation(character.getLocationId());
        ChatRequest convRequest =
                new ChatRequest(request.characterId(), request.message(), request.conversationId());
        Conversation conversation = resolveConversation(userId, convRequest, character.getId());
        List<ChatHistoryTurn> historyTurns = loadHistoryTurns(conversation.getId());

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("user")
                .content(request.message())
                .createdAt(Instant.now())
                .build());
        List<Map<String, String>> historyPayload = historyTurns.stream()
                .map(turn -> Map.of("role", turn.role(), "content", turn.content()))
                .collect(Collectors.toList());

        Profile profile = profileRepository.findById(userId).orElse(null);
        int userLevel = profile != null && profile.getLevel() != null ? profile.getLevel() : 1;
        long artifactsUnlocked = userArtifactRepository.findByUserId(userId).size();
        long discoveriesCount = userDiscoveryRepository.findByUserId(userId).size();

        Map<String, Object> playerContext =
                playerStoryContextService.build(userId, character.getLocationId());

        String reply;
        List<ChatSource> responseSources;
        boolean pgVectorRagUsed = false;
        String siteCode = request.siteCode() != null && !request.siteCode().isBlank()
                ? request.siteCode().trim().toLowerCase()
                : location.getSiteCode();
        Optional<RagChatAnswer> pgVectorAnswer = ragChatService.tryAnswer(new RagChatRequest(
                request.message(),
                request.stationCode(),
                siteCode,
                PersonaMapper.resolvePersonaKey(character.getName()),
                character.getPersonaPrompt(),
                location.getName(),
                historyTurns));
        if (pgVectorAnswer != null && pgVectorAnswer.isPresent()) {
            // B5 pgvector RAG: verified chunks + [n] citations (or soft refusal when nothing is citable).
            reply = pgVectorAnswer.get().reply();
            responseSources = pgVectorAnswer.get().sources();
            pgVectorRagUsed = true;
        } else {
            try {
                RagChatResponse ragResponse = ragAiChatClient.generateWithSources(
                        request.message(),
                        PersonaMapper.resolvePersonaKey(character.getName()),
                        Collections.emptyMap(),
                        location.getKnowledgeContext() != null ? location.getKnowledgeContext() : "",
                        resolveCiteSources(location),
                        character.getLocationId(),
                        historyPayload,
                        userLevel,
                        artifactsUnlocked,
                        discoveriesCount,
                        playerContext);
                reply = ragResponse.reply();
                responseSources = !ragResponse.sources().isEmpty()
                        ? ragResponse.sources()
                        : resolveSourcesList(location);
            } catch (BusinessRuleException ex) {
                log.warn("RAG AI unavailable, falling back to direct LLM: {}", ex.getMessage());
                String prompt = buildPrompt(character.getPersonaPrompt(), location, conversation.getId());
                reply = chatLlmClient.generate(prompt);
                responseSources = resolveSourcesList(location);
            }
        }
        List<ChatSource> visibleSources = filterSourcesForUser(userId, siteCode, responseSources);
        if (pgVectorRagUsed && visibleSources.isEmpty() && !responseSources.isEmpty()) {
            // Free tier does not show sources: drop the dangling [n] markers too.
            reply = reply.replaceAll("\\s*\\[\\d{1,2}]", "");
        }

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("assistant")
                .content(reply)
                .createdAt(Instant.now())
                .build());

        log.info(
                "chat orchestrated userId={} characterId={} conversationId={}",
                userId,
                request.characterId(),
                conversation.getId());
        chatRateLimiter.recordSuccess(userId);
        return new ChatResponse(reply, conversation.getId(), visibleSources);
    }

    private List<ChatSource> filterSourcesForUser(UUID userId, String siteCode, List<ChatSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return usageQuotaService.shouldIncludeChatSources(userId, siteCode) ? sources : List.of();
    }

    @Override
    public Page<MessageResponse> getMessages(UUID userId, UUID conversationId, Pageable pageable) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        if (!conversation.getUserId().equals(userId)) {
            throw new AuthException("Forbidden");
        }
        return messageRepository.findByConversationId(conversationId, pageable).map(MessageResponse::from);
    }

    private Location resolveLocation(UUID locationId) {
        if (locationId == null) {
            return Location.builder().name("di tích lịch sử").build();
        }
        return locationService.findById(locationId);
    }

    private Conversation resolveConversation(UUID userId, ChatRequest request, UUID characterId) {
        if (request.conversationId() != null) {
            Conversation existing = conversationRepository
                    .findById(request.conversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            if (!existing.getUserId().equals(userId)) {
                throw new AuthException("Forbidden");
            }
            return existing;
        }
        return conversationRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .userId(userId)
                        .characterId(characterId)
                        .createdAt(Instant.now())
                        .build()));
    }

    private String buildPrompt(String personaPrompt, Location location, UUID conversationId) {
        String locationName = location.getName() != null ? location.getName() : "di tích lịch sử";
        String citeSources = resolveCiteSources(location);

        StringBuilder prompt = new StringBuilder(personaPrompt)
                .append("\n\n")
                .append("Bạn là hướng dẫn viên lịch sử tại ")
                .append(locationName)
                .append(". Chỉ trả lời trong phạm vi lịch sử liên quan đến ")
                .append(locationName)
                .append(" và nhân vật bạn đang đóng.\n\n")
                .append("QUY TẮC BẮT BUỘC:\n")
                .append("1. Cuối MỖI câu trả lời, thêm một dòng riêng bắt đầu bằng \"Nguồn: \" và chọn một trong các nguồn hợp lệ sau: ")
                .append(citeSources)
                .append(".\n")
                .append("2. Nếu câu hỏi NGOÀI phạm vi dữ kiện được cung cấp hoặc bạn không chắc chắn, trả lời chính xác: \"")
                .append(OUT_OF_SCOPE_REPLY)
                .append("\" — TUYỆT ĐỐI KHÔNG bịa số liệu, ngày tháng hay sự kiện.\n")
                .append("3. Giữ giọng nhân vật trong persona ở trên (thân thiện, xưng hô phù hợp).\n")
                .append("4. Chỉ dựa trên dữ kiện lịch sử có thật; nếu không rõ, nói thật là không rõ.");

        if (location.getKnowledgeContext() != null && !location.getKnowledgeContext().isBlank()) {
            prompt.append("\n\nDỮ KIỆN ĐÃ XÁC MINH (ưu tiên, trả lời ngắn gọn):\n")
                    .append(location.getKnowledgeContext().trim());
        }

        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAt(conversationId);
        int start = Math.max(0, history.size() - 10);
        if (!history.isEmpty()) {
            prompt.append("\n\nLịch sử hội thoại gần đây:\n");
            for (int i = start; i < history.size(); i++) {
                Message message = history.get(i);
                String speaker = "user".equals(message.getRole()) ? "Người dùng" : "Bạn";
                prompt.append(speaker).append(": ").append(message.getContent()).append("\n");
            }
        }
        return prompt.toString();
    }

    private Conversation resolveContextConversation(UUID userId, UUID characterId, UUID conversationId) {
        if (conversationId != null) {
            Conversation existing = conversationRepository
                    .findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            if (!existing.getUserId().equals(userId)) {
                throw new AuthException("Forbidden");
            }
            return existing;
        }
        return conversationRepository.findByUserIdAndCharacterId(userId, characterId).orElse(null);
    }

    private List<ChatHistoryTurn> loadHistoryTurns(UUID conversationId) {
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAt(conversationId);
        int start = Math.max(0, history.size() - MAX_HISTORY_TURNS);
        List<ChatHistoryTurn> turns = new ArrayList<>();
        for (int i = start; i < history.size(); i++) {
            Message message = history.get(i);
            if ("user".equals(message.getRole()) || "assistant".equals(message.getRole())) {
                turns.add(new ChatHistoryTurn(message.getRole(), message.getContent()));
            }
        }
        return turns;
    }

    private String resolveCiteSources(Location location) {
        if (location.getSources() != null && !location.getSources().isBlank()) {
            return location.getSources();
        }
        String name = location.getName() != null ? location.getName() : "di tích lịch sử";
        return "Khu di tích lịch sử " + name;
    }

    private List<ChatSource> resolveSourcesList(Location location) {
        String sources = resolveCiteSources(location);
        if (sources == null || sources.isBlank()) {
            return List.of();
        }
        return Arrays.stream(sources.split("[\\n,;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::toChatSource)
                .toList();
    }

    private ChatSource toChatSource(String title) {
        String excerpt = title.length() > 120 ? title.substring(0, 117) + "..." : title;
        return new ChatSource(title, excerpt, null);
    }
}
