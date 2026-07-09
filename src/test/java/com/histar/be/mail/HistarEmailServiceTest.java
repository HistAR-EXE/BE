package com.histar.be.mail;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistarEmailServiceTest {

    @Mock
    private HistarMailProperties mailProperties;

    @Mock
    private TestHookProperties testHookProperties;

    @Mock
    private EmailSender emailSender;

    private HistarEmailService histarEmailService;

    @BeforeEach
    void setUp() {
        histarEmailService = new HistarEmailService(mailProperties, testHookProperties, emailSender);
    }

    @Test
    void sendHtml_mailDisabled_skipsProvider() {
        when(mailProperties.isEnabled()).thenReturn(false);

        histarEmailService.sendHtml("user@histar.vn", "subject", "<p>hi</p>");

        verify(emailSender, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void sendHtml_testHooksEnabled_skipsProvider() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(testHookProperties.isEnabled()).thenReturn(true);

        histarEmailService.sendHtml("user@histar.vn", "subject", "<p>hi</p>");

        verify(emailSender, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void sendHtml_mailEnabled_delegatesToProvider() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(testHookProperties.isEnabled()).thenReturn(false);
        when(mailProperties.getProvider()).thenReturn("resend");

        histarEmailService.sendHtml("user@histar.vn", "Test subject", "<p>hi</p>");

        verify(emailSender).sendHtml(eq("user@histar.vn"), eq("Test subject"), eq("<p>hi</p>"));
    }
}
