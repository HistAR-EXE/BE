package com.histar.be.billing.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.histar.be.billing.entity.*;
import com.histar.be.billing.repository.*;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.service.OrganizationEntitlementService;
import com.histar.be.common.exception.QuotaExceededException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
@DataJpaTest(properties={"spring.datasource.url=jdbc:h2:mem:ai_quota_lease;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(AiQuotaReservationService.class)
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class AiQuotaReservationIntegrationTest {
 @Autowired AiQuotaReservationService service;
 @Autowired AiQuotaReservationRepository reservations;
 @Autowired UsageQuotaRepository usages;
 @Autowired ProfileRepository profiles;
 @Autowired OrganizationRepository organizations;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @MockitoBean UsageQuotaService quotas;
 @MockitoBean OrganizationEntitlementService memberships;
 LocalDate day=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
 Profile account(){return profiles.saveAndFlush(Profile.builder().email(UUID.randomUUID()+"@quota.test").role("USER").tier("FREE").build());}
 @BeforeEach void policy(){
  when(quotas.getUserDailyLimit(any())).thenReturn(10);
  when(quotas.getUserDailyUsage(any())).thenAnswer(i->usages.findByUserIdAndYearAndMonthAndDay(i.getArgument(0),day.getYear(),day.getMonthValue(),day.getDayOfMonth()).map(UsageQuota::getUsedAiQueries).orElse(0));
 }
 void seedUser(Profile user,int used){usages.saveAndFlush(UsageQuota.builder().userId(user.getId()).year(day.getYear()).month(day.getMonthValue()).day(day.getDayOfMonth()).usedAiQueries(used).build());}
 @Test void simultaneousFreeRequestsAtNineOfTenAdmitExactlyOne() throws Exception {
  var user=account();seedUser(user,9);
  List<UUID> tokens=race(Collections.nCopies(8,user.getId()));
  assertThat(tokens).hasSize(1);service.settle(user.getId(),tokens.get(0));service.settle(user.getId(),tokens.get(0));
  assertThat(quotas.getUserDailyUsage(user.getId())).isEqualTo(10);
 }
 @Test void concurrentSettlementOfOneTokenOnlyChargesOnce() throws Exception {
  var user=account();seedUser(user,9);UUID token=service.reserve(user.getId());
  var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try {var first=pool.submit(()->{gate.await();service.settle(user.getId(),token);return true;});var second=pool.submit(()->{gate.await();service.settle(user.getId(),token);return true;});gate.countDown();first.get(20,TimeUnit.SECONDS);second.get(20,TimeUnit.SECONDS);}
  finally{pool.shutdownNow();}
  assertThat(quotas.getUserDailyUsage(user.getId())).isEqualTo(10);
 }
 @Test void failuresReleaseWithoutChargeAndExpiredLeasesDoNotConsumeCapacity(){
  var user=account();seedUser(user,9);UUID first=service.reserve(user.getId());service.release(user.getId(),first);
  assertThat(quotas.getUserDailyUsage(user.getId())).isEqualTo(9);
  UUID second=service.reserve(user.getId());var row=reservations.findById(second).orElseThrow();row.setExpiresAt(Instant.now().minusSeconds(1));reservations.saveAndFlush(row);
  UUID third=service.reserve(user.getId());assertThat(third).isNotNull();service.expireStale();
  assertThat(reservations.findById(second).orElseThrow().getStatus()).isEqualTo("EXPIRED");
  assertThatThrownBy(()->service.settle(user.getId(),second)).isInstanceOf(QuotaExceededException.class);
  service.release(user.getId(),third);
 }
 @Test void responseTransactionRollbackAlsoRollsBackSuccessCharge(){
  var user=account();seedUser(user,9);UUID token=service.reserve(user.getId());
  assertThatThrownBy(()->new TransactionTemplate(transactions).execute(status->{service.settle(user.getId(),token);throw new IllegalStateException("message persist failed");})).isInstanceOf(IllegalStateException.class);
  service.release(user.getId(),token);
  assertThat(quotas.getUserDailyUsage(user.getId())).isEqualTo(9);
  assertThat(reservations.findById(token).orElseThrow().getStatus()).isEqualTo("RELEASED");
 }
 @Test void simultaneousOrganizationRequestsShareOneRemainingMonthlySlot() throws Exception {
  var org=organizations.saveAndFlush(Organization.builder().name("Shared").planType("MICRO").status("ACTIVE").build());
  var first=account();first.setOrgId(org.getId());profiles.saveAndFlush(first);var second=account();second.setOrgId(org.getId());profiles.saveAndFlush(second);
  when(memberships.hasActiveMembership(any())).thenReturn(true);when(quotas.getOrgMonthlyUsage(org.getId())).thenReturn(9);
  List<UUID> tokens=race(List.of(first.getId(),second.getId(),first.getId(),second.getId()));assertThat(tokens).hasSize(1);
 }
 @Test void unlimitedOrganizationStillCountsSuccessfulQueries(){
  var org=organizations.saveAndFlush(Organization.builder().name("Unlimited").planType("PREMIUM").status("ACTIVE").build());
  var user=account();user.setOrgId(org.getId());profiles.saveAndFlush(user);
  when(memberships.hasActiveMembership(any())).thenReturn(true);when(quotas.getUserDailyLimit(any())).thenReturn(-1);
  UUID token=service.reserve(user.getId());assertThat(token).isNotNull();service.settle(user.getId(),token);
  assertThat(usages.findByOrganizationIdAndYearAndMonth(org.getId(),day.getYear(),day.getMonthValue()).orElseThrow().getUsedAiQueries()).isEqualTo(1);
 }
 List<UUID> race(List<UUID> users) throws Exception {
  var pool=Executors.newFixedThreadPool(users.size());var gate=new CountDownLatch(1);List<Future<UUID>> futures=new ArrayList<>();
  try{for(UUID user:users)futures.add(pool.submit(()->{gate.await();try{return service.reserve(user);}catch(QuotaExceededException denied){return null;}}));gate.countDown();List<UUID> result=new ArrayList<>();for(var f:futures){var token=f.get(20,TimeUnit.SECONDS);if(token!=null)result.add(token);}return result;}
  finally{pool.shutdownNow();}
 }
}
