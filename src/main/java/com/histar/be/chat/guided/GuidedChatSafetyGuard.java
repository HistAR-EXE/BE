package com.histar.be.chat.guided;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Rejects common attempts to override the heritage-guide instruction boundary before provider calls. */
@Component
public class GuidedChatSafetyGuard {

    private static final Set<String> BLOCKED_PATTERNS = Set.of(
            "ignore previous instructions",
            "ignore all instructions",
            "system prompt",
            "developer message",
            "jailbreak",
            "bỏ qua hướng dẫn",
            "tiết lộ prompt");

    public GuidedSafetyDecision evaluate(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        boolean blocked = BLOCKED_PATTERNS.stream().anyMatch(normalized::contains);
        if (blocked) {
            return GuidedSafetyDecision.block(
                    "Mình chỉ hỗ trợ hành trình khám phá di sản và không thể làm theo yêu cầu thay đổi hướng dẫn hệ thống. "
                            + "Bạn có thể hỏi về địa điểm, nhân vật hoặc tư liệu đang tham quan.");
        }
        return GuidedSafetyDecision.allow();
    }
}
