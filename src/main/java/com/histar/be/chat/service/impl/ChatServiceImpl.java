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
import com.histar.be.chat.dto.StationChatContext;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.service.PreparedChatResponse;
import com.histar.be.chat.service.ChatStreamChunk;
import com.histar.be.chat.service.PersonaMapper;
import com.histar.be.chat.service.PlayerStoryContextService;
import com.histar.be.chat.service.RagAiChatClient;
import com.histar.be.chat.service.RagChatResponse;
import com.histar.be.chat.service.StationChatContextResolver;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.ErrorCode;
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
import com.histar.be.quest.narrative.NarrativeInvestigationService;
import com.histar.be.observability.ProductionMetrics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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

    private final CharacterService characterService;
    private final LocationService locationService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RagAiChatClient ragAiChatClient;
    private final ChatRateLimiter chatRateLimiter;
    private final UsageQuotaService usageQuotaService;
    private final ProfileRepository profileRepository;
    private final UserArtifactRepository userArtifactRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final PlayerStoryContextService playerStoryContextService;
    private final EmailVerifiedGuard emailVerifiedGuard;
    private final RagChatService ragChatService;
    private final ProductionMetrics productionMetrics;
    private final NarrativeInvestigationService narrativeInvestigationService;
    private final StationChatContextResolver stationChatContextResolver;

    private void assertEmailVerified(UUID userId) {
        emailVerifiedGuard.assertEmailVerified(userId);
    }

    @Override
    @Transactional
    public ChatResponse chat(UUID userId, ChatRequest request) {
        return sendOrchestrated(userId,
                new ChatMessageRequest(request.characterId(), request.message(), request.conversationId()));
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

        CharacterEntity character = characterService.findEnabledChatAgentById(request.characterId());
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
        PreparedChatResponse prepared = prepareOrchestrated(userId, request);
        return persistPrepared(userId, request, prepared);
    }

    @Override
    @Transactional
    public PreparedChatResponse prepareOrchestrated(UUID userId, ChatMessageRequest request) {
        return prepareOrchestrated(userId, request, null);
    }

    @Override
    @Transactional
    public PreparedChatResponse prepareOrchestratedStreaming(UUID userId, ChatMessageRequest request, Consumer<ChatStreamChunk> publisher) {
        return prepareOrchestrated(userId, request, publisher);
    }

    private PreparedChatResponse prepareOrchestrated(UUID userId, ChatMessageRequest request, Consumer<ChatStreamChunk> publisher) {
        if (userId == null) {
            throw new AuthException("Unauthorized");
        }
        assertEmailVerified(userId);
        boolean questHint = com.histar.be.chat.service.QuestChatHints.isExempt(request.message());
        if (!questHint) {
            chatRateLimiter.checkAndIncrement(userId);
        }
        long startedNanos = System.nanoTime();

        CharacterEntity character = characterService.findEnabledChatAgentById(request.characterId());
        Location location = resolveLocation(character.getLocationId());
        ChatRequest convRequest =
                new ChatRequest(request.characterId(), request.message(), request.conversationId());
        Conversation conversation = resolveConversation(userId, convRequest, character.getId());
        List<ChatHistoryTurn> historyTurns = loadHistoryTurns(conversation.getId());

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
        String answerSource = "rag-ai";
        boolean pgVectorRagUsed = false;
        String locationSiteCode = location.getSiteCode() == null
                ? ""
                : location.getSiteCode().trim().toLowerCase(Locale.ROOT);
        String siteCode = request.siteCode() != null && !request.siteCode().isBlank()
                ? request.siteCode().trim().toLowerCase(Locale.ROOT)
                : locationSiteCode;
        if (!siteCode.equals(locationSiteCode)) {
            throw new BaseException(
                    ErrorCode.INVALID_STATION_CONTEXT,
                    "Địa điểm và site của trạm không khớp");
        }
        StationChatContext stationContext = null;
        if (request.stationCode() != null && !request.stationCode().isBlank()
                && stationChatContextResolver != null) {
            stationContext = stationChatContextResolver
                    .resolve(siteCode, request.stationCode())
                    .orElse(null);
        }
        String verifiedStationCode = stationContext == null
                ? request.stationCode()
                : stationContext.stationCode();
        String verifiedSiteCode = stationContext == null
                ? siteCode
                : stationContext.siteCode();
        RagChatRequest ragRequest = new RagChatRequest(
                request.message(),
                verifiedStationCode,
                verifiedSiteCode,
                PersonaMapper.resolvePersonaKey(character.getName()),
                character.getPersonaPrompt(),
                location.getName(), historyTurns, stationContext);
        Optional<RagChatAnswer> pgVectorAnswer = publisher == null
                ? ragChatService.tryAnswer(ragRequest)
                : ragChatService.tryAnswerStreaming(ragRequest, chunk -> {
                    List<ChatSource> visible = filterSourcesForUser(userId, siteCode, chunk.sources());
                    if (!visible.isEmpty()) publisher.accept(new ChatStreamChunk(chunk.content(), visible));
                });
        if (pgVectorAnswer != null && pgVectorAnswer.isPresent()) {
            // B5 pgvector RAG: verified chunks + [n] citations (or soft refusal when nothing is citable).
            reply = pgVectorAnswer.get().reply();
            responseSources = pgVectorAnswer.get().sources();
            answerSource = "semantic";
            pgVectorRagUsed = true;
        } else {
            try {
                RagChatResponse ragResponse = stationContext == null
                        ? ragAiChatClient.generateWithSources(
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
                                playerContext)
                        : ragAiChatClient.generateWithSources(
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
                                playerContext,
                                stationContext);
                reply = ragResponse.reply();
                answerSource = publicAnswerSource(ragResponse.cache());
                responseSources = ragResponse.sources();
            } catch (RuntimeException ex) {
                recordChatAnswer(answerSource, startedNanos, false);
                throw ex;
            }
        }
        List<ChatSource> visibleSources = filterSourcesForUser(userId, siteCode, responseSources);
        if (pgVectorRagUsed && visibleSources.isEmpty() && !responseSources.isEmpty()) {
            // Free tier does not show sources: drop the dangling [n] markers too.
            reply = reply.replaceAll("\\s*\\[\\d{1,2}]", "");
        }

        return new PreparedChatResponse(conversation.getId(), reply, visibleSources, answerSource, questHint, startedNanos);
    }

    @Override
    @Transactional
    public ChatResponse persistPrepared(UUID userId, ChatMessageRequest request, PreparedChatResponse prepared) {
        if (userId == null || prepared == null) {
            throw new AuthException("Unauthorized");
        }
        Conversation conversation = conversationRepository.findById(prepared.conversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        if (!conversation.getUserId().equals(userId)) {
            throw new AuthException("Forbidden");
        }
        messageRepository.save(Message.builder().conversationId(conversation.getId()).role("user")
                .content(request.message()).createdAt(Instant.now()).build());
        messageRepository.save(Message.builder().conversationId(conversation.getId()).role("assistant").serverGenerated(true)
                .content(prepared.reply()).createdAt(Instant.now()).build());
        if (request.narrativeQuestId() != null && request.narrativeNodeCode() != null && request.narrativeVersion() != null) {
            narrativeInvestigationService.recordChatInteraction(userId, request.narrativeQuestId(),
                    request.narrativeNodeCode(), request.narrativeVersion());
        }
        log.info("chat orchestrated userId={} characterId={} conversationId={}", userId, request.characterId(), conversation.getId());
        recordChatAnswer(prepared.answerSource(), prepared.startedNanos(), true);
        if (!prepared.questHint()) {
            chatRateLimiter.recordSuccess(userId);
        }
        return new ChatResponse(prepared.reply(), conversation.getId(), prepared.sources());
    }

    private static String publicAnswerSource(String cache) {
        if (cache == null || cache.isBlank()) {
            return "rag-ai";
        }
        return switch (cache) {
            case "curated", "faq", "no_source", "guard" -> cache;
            case "semantic_cache", "semantic" -> "semantic";
            case "llm", "gemini" -> "rag-ai";
            default -> "rag-ai";
        };
    }

    private void recordChatAnswer(String source, long startedNanos, boolean successful) {
        long latencyMs = (System.nanoTime() - startedNanos) / 1_000_000L;
        productionMetrics.recordChat(java.time.Duration.ofMillis(latencyMs), source, successful);
        log.info("requestId={} answer_source={} latency_ms={} outcome={}",
                org.slf4j.MDC.get("requestId"), source, latencyMs, successful ? "success" : "error");
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
        // Missing metadata is not evidence. The Python RAG returns citations from the
        // same retrieved chunks as the answer; never synthesize a location-name source.
        return "";
    }

}
