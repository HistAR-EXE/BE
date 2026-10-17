package com.histar.be.chat.service.impl;

import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.common.exception.RateLimitedException;
import com.histar.be.common.ratelimit.ChatBurstRateLimiter;
import com.histar.be.common.ratelimit.RateLimitDecision;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    private final UsageQuotaService usageQuotaService;
    private final ChatBurstRateLimiter chatBurstRateLimiter;

    public void checkAndIncrement(UUID userId) {
        usageQuotaService.assertCanSendChat(userId);
        if (userId != null) {
            RateLimitDecision decision = chatBurstRateLimiter.tryAcquire(userId);
            if (!decision.allowed()) {
                throw new RateLimitedException(
                        "Bạn đang gửi tin nhắn quá nhanh. Vui lòng chờ " + decision.retryAfterSeconds() + " giây.",
                        decision.retryAfterSeconds());
            }
        }
    }

    public void recordSuccess(UUID userId) {
        usageQuotaService.recordChatSuccess(userId);
    }
}
