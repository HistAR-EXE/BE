package com.histar.be.billing.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.histar.be.billing.entity.*;
import com.histar.be.billing.repository.*;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.service.OrganizationEntitlementService;
import com.histar.be.common.exception.QuotaExceededException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
class AiQuotaReservationServiceTest {
 AiQuotaReservationRepository reservations=mock(AiQuotaReservationRepository.class);
 UsageQuotaRepository usages=mock(UsageQuotaRepository.class);
 ProfileRepository profiles=mock(ProfileRepository.class);
 OrganizationRepository orgs=mock(OrganizationRepository.class);
 OrganizationEntitlementService memberships=mock(OrganizationEntitlementService.class);
 UsageQuotaService quotas=mock(UsageQuotaService.class);
 UUID user=UUID.randomUUID();
 Profile profile=Profile.builder().id(user).build();
 AiQuotaReservationService service;
 @BeforeEach void setup(){
  service=new AiQuotaReservationService(reservations,usages,profiles,orgs,memberships,quotas);
  when(profiles.findByIdForUpdate(user)).thenReturn(Optional.of(profile));
  when(quotas.getUserDailyLimit(profile)).thenReturn(10);
 }
 @Test void nearLimitIncludesPendingReservationsBeforeCallingProvider(){
  when(quotas.getUserDailyUsage(user)).thenReturn(9);
  when(reservations.countActiveUser(eq(user),any(),any())).thenReturn(1L);
  assertThatThrownBy(()->service.reserve(user)).isInstanceOf(QuotaExceededException.class);
  verify(reservations,never()).save(any());
 }
 @Test void releasedAndExpiredReservationsAreNotCharged(){
  UUID token=UUID.randomUUID(); service.release(user,token);
  verify(reservations).release(eq(token),eq(user)); verifyNoInteractions(usages);
 }
 @Test void settlementIsIdempotentAndUsesCapturedDay(){
  UUID token=UUID.randomUUID(); var day=LocalDate.of(2026,10,27);
  var pending=AiQuotaReservation.builder().id(token).userId(user).quotaDate(day).status("ACTIVE").expiresAt(Instant.now().plusSeconds(60)).build();
  var scope=mock(AiQuotaReservationRepository.Scope.class);when(scope.getUserId()).thenReturn(user);
  when(reservations.findScope(token)).thenReturn(Optional.of(scope));
  when(reservations.findByIdForUpdate(token)).thenReturn(Optional.of(pending));
  var usage=UsageQuota.builder().userId(user).year(2026).month(10).day(27).usedAiQueries(9).build();
  when(usages.findByUserIdAndYearAndMonthAndDay(user,2026,10,27)).thenReturn(Optional.of(usage));
  service.settle(user,token); service.settle(user,token);
  assertThat(usage.getUsedAiQueries()).isEqualTo(10); verify(usages,times(1)).save(usage);
 }
 @Test void expiredTokenCannotBeChargedOrPersistedAsSuccess(){
  UUID token=UUID.randomUUID();var pending=AiQuotaReservation.builder().id(token).userId(user).quotaDate(LocalDate.now()).status("ACTIVE").expiresAt(Instant.now().minusSeconds(1)).build();
  var scope=mock(AiQuotaReservationRepository.Scope.class);when(scope.getUserId()).thenReturn(user);
  when(reservations.findScope(token)).thenReturn(Optional.of(scope));when(reservations.findByIdForUpdate(token)).thenReturn(Optional.of(pending));
  assertThatThrownBy(()->service.settle(user,token)).isInstanceOf(QuotaExceededException.class);verifyNoInteractions(usages);
 }
 @Test void personalUnlimitedDoesNotReserveOrCharge(){
  when(quotas.getUserDailyLimit(profile)).thenReturn(-1);assertThat(service.reserve(user)).isNull();verify(reservations,never()).save(any());
 }
}
