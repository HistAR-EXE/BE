package com.histar.be.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.repository.B2cRenewalReminderRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class B2cRenewalReminderServiceTest {

    @Mock
    private B2cSubscriptionRepository subscriptionRepository;

    @Mock
    private B2cRenewalReminderRepository reminderRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private JavaMailSender mailSender;

    private HistarMailProperties mailProperties;
    private B2cRenewalReminderService service;

    @BeforeEach
    void setUp() {
        mailProperties = new HistarMailProperties();
        mailProperties.setEnabled(false);
        service = new B2cRenewalReminderService(
                subscriptionRepository, reminderRepository, profileRepository, mailSender, mailProperties);
    }

    @Test
    void shouldSendReminderAtSevenDaysAndPersistMarker() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        B2cSubscription sub = B2cSubscription.builder()
                .id(subId)
                .userId(userId)
                .isActive(true)
                .endDate(today.plusDays(7))
                .build();
        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        when(reminderRepository.existsBySubscriptionIdAndReminderDay(subId, 7)).thenReturn(false);
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).email("user@histar.vn").build()));

        int sent = service.sendDailyRenewalReminders();

        assertEquals(1, sent);
        verify(reminderRepository).save(any());
    }

    @Test
    void shouldNotDuplicateWhenReminderAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        B2cSubscription sub = B2cSubscription.builder()
                .id(subId)
                .userId(userId)
                .isActive(true)
                .endDate(today.plusDays(3))
                .build();
        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        when(reminderRepository.existsBySubscriptionIdAndReminderDay(subId, 3)).thenReturn(true);

        int sent = service.sendDailyRenewalReminders();

        assertEquals(0, sent);
        verify(reminderRepository, never()).save(any());
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
