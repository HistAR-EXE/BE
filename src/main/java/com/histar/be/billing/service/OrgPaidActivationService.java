package com.histar.be.billing.service;

import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A separately committed receipt is never rolled back by failed entitlement activation. */
@Service
@RequiredArgsConstructor
public class OrgPaidActivationService {
    private final OrgPaymentTransactionRepository payments;
    private final ObjectProvider<SepayOrgPaymentService> sepay;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activate(UUID paymentId) {
        var payment = payments.findByIdForUpdate(paymentId).orElseThrow();
        if (!"PAID".equalsIgnoreCase(payment.getStatus()) || payment.getSubscriptionId() != null) return;
        sepay.getObject().activatePaidOrgOnce(payment);
    }
}
