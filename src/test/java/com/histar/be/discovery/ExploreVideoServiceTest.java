package com.histar.be.discovery;

import com.histar.be.discovery.explore.video.*;
import com.histar.be.discovery.explore.*;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.discovery.service.DiscoveryRewardService;
import com.histar.be.discovery.dto.RecordDiscoveryResponse;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.organization.service.TrialEntitlementGuard;
import com.histar.be.common.exception.BusinessRuleException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExploreVideoServiceTest {
 final UUID user=UUID.randomUUID();
 final ExploreVideoCatalog catalog=new ExploreVideoCatalog();
 final ExploreVideoProgressRepository progress=mock(ExploreVideoProgressRepository.class);
 final ProfilePointsService points=mock(ProfilePointsService.class);
 final UserDiscoveryRepository discoveries=mock(UserDiscoveryRepository.class);
 final DiscoveryRewardService rewards=mock(DiscoveryRewardService.class);
 final ExploreService explore=mock(ExploreService.class);
 final ExploreAttemptRepository attempts=mock(ExploreAttemptRepository.class);
 final ExploreVideoService service=new ExploreVideoService(catalog,progress,points,discoveries,rewards,explore,attempts,mock(TrialEntitlementGuard.class));
 final String id="tunnel-digging";
 ExploreVideoProgress row(double watched){
  var row=ExploreVideoProgress.builder().userId(user).videoId(id).watchedSeconds(watched).sessionId(UUID.randomUUID())
   .heartbeatAt(Instant.now().minusSeconds(5)).sessionStartedAt(Instant.now().minusSeconds(5)).sessionStartedSeconds(watched).updatedAt(Instant.now()).build();
  when(progress.findByUserIdAndVideoId(user,id)).thenReturn(Optional.of(row)); return row;
 }
 @Test void quizBeforeVerifiedWatchDoesNotGradeOrAward(){
  row(20);
  assertThatThrownBy(()->service.complete(user,id,new ExploreVideoService.Answer(1))).isInstanceOf(BusinessRuleException.class).hasMessageContaining("90%");
  verifyNoInteractions(rewards);verifyNoInteractions(attempts);
 }
 @Test void correctWatchedQuizUsesExistingRewardPipeline(){
  row(75);
  when(rewards.record(user,ExploreChallengeCatalog.LOCATION,"video:"+id,"explore-video"))
   .thenReturn(new RecordDiscoveryResponse(true,30,List.of(),null,List.of()));
  var response=service.complete(user,id,new ExploreVideoService.Answer(catalog.get(id).correctOption()));
  assertThat(response.passed()).isTrue(); assertThat(response.xpEarned()).isEqualTo(30);
  verify(attempts).save(any(ExploreAttempt.class));
 }
 @Test void wrongWatchedQuizRecordsAttemptButDoesNotAward(){
  row(75);
  var result=service.complete(user,id,new ExploreVideoService.Answer((catalog.get(id).correctOption()+1)%3));
  assertThat(result.passed()).isFalse();assertThat(result.xpEarned()).isZero(); verifyNoInteractions(rewards);
 }
 @Test void repeatedCompletionCannotAwardTwice(){
  when(discoveries.existsByUserIdAndLocationIdAndDiscoveryKey(user,ExploreChallengeCatalog.LOCATION,"video:"+id)).thenReturn(true);
  var result=service.complete(user,id,new ExploreVideoService.Answer(1));
  assertThat(result.alreadyCompleted()).isTrue();assertThat(result.xpEarned()).isZero();verifyNoInteractions(rewards);
 }
 @Test void staleOrForeignSessionCannotAdvanceProgress(){
  row(5);
  assertThatThrownBy(()->service.heartbeat(user,id,new ExploreVideoService.Heartbeat(UUID.randomUUID(),10.0))).hasMessageContaining("Phiên xem");
  verify(progress,never()).save(any());
 }
 @Test void repeatedToleranceCannotBypassTotalSessionClock(){
  var row=row(20);row.setSessionStartedSeconds(0);row.setSessionStartedAt(Instant.now());
  assertThatThrownBy(()->service.heartbeat(user,id,new ExploreVideoService.Heartbeat(row.getSessionId(),21.0))).hasMessageContaining("thời gian xem");
  verify(progress,never()).save(any());
 }
 @Test void restartingSessionsCannotMintTheFixedHeartbeatTolerance(){
  var row=row(10);row.setSessionStartedSeconds(10);row.setSessionStartedAt(Instant.now());
  assertThatThrownBy(()->service.heartbeat(user,id,new ExploreVideoService.Heartbeat(row.getSessionId(),10.9))).hasMessageContaining("thời gian xem");
 }
 @Test void sessionRestartInvalidatesOldTokenAndResumesOnlyVerifiedFrontier(){
  var row=row(15);UUID old=row.getSessionId();
  var state=service.start(user,id);
  assertThat(state.sessionId()).isNotEqualTo(old);assertThat(state.resumeSeconds()).isEqualTo(15);
 }
 @Test void publicLibraryNeverSerializesPrivateAnswer() throws Exception {
   var json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(service.library(user));
   assertThat(json).doesNotContain("correctOption");assertThat(service.library(user).videos()).hasSize(7);
   assertThatThrownBy(()->catalog.get("cedar-falls")).isInstanceOf(BusinessRuleException.class);
 }
 @Test void genericAttemptEndpointDoesNotAcceptVideoKeys(){
  assertThat(ExploreChallengeCatalog.QUIZZES).doesNotContainKey("video:"+id);
  assertThat(ExploreChallengeCatalog.CLUES).doesNotContain("video:"+id);
 }
}
