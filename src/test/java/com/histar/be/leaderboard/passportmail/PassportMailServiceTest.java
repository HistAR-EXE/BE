package com.histar.be.leaderboard.passportmail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.*;

class PassportMailServiceTest {
    @Test void disabledStatusDoesNotClaimDeliveryAvailable() {
        var profiles=mock(ProfileRepository.class);
        var id=UUID.randomUUID();
        when(profiles.findById(id)).thenReturn(Optional.of(Profile.builder().id(id).email("verified@example.com").emailVerified(true).build()));
        var mail=new HistarMailProperties(); mail.setEnabled(false);
        var service=new PassportMailService(profiles,mock(PassportMailRepository.class),mock(HistarEmailService.class),mail,new TestHookProperties(),new HistarPassportMailProperties(),mock(org.springframework.transaction.PlatformTransactionManager.class));
        assertFalse(service.status(id).available());
    }
    @Test void missingProfileIsUnauthorized() {
        var service=new PassportMailService(mock(ProfileRepository.class),mock(PassportMailRepository.class),mock(HistarEmailService.class),new HistarMailProperties(),new TestHookProperties(),new HistarPassportMailProperties(),mock(org.springframework.transaction.PlatformTransactionManager.class));
        assertEquals(401,assertThrows(PassportMailException.class,()->service.status(UUID.randomUUID())).getStatus());
    }
    @Test void invalidFromIsUnavailableBeforeReservation() {
        var settings=new HistarPassportMailProperties(); settings.setFrom("bad\r\nBcc: victim@example.com");
        var attempts=mock(PassportMailRepository.class);
        var sender=mock(HistarEmailService.class);
        var service=new PassportMailService(mock(ProfileRepository.class),attempts,sender,new HistarMailProperties(),new TestHookProperties(),settings,mock(org.springframework.transaction.PlatformTransactionManager.class));
        assertEquals(503,assertThrows(PassportMailException.class,()->service.send(UUID.randomUUID(),null,UUID.randomUUID())).getStatus());
        verifyNoInteractions(attempts,sender);
    }}

