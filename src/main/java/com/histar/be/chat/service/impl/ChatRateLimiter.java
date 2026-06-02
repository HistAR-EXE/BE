package com.histar.be.chat.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.message.repository.MessageRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatRateLimiter {

    private final int dailyLimit;
    private final MessageRepository messageRepository;

    public ChatRateLimiter(
            @Value("${gemini.daily-message-limit}") int dailyLimit, MessageRepository messageRepository) {
        this.dailyLimit = dailyLimit;
        this.messageRepository = messageRepository;
    }

    public void checkAndIncrement(UUID userId) {
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC);
        ZonedDateTime startUtc = utcDay.atStartOfDay(ZoneOffset.UTC);
        ZonedDateTime endUtc = startUtc.plusDays(1);

        long currentCount = messageRepository.countUserMessagesByUserIdAndCreatedAtBetween(
                userId, startUtc.toInstant(), endUtc.toInstant());
        if (currentCount >= dailyLimit) {
            throw new BusinessRuleException("Đã đạt giới hạn " + dailyLimit + " tin nhắn/ngày");
        }
    }
}
