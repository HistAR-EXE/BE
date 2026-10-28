package com.histar.be.billing.repository;
import com.histar.be.billing.entity.AiQuotaReservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
public interface AiQuotaReservationRepository extends JpaRepository<AiQuotaReservation,UUID> {
 interface Scope { UUID getUserId(); UUID getOrganizationId(); }
 @Query("select r.userId as userId, r.organizationId as organizationId from AiQuotaReservation r where r.id=:id")
 Optional<Scope> findScope(@Param("id") UUID id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from AiQuotaReservation r where r.id=:id")
 Optional<AiQuotaReservation> findByIdForUpdate(@Param("id") UUID id);
 @Query("select count(r) from AiQuotaReservation r where r.userId=:user and r.organizationId is null and r.quotaDate=:day and r.status='ACTIVE' and r.expiresAt>:now")
 long countActiveUser(@Param("user") UUID user,@Param("day") LocalDate day,@Param("now") Instant now);
 @Query("select count(r) from AiQuotaReservation r where r.organizationId=:org and r.quotaDate=:month and r.status='ACTIVE' and r.expiresAt>:now")
 long countActiveOrg(@Param("org") UUID org,@Param("month") LocalDate month,@Param("now") Instant now);
 @Modifying @Query("update AiQuotaReservation r set r.status='RELEASED' where r.id=:id and r.userId=:user and r.status='ACTIVE'")
 int release(@Param("id") UUID id,@Param("user") UUID user);
 @Modifying @Query("update AiQuotaReservation r set r.status='EXPIRED' where r.status='ACTIVE' and r.expiresAt<=:now")
 int expireStale(@Param("now") Instant now);
}
