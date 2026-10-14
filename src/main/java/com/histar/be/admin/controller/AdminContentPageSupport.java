package com.histar.be.admin.controller;

import com.histar.be.common.response.PageResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class AdminContentPageSupport {

    private AdminContentPageSupport() {
    }

    static <T> PageResponse<T> page(
            List<T> source,
            int requestedPage,
            int requestedSize,
            String query,
            String sort,
            Function<T, String> label) {
        int page = Math.max(requestedPage, 0);
        int size = Math.min(Math.max(requestedSize, 1), 100);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean descending = sort != null && sort.toLowerCase(Locale.ROOT).endsWith(",desc");

        Comparator<T> comparator = Comparator.comparing(
                item -> safeLabel(label.apply(item)),
                String.CASE_INSENSITIVE_ORDER);
        if (descending) {
            comparator = comparator.reversed();
        }

        List<T> filtered = source.stream()
                .filter(item -> normalizedQuery.isEmpty()
                        || safeLabel(label.apply(item)).toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(comparator)
                .toList();
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / size);

        return PageResponse.<T>builder()
                .items(filtered.subList(fromIndex, toIndex))
                .page(page)
                .size(size)
                .totalItems(filtered.size())
                .totalPages(totalPages)
                .build();
    }

    private static String safeLabel(String value) {
        return value == null ? "" : value;
    }
}
