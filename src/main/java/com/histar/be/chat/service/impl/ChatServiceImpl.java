package com.histar.be.chat.service.impl;

import com.histar.be.character.service.CharacterService;
import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.MessageResponse;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.service.GeminiClient;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.conversation.entity.Conversation;
import com.histar.be.conversation.repository.ConversationRepository;
import com.histar.be.message.entity.Message;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.character.entity.CharacterEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String GUARDRAIL =
            "QUAN TRỌNG: Chỉ trả lời dựa trên dữ kiện lịch sử có thật. Nếu không chắc, hãy nói thật là không rõ, TUYỆT ĐỐI không bịa.";

    private final CharacterService characterService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final GeminiClient geminiClient;
    private final ChatRateLimiter chatRateLimiter;

    @Override
    @Transactional
    public ChatResponse chat(UUID userId, ChatRequest request) {
        if (userId == null) {
            throw new AuthException("Unauthorized");
        }
        chatRateLimiter.checkAndIncrement(userId);

        CharacterEntity character = characterService.findById(request.characterId());
        Conversation conversation = resolveConversation(userId, request, character.getId());

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("user")
                .content(request.message())
                .createdAt(Instant.now())
                .build());

        String prompt = buildPrompt(character.getPersonaPrompt(), conversation.getId());
        String reply = geminiClient.generate(prompt);

        messageRepository.save(Message.builder()
                .conversationId(conversation.getId())
                .role("assistant")
                .content(reply)
                .createdAt(Instant.now())
                .build());

        return new ChatResponse(reply, conversation.getId());
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

    private String buildPrompt(String personaPrompt, UUID conversationId) {
        StringBuilder prompt = new StringBuilder(personaPrompt).append("\n\n").append(GUARDRAIL);
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
}
