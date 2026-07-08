package com.histar.be.chat.service.impl;

import com.histar.be.billing.service.UsageQuotaService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    private final UsageQuotaService usageQuotaService;

    public void checkAndIncrement(UUID userId) {
        usageQuotaService.assertCanSendChat(userId);
    }

    public void recordSuccess(UUID userId) {
        usageQuotaService.recordChatSuccess(userId);
    }
}
