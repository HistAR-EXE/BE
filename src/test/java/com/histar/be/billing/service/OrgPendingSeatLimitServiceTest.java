package com.histar.be.billing.service;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.config.SepayProperties;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrgPendingSeatLimitServiceTest {
 @Test void pendingLowerPlanReservesItsCapacityUntilGraceExpiry(){
  var repo=mock(OrgPaymentTransactionRepository.class);var config=new SepayProperties();var id=UUID.randomUUID();
  when(repo.findReservedPlans(eq(id),any())).thenReturn(List.of(OrgPaymentTransaction.builder().planType("MICRO").expiresAt(Instant.now().plusSeconds(60)).build(),OrgPaymentTransaction.builder().planType("STANDARD").expiresAt(Instant.now().plusSeconds(60)).build()));
  assertThat(new OrgPendingSeatLimitService(repo,config).pendingLimit(id)).isEqualTo(100);
 }
}
