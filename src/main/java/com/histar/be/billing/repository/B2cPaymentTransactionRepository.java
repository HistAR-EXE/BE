package com.histar.be.billing.repository;

import com.histar.be.billing.entity.B2cPaymentTransaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface B2cPaymentTransactionRepository extends JpaRepository<B2cPaymentTransaction, UUID> {

    Optional<B2cPaymentTransaction> findByOrderCode(String orderCode);

    Optional<B2cPaymentTransaction> findByTransferContent(String transferContent);

    Optional<B2cPaymentTransaction> findByProviderTransactionId(Long providerTransactionId);

    Optional<B2cPaymentTransaction> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    List<B2cPaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<B2cPaymentTransaction> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("select coalesce(sum(t.amountVnd), 0) from B2cPaymentTransaction t "
            + "where upper(t.status) = 'PAID' and t.paidAt is not null and t.paidAt >= :from and t.paidAt < :to")
    long sumPaidAmountBetween(@Param("from") Instant from, @Param("to") Instant to);

    long countByStatusIgnoreCaseAndPaidAtIsNotNullAndPaidAtBefore(String status, Instant before);
}
