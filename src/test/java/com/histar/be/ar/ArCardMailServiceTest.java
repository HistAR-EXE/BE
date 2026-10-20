package com.histar.be.ar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.mail.EmailSender;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

class ArCardMailServiceTest {
    final UUID user = UUID.randomUUID();
    final ProfileRepository profiles = mock(ProfileRepository.class);
    final ArCardMailRepository attempts = mock(ArCardMailRepository.class);
    final EmailSender sender = mock(EmailSender.class);
    final HistarMailProperties mail = new HistarMailProperties();
    final TestHookProperties hooks = new TestHookProperties();
    final HistarAppProperties app = new HistarAppProperties();
    ArCardMailService service() {
        app.setFrontendUrl("https://www.timelens.asia");
        when(profiles.findById(user)).thenReturn(Optional.of(Profile.builder().id(user).email("owner@example.invalid").emailVerified(true).build()));
        var profile=profiles.findById(user);
        when(profiles.findByIdForUpdate(user)).thenReturn(profile);
        when(attempts.save(any())).thenAnswer(call -> call.getArgument(0));
        return new ArCardMailService(profiles, attempts, sender, mail, hooks, app, mock(PlatformTransactionManager.class));
    }
    @Test void selectedCardContainsOnlyItsOwnImageAndLinks() {
        var service=service();
        service.send(user,"hat",UUID.randomUUID());
        var html=ArgumentCaptor.forClass(String.class);
        verify(sender).sendHtmlBounded(eq("owner@example.invalid"),eq("TimeLens · Ảnh AR Mũ cối"),html.capture());
        assertEquals(1,html.getValue().split("view=target",-1).length-1);
        assertEquals(1,html.getValue().split("view=camera",-1).length-1);
        assertTrue(html.getValue().contains("DSCF9675"));
        assertFalse(html.getValue().contains("DSCF9770"));
        assertEquals(1,html.getValue().split("<img ",-1).length-1);
        assertFalse(html.getValue().contains("localhost"));
        assertTrue(html.getValue().contains("in màu"));
    }
    @Test void disabledMailAndUnverifiedProfilesNeverSend() {
        var service=service(); mail.setEnabled(false);
        assertEquals(503,assertThrows(ArCardMailException.class,()->service.send(user,"hat",UUID.randomUUID())).status());
        verifyNoInteractions(sender);
        mail.setEnabled(true);
        when(profiles.findByIdForUpdate(user)).thenReturn(Optional.of(Profile.builder().id(user).email("owner@example.invalid").emailVerified(false).build()));
        assertEquals(403,assertThrows(ArCardMailException.class,()->service.send(user,"hat",UUID.randomUUID())).status());
        verifyNoInteractions(sender);
    }
    @Test void acceptedRequestIsNeverDeliveredTwice() {
        var service=service(); var key=UUID.randomUUID();
        var old=ArCardMailAttempt.created(user,key,"hat",java.time.Instant.now());old.finish("ACCEPTED");
        when(attempts.findByUserIdAndRequestId(user,key)).thenReturn(Optional.of(old));
        assertEquals("ACCEPTED",service.send(user,"hat",key).status());
        verifyNoInteractions(sender);
    }
    @Test void quotaAndUnknownAttemptPreventExtraMail() {
        var service=service(); var key=UUID.randomUUID();
        var old=ArCardMailAttempt.created(user,key,"hat",java.time.Instant.now());old.finish("UNKNOWN");
        when(attempts.findByUserIdAndRequestId(user,key)).thenReturn(Optional.of(old));
        assertEquals("MAIL_DELIVERY_UNKNOWN",assertThrows(ArCardMailException.class,()->service.send(user,"hat",key)).code());
        when(attempts.findByUserIdAndRequestId(user,key)).thenReturn(Optional.empty());
        when(attempts.findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(eq(user),any())).thenReturn(List.of(old));
        assertEquals(429,assertThrows(ArCardMailException.class,()->service.send(user,"hat",key)).status());
        verifyNoInteractions(sender);
    }
    @Test void localUrlIsUnavailableAndTransportFailureIsNotSuccess() {
        var service=service();app.setFrontendUrl("http://localhost:5175");
        assertFalse(service.status(user).available());
        assertEquals(503,assertThrows(ArCardMailException.class,()->service.send(user,"hat",UUID.randomUUID())).status());
        app.setFrontendUrl("https://www.timelens.asia");
        doThrow(new com.histar.be.mail.EmailDeliveryException("timeout")).when(sender).sendHtmlBounded(any(),any(),any());
        assertEquals(502,assertThrows(ArCardMailException.class,()->service.send(user,"hat",UUID.randomUUID())).status());
    }

    @Test void everySelectionHasExactlyOneMatchingImageAndNeverOtherCards() {
        var service=service();
        String[][] photos={{"hat","DSCF9675"},{"lamp","DSCF9765"},{"canteen","DSCF9786"},{"compass","DSCF9770"},{"binoculars","DSCF9691"},{"shovel","DSCF9805"},{"pot","DSCF9792"},{"rifle","DSCF9756"},{"flashlight","DSCF9806"},{"firstaid","DSCF9803"}};
        for(var photo:photos)assertEquals(photo[0],service.send(user,photo[0],UUID.randomUUID()).artifactId());
        var bodies=ArgumentCaptor.forClass(String.class);verify(sender,times(10)).sendHtmlBounded(eq("owner@example.invalid"),anyString(),bodies.capture());
        for(int i=0;i<photos.length;i++) {
            var html=bodies.getAllValues().get(i);assertEquals(1,html.split("<img ",-1).length-1);
            assertEquals(1,html.split("view=target",-1).length-1);assertEquals(1,html.split("view=camera",-1).length-1);
            assertTrue(html.contains(photos[i][1]+".jpg"));assertTrue(html.contains("artifact="+photos[i][0]+"&amp;view=camera"));
            for(int j=0;j<photos.length;j++)if(i!=j)assertFalse(html.contains(photos[j][1]),photos[j][1]);
        }
    }
    @Test void changedArtifactAndHistoricalBulkKeyCannotClaimAcceptanceOrResend() {
        var service=service();var key=UUID.randomUUID();
        for(String oldArtifact:Arrays.asList("hat",null)) {
            var old=ArCardMailAttempt.created(user,key,oldArtifact,java.time.Instant.now());old.finish("ACCEPTED");
            when(attempts.findByUserIdAndRequestId(user,key)).thenReturn(Optional.of(old));
            assertEquals("REQUEST_ID_REUSED",assertThrows(ArCardMailException.class,()->service.send(user,"lamp",key)).code());
        }
        verifyNoInteractions(sender);
    }
    @Test void unknownArtifactAndUnavailableReasonsAreExplicit() {
        var service=service();
        assertEquals(422,assertThrows(ArCardMailException.class,()->service.send(user,"unknown",UUID.randomUUID())).status());
        assertNull(service.status(user).unavailableReason());
        mail.setEnabled(false);assertEquals("MAIL_DISABLED",service.status(user).unavailableReason());
        mail.setEnabled(true);hooks.setEnabled(true);assertEquals("TEST_MODE",service.status(user).unavailableReason());
        hooks.setEnabled(false);app.setFrontendUrl("http://localhost:5175");assertEquals("PUBLIC_URL_REQUIRED",service.status(user).unavailableReason());
        org.springframework.test.util.ReflectionTestUtils.setField(service,"enabled",false);assertEquals("FEATURE_DISABLED",service.status(user).unavailableReason());
        verifyNoInteractions(sender);
    }
}
