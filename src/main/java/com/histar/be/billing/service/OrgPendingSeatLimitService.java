package com.histar.be.billing.service;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.billing.policy.OrgPlanLimits;
import com.histar.be.config.SepayProperties;
import com.histar.be.organization.entity.OrgSubscription;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
/** Read while holding the organization lock during enrollment or pending-intent creation. */
@Service @RequiredArgsConstructor
public class OrgPendingSeatLimitService {
 private final OrgPaymentTransactionRepository payments;
 private final SepayProperties properties;
 public Integer pendingLimit(UUID orgId){
  Instant cutoff=Instant.now().minus(Math.max(0,properties.getExpiryGraceMinutes()),ChronoUnit.MINUTES);
  return payments.findReservedPlans(orgId,cutoff).stream()
   .map(p->OrgPlanLimits.forPlan(OrgSubscription.fromPlanType(p.getPlanType())).maxVerifiedAccounts())
   .min(Integer::compareTo).orElse(null);
 }
}
