package com.histar.be.chat.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatRateLimiter {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final int dailyLimit;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final HistarOrgProperties histarOrgProperties;

    public ChatRateLimiter(
            @Value("${gemini.daily-message-limit}") int dailyLimit,
            MessageRepository messageRepository,
            ProfileRepository profileRepository,
            HistarOrgProperties histarOrgProperties) {
        this.dailyLimit = dailyLimit;
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.histarOrgProperties = histarOrgProperties;
    }

    public void checkAndIncrement(UUID userId) {
        var profile = profileRepository.findById(userId).orElse(null);
        if (profile != null && hasUnlimitedChat(profile)) {
            return;
        }
        int limit = histarOrgProperties.getTier().getFreeChatDailyLimit() > 0
                ? histarOrgProperties.getTier().getFreeChatDailyLimit()
                : dailyLimit;

        LocalDate vnDay = LocalDate.now(VN_ZONE);
        ZonedDateTime startVn = vnDay.atStartOfDay(VN_ZONE);
        ZonedDateTime endVn = startVn.plusDays(1);

        long currentCount = messageRepository.countUserMessagesByUserIdAndCreatedAtBetween(
                userId, startVn.toInstant(), endVn.toInstant());
        if (currentCount >= limit) {
            throw new BusinessRuleException(
                    "Đã đạt giới hạn " + limit + " tin nhắn/ngày. Nâng cấp Premium để chat không giới hạn.");
        }
    }

    private static boolean hasUnlimitedChat(Profile profile) {
        UserRole role = UserRole.fromStored(profile.getRole());
        if (role == UserRole.ADMIN || role == UserRole.TEACHER) {
            return true;
        }
        return UserTier.PREMIUM == UserTier.fromStored(profile.getTier());
    }
}
