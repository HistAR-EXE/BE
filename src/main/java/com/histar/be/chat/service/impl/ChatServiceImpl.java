package com.histar.be.chat.service.impl;

import com.histar.be.character.entity.CharacterEntity;
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
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.message.entity.Message;
import com.histar.be.message.repository.MessageRepository;
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

    private static final String OUT_OF_SCOPE_REPLY =
            "Mình chưa có thông tin chính xác về điều này. Bạn nên tham khảo tài liệu chính thức từ Ban quản lý di tích.";

    private final CharacterService characterService;
    private final LocationService locationService;
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

        Location location = resolveLocation(character.getLocationId());
        String prompt = buildPrompt(character.getPersonaPrompt(), location, conversation.getId());
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

    private String resolveCiteSources(Location location) {
        if (location.getSources() != null && !location.getSources().isBlank()) {
            return location.getSources();
        }
        String name = location.getName() != null ? location.getName() : "di tích lịch sử";
        return "Khu di tích lịch sử " + name;
    }
}
