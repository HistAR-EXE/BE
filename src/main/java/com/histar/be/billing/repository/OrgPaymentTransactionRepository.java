package com.histar.be.billing.repository;

import com.histar.be.billing.entity.OrgPaymentTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface OrgPaymentTransactionRepository extends JpaRepository<OrgPaymentTransaction, UUID> {

    Optional<OrgPaymentTransaction> findByOrderCode(String orderCode);

    Optional<OrgPaymentTransaction> findByTransferContent(String transferContent);

    Optional<OrgPaymentTransaction> findByProviderTransactionId(Long providerTransactionId);

    Optional<OrgPaymentTransaction> findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(UUID requesterUserId, String status);

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
