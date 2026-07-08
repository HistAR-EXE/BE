package com.histar.be.billing.repository;

import com.histar.be.billing.entity.OrgPaymentTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgPaymentTransactionRepository extends JpaRepository<OrgPaymentTransaction, UUID> {

    Optional<OrgPaymentTransaction> findByOrderCode(String orderCode);

    Optional<OrgPaymentTransaction> findByTransferContent(String transferContent);

    Optional<OrgPaymentTransaction> findByProviderTransactionId(Long providerTransactionId);

    Optional<OrgPaymentTransaction> findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(UUID requesterUserId, String status);
}
