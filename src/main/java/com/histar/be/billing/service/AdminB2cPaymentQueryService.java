package com.histar.be.billing.service;

import com.histar.be.billing.dto.AdminRecentB2cPaymentItem;
import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminB2cPaymentQueryService {

    private final B2cPaymentTransactionRepository paymentTransactionRepository;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<AdminRecentB2cPaymentItem> listRecent(int limit) {
        int size = clamp(limit);
        return paymentTransactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size)).stream()
                .map(this::toItem)
                .toList();
    }

    /** Lists payments filtered by status (e.g. UNDERPAID); blank status behaves like {@link #listRecent(int)}. */
    @Transactional(readOnly = true)
    public List<AdminRecentB2cPaymentItem> listByStatus(String status, int limit) {
        if (status == null || status.isBlank()) {
            return listRecent(limit);
        }
        return paymentTransactionRepository
                .findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(Locale.ROOT), PageRequest.of(0, clamp(limit)))
                .stream()
                .map(this::toItem)
                .toList();
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 100);
    }

    private AdminRecentB2cPaymentItem toItem(B2cPaymentTransaction tx) {
        String email = profileRepository
                .findById(tx.getUserId())
                .map(p -> p.getEmail())
                .orElse(null);
        int received = SepayB2cPaymentService.STATUS_UNDERPAID.equals(tx.getStatus())
                ? SepayB2cPaymentService.receivedAmountFromPayload(tx.getProviderPayload())
                : ("PAID".equals(tx.getStatus()) ? tx.getAmountVnd() : 0);
        return new AdminRecentB2cPaymentItem(
                tx.getOrderCode(),
                tx.getStatus(),
                email,
                tx.getAmountVnd(),
                tx.getCreatedAt(),
                tx.getTransferContent(),
                received);
    }
}
