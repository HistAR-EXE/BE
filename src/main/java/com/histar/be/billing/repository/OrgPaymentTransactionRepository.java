package com.histar.be.billing.repository;

import com.histar.be.billing.entity.OrgPaymentTransaction;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface OrgPaymentTransactionRepository extends JpaRepository<OrgPaymentTransaction, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from OrgPaymentTransaction t where t.id = :id")
    Optional<OrgPaymentTransaction> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from OrgPaymentTransaction t where t.orderCode = :orderCode")
    Optional<OrgPaymentTransaction> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from OrgPaymentTransaction t where t.transferContent = :transferContent")
    Optional<OrgPaymentTransaction> findByTransferContentForUpdate(@Param("transferContent") String transferContent);

    Optional<OrgPaymentTransaction> findByOrderCode(String orderCode);

    Optional<OrgPaymentTransaction> findByTransferContent(String transferContent);

    Optional<OrgPaymentTransaction> findByProviderTransactionId(Long providerTransactionId);

    Optional<OrgPaymentTransaction> findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(UUID requesterUserId, String status);

    @Query("""
            select count(t) from OrgPaymentTransaction t
            where (:status is null or upper(t.status) = :status)
              and (:from is null or coalesce(t.paidAt, t.createdAt) >= :from)
              and (:to is null or coalesce(t.paidAt, t.createdAt) <= :to)
            """)
    long countForAdminExport(
            @Param("status") String status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select t from OrgPaymentTransaction t
            where (:status is null or upper(t.status) = :status)
              and (:from is null or coalesce(t.paidAt, t.createdAt) >= :from)
              and (:to is null or coalesce(t.paidAt, t.createdAt) <= :to)
            """)
    java.util.List<OrgPaymentTransaction> findForAdminExport(
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("select coalesce(sum(t.amountVnd), 0) from OrgPaymentTransaction t "
            + "where upper(t.status) = 'PAID' and t.paidAt is not null and t.paidAt >= :from and t.paidAt < :to")
    long sumPaidAmountBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(t.planType, 'UNKNOWN'), coalesce(sum(t.amountVnd), 0) from OrgPaymentTransaction t "
            + "where upper(t.status) = 'PAID' and t.paidAt is not null and t.paidAt >= :from and t.paidAt < :to "
            + "group by coalesce(t.planType, 'UNKNOWN') order by coalesce(sum(t.amountVnd), 0) desc")
    java.util.List<Object[]> sumPaidAmountGroupedByPlanTypeBetween(
            @Param("from") Instant from, @Param("to") Instant to);

    long countByStatusIgnoreCaseAndPaidAtIsNotNullAndPaidAtBefore(String status, Instant before);
}
