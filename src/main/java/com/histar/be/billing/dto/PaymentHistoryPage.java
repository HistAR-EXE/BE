package com.histar.be.billing.dto;

import java.util.List;

public record PaymentHistoryPage(List<PaymentHistoryItem> items, int page, int size,
        long totalItems, int totalPages, Summary summary) {
    public record Summary(long totalTransactions, long paidTransactions, long totalReceivedVnd, long activePackages) {}
}
