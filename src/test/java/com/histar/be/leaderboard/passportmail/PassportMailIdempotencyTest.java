package com.histar.be.leaderboard.passportmail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.*;
import java.time.*;
import java.security.MessageDigest;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.mock.web.MockMultipartFile;
class PassportMailIdempotencyTest {
 ProfileRepository profiles;PassportMailRepository repo;HistarEmailService sender;PassportMailService service;
 UUID user=UUID.randomUUID(),key=UUID.randomUUID();Instant now=Instant.parse("2026-10-18T00:00:00Z");MockMultipartFile image;String hash;
 @BeforeEach void setup() throws Exception {
  profiles=mock(ProfileRepository.class);repo=mock(PassportMailRepository.class);sender=mock(HistarEmailService.class);
  var tx=mock(PlatformTransactionManager.class);when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
  var profile=Profile.builder().id(user).email("db@example.com").emailVerified(true).build();
  when(profiles.findById(user)).thenReturn(Optional.of(profile));when(profiles.findByIdForUpdate(user)).thenReturn(Optional.of(profile));
  when(repo.findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(any(),any())).thenReturn(List.of());
  service=new PassportMailService(profiles,repo,sender,new HistarMailProperties(),new TestHookProperties(),new HistarPassportMailProperties(),tx);service.setClock(Clock.fixed(now,ZoneOffset.UTC));
  var b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1200,1500,BufferedImage.TYPE_INT_ARGB),"png",b);
  image=new MockMultipartFile("image","x.png","image/png",b.toByteArray());
  hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new PassportPngNormalizer().normalize(image)));
 }
 @Test void acceptedReplayNeverResendsAndReportsCurrentQuota() {
  var old=PassportMailAttempt.created(UUID.randomUUID(),user,key,hash,now.minusSeconds(120));old.finish("ACCEPTED");
  when(repo.findByUserIdAndRequestId(user,key)).thenReturn(Optional.of(old));
  when(repo.findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(any(),any())).thenReturn(List.of(old));
  var response=service.send(user,image,key);assertEquals("ACCEPTED",response.status());assertEquals(0,response.cooldownSeconds());assertEquals(2,response.remainingToday());verifyNoInteractions(sender);
 }
 @Test void unknownReplayNeverResends() {
  var old=PassportMailAttempt.created(UUID.randomUUID(),user,key,hash,now);old.finish("UNKNOWN");
  when(repo.findByUserIdAndRequestId(user,key)).thenReturn(Optional.of(old));
  assertEquals("MAIL_DELIVERY_UNKNOWN",assertThrows(PassportMailException.class,()->service.send(user,image,key)).getCode());verifyNoInteractions(sender);
 }
 @Test void differentImageCannotReuseRequestId() {
  var old=PassportMailAttempt.created(UUID.randomUUID(),user,key,"0".repeat(64),now);old.finish("ACCEPTED");
  when(repo.findByUserIdAndRequestId(user,key)).thenReturn(Optional.of(old));
  assertEquals("REQUEST_ID_REUSED",assertThrows(PassportMailException.class,()->service.send(user,image,key)).getCode());verifyNoInteractions(sender);
 }
 @Test void allAttemptStatesCountTowardCooldownAndDailyLimit() {
  var first=PassportMailAttempt.created(UUID.randomUUID(),user,UUID.randomUUID(),hash,now.minusSeconds(180));first.finish("UNKNOWN");
  var second=PassportMailAttempt.created(UUID.randomUUID(),user,UUID.randomUUID(),hash,now.minusSeconds(120));second.finish("ACCEPTED");
  var third=PassportMailAttempt.created(UUID.randomUUID(),user,UUID.randomUUID(),hash,now.minusSeconds(10));
  when(repo.findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(any(),any())).thenReturn(List.of(first,second,third));
  var failure=assertThrows(PassportMailException.class,()->service.send(user,image,key));assertEquals(429,failure.getStatus());assertEquals(86220L,failure.getRetryAfterSeconds());verifyNoInteractions(sender);verify(repo,never()).saveAndFlush(any());
 }
 @Test void unverifiedRecipientRejectedBeforeReserve() {
  when(profiles.findById(user)).thenReturn(Optional.of(Profile.builder().id(user).email("db@example.com").emailVerified(false).build()));
  assertEquals(403,assertThrows(PassportMailException.class,()->service.send(user,image,key)).getStatus());verifyNoInteractions(sender,repo);
 }
 @Test void transportExceptionIsSanitizedAndStoredUnknown() {
  var attempt=PassportMailAttempt.created(UUID.randomUUID(),user,key,hash,now);
  when(repo.saveAndFlush(any())).thenReturn(attempt);when(repo.findById(any())).thenReturn(Optional.of(attempt));
  doThrow(new RuntimeException("SECRET PROVIDER BODY")).when(sender).sendAttachment(anyString(),anyString(),anyString(),anyString(),anyString(),any());
  var failure=assertThrows(PassportMailException.class,()->service.send(user,image,key));assertEquals(502,failure.getStatus());assertFalse(failure.getMessage().contains("SECRET"));assertEquals("UNKNOWN",attempt.getState());
 }
}
