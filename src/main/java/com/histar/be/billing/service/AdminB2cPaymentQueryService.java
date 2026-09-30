package com.histar.be.billing.service;

import com.histar.be.billing.dto.AdminRecentB2cPaymentItem;
import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
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
        int size = Math.min(Math.max(limit, 1), 100);
        return paymentTransactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size)).stream()
                .map(this::toItem)
                .toList();
    }

    private AdminRecentB2cPaymentItem toItem(B2cPaymentTransaction tx) {
        String email = profileRepository
                .findById(tx.getUserId())
                .map(p -> p.getEmail())
                .orElse(null);
        return new AdminRecentB2cPaymentItem(
                tx.getOrderCode(),
                tx.getStatus(),
                email,
                tx.getAmountVnd(),
                tx.getCreatedAt());
    }
}
