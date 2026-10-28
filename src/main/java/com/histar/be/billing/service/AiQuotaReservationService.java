package com.histar.be.billing.service;
import com.histar.be.billing.entity.*;
import com.histar.be.billing.repository.*;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.service.OrganizationEntitlementService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.UUID;

/** Durable leases reserve capacity without charging failed AI requests or keeping locks over network calls. */
@Service @RequiredArgsConstructor
public class AiQuotaReservationService {
 private static final ZoneId VN=ZoneId.of("Asia/Ho_Chi_Minh");
 private static final Duration LEASE=Duration.ofMinutes(10);
 private final AiQuotaReservationRepository reservations;
 private final UsageQuotaRepository usages;
 private final ProfileRepository profiles;
 private final OrganizationRepository orgs;
 private final OrganizationEntitlementService memberships;
 private final UsageQuotaService quotas;

 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public UUID reserve(UUID userId){
  var profile=profiles.findByIdForUpdate(userId).orElseThrow(()->new AuthException("Unauthorized"));
  if("ADMIN".equalsIgnoreCase(profile.getRole()))return null;
  UUID org=profile.getOrgId();
  // Account then organization locks are held only for this local check+insert transaction.
  if(org!=null) orgs.findByIdForUpdate(org);
  quotas.assertCanSendChat(userId);
  boolean shared=org!=null && memberships.hasActiveMembership(profile);
  int limit=quotas.getUserDailyLimit(profile);
  if(!shared && limit<0) return null;
  Instant now=Instant.now();
  LocalDate day=LocalDate.now(VN); if(shared)day=day.withDayOfMonth(1);
  long active=shared?reservations.countActiveOrg(org,day,now):reservations.countActiveUser(userId,day,now);
  int used=shared?quotas.getOrgMonthlyUsage(org):quotas.getUserDailyUsage(userId);
  if(limit>=0 && (long)used+active>=limit) throw quota("Đã hết lượt AI khả dụng. Một số yêu cầu đang được xử lý; vui lòng thử lại sau hoặc nâng cấp gói.",shared);
  UUID token=UUID.randomUUID();
  reservations.save(AiQuotaReservation.builder().id(token).userId(userId).organizationId(shared?org:null)
    .quotaDate(day).status("ACTIVE").expiresAt(now.plus(LEASE)).build());
  return token;
 }

 /** Joins the response-persistence transaction: the answer and its charge commit or roll back together. */
 @Transactional
 public void settle(UUID userId,UUID token){
  if(token==null)return;
  var initial=reservations.findScope(token).orElseThrow(()->quota("Phiên AI không còn hợp lệ. Vui lòng gửi lại.",false));
  if(!initial.getUserId().equals(userId))throw new AuthException("Forbidden");
  profiles.findByIdForUpdate(userId).orElseThrow(()->new AuthException("Unauthorized"));
  if(initial.getOrganizationId()!=null)orgs.findByIdForUpdate(initial.getOrganizationId()).orElseThrow(()->quota("Tổ chức không tồn tại",true));
  var lease=reservations.findByIdForUpdate(token).orElseThrow();
  if("COMPLETED".equals(lease.getStatus()))return;
  if(!"ACTIVE".equals(lease.getStatus()) || !lease.getExpiresAt().isAfter(Instant.now()))throw quota("Phiên AI đã hết hạn hoặc bị hủy. Vui lòng gửi lại.",lease.getOrganizationId()!=null);
  var date=lease.getQuotaDate();var org=lease.getOrganizationId();
  UsageQuota usage=org==null
    ?usages.findByUserIdAndYearAndMonthAndDay(userId,date.getYear(),date.getMonthValue(),date.getDayOfMonth()).orElseGet(()->UsageQuota.builder().userId(userId).year(date.getYear()).month(date.getMonthValue()).day(date.getDayOfMonth()).usedAiQueries(0).build())
    :usages.findByOrganizationIdAndYearAndMonth(org,date.getYear(),date.getMonthValue()).orElseGet(()->UsageQuota.builder().organizationId(org).year(date.getYear()).month(date.getMonthValue()).usedAiQueries(0).build());
  usage.setUsedAiQueries(usage.getUsedAiQueries()+1);usages.save(usage);
  lease.setStatus("COMPLETED");reservations.save(lease);
 }

 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void release(UUID userId,UUID token){if(token!=null)reservations.release(token,userId);}

 @org.springframework.scheduling.annotation.Scheduled(fixedDelay=60000)
 @Transactional
 public void expireStale(){reservations.expireStale(Instant.now());}
 private static QuotaExceededException quota(String message,boolean org){return new QuotaExceededException(message,"/pricing",org?"ORG_MONTHLY":"B2C_DAILY",null);}
}
