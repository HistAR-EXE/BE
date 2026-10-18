package com.histar.be.leaderboard.passportmail;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
class PassportMailReservationTest {
 @Test void reservesVerifiedDatabaseRecipientAndNeverSavesProfile() throws Exception {
  var profiles=mock(ProfileRepository.class); var repo=mock(PassportMailRepository.class); var sender=mock(HistarEmailService.class);
  var tx=mock(PlatformTransactionManager.class);when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
  var id=UUID.randomUUID(); var request=UUID.randomUUID();
  var profile=Profile.builder().id(id).email("db@example.com").emailVerified(true).totalPoints(123).build();
  when(profiles.findById(id)).thenReturn(Optional.of(profile));when(profiles.findByIdForUpdate(id)).thenReturn(Optional.of(profile));
  when(repo.findByUserIdAndRequestId(id,request)).thenReturn(Optional.empty());
  when(repo.findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(any(),any())).thenReturn(List.of());
  when(repo.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
  when(repo.findById(any())).thenAnswer(i->Optional.of(PassportMailAttempt.created(i.getArgument(0),id,request,"hash",Instant.now())));
  var service=new PassportMailService(profiles,repo,sender,new HistarMailProperties(),new TestHookProperties(),new HistarPassportMailProperties(),tx);
  var b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1200,1500,BufferedImage.TYPE_INT_ARGB),"png",b);
  assertEquals("ACCEPTED", service.send(id,new MockMultipartFile("image","x.png","image/png",b.toByteArray()),request).status());
  verify(sender).sendAttachment(eq("TimeLens <histar.timelens@gmail.com>"),eq("db@example.com"),anyString(),anyString(),eq("TimeLens-passport.png"),any());
  verify(profiles,never()).save(any());assertEquals(123,profile.getTotalPoints());
 }
}
