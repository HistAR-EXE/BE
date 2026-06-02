package com.histar.be.chat.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatRateLimiter {

    private final int dailyLimit;
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    public ChatRateLimiter(@Value("${gemini.daily-message-limit}") int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public void checkAndIncrement(UUID userId) {
        String key = userId + ":" + LocalDate.now();
        AtomicInteger counter = counts.computeIfAbsent(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > dailyLimit) {
            throw new BusinessRuleException("Đã đạt giới hạn " + dailyLimit + " tin nhắn/ngày");
        }
    }
}
