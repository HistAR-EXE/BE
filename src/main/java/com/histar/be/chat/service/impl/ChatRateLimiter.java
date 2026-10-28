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
    private final com.histar.be.billing.service.AiQuotaReservationService reservations;
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

    public UUID reserve(UUID userId) {
        // Burst admission happens before acquiring a durable quota lease.
        if (userId != null) {
            var decision = chatBurstRateLimiter.tryAcquire(userId);
            if (!decision.allowed()) throw new RateLimitedException("Bạn đang gửi tin nhắn quá nhanh.", decision.retryAfterSeconds());
        }
        return reservations.reserve(userId);
    }

    public void settle(UUID userId, UUID token) { reservations.settle(userId, token); }

    public void release(UUID userId, UUID token) {
        boolean interrupted = Thread.interrupted();
        try { reservations.release(userId, token); }
        catch (RuntimeException failure) {
            // A failed refund cannot charge the user. The lease also has a bounded expiry.
            org.slf4j.LoggerFactory.getLogger(ChatRateLimiter.class).warn("Could not release AI quota lease {}", token, failure);
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }

    public void recordSuccess(UUID userId) {
        usageQuotaService.recordChatSuccess(userId);
    }
}
