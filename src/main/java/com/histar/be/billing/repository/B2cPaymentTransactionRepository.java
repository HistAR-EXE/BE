package com.histar.be.billing.repository;

import com.histar.be.billing.entity.B2cPaymentTransaction;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface B2cPaymentTransactionRepository extends JpaRepository<B2cPaymentTransaction, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from B2cPaymentTransaction t where t.id = :id")
    Optional<B2cPaymentTransaction> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from B2cPaymentTransaction t where t.orderCode = :orderCode")
    Optional<B2cPaymentTransaction> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from B2cPaymentTransaction t where t.transferContent = :transferContent")
    Optional<B2cPaymentTransaction> findByTransferContentForUpdate(@Param("transferContent") String transferContent);

    Optional<B2cPaymentTransaction> findByOrderCode(String orderCode);

    Optional<B2cPaymentTransaction> findByTransferContent(String transferContent);

    Optional<B2cPaymentTransaction> findByProviderTransactionId(Long providerTransactionId);

    Optional<B2cPaymentTransaction> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    @Query("""
            select count(t) from B2cPaymentTransaction t
            where (:status is null or upper(t.status) = :status)
              and (:from is null or coalesce(t.paidAt, t.createdAt) >= :from)
              and (:to is null or coalesce(t.paidAt, t.createdAt) <= :to)
            """)
    long countForAdminExport(
            @Param("status") String status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select t from B2cPaymentTransaction t
            where (:status is null or upper(t.status) = :status)
              and (:from is null or coalesce(t.paidAt, t.createdAt) >= :from)
              and (:to is null or coalesce(t.paidAt, t.createdAt) <= :to)
            """)
    List<B2cPaymentTransaction> findForAdminExport(
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    List<B2cPaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<B2cPaymentTransaction> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("select coalesce(sum(t.amountVnd), 0) from B2cPaymentTransaction t "
            + "where upper(t.status) = 'PAID' and t.paidAt is not null and t.paidAt >= :from and t.paidAt < :to")
    long sumPaidAmountBetween(@Param("from") Instant from, @Param("to") Instant to);

    long countByStatusIgnoreCaseAndPaidAtIsNotNullAndPaidAtBefore(String status, Instant before);
}
