package com.histar.be.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.common.response.PageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminContentPageSupportTest {

    @Test
    void filtersSortsAndPaginatesContent() {
        PageResponse<String> result = AdminContentPageSupport.page(
                List.of("Zulu", "alpha", "Alpine", "beta"),
                0,
                1,
                "al",
                "name,asc",
                value -> value);

        assertThat(result.getItems()).containsExactly("alpha");
        assertThat(result.getTotalItems()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void clampsPageSizeAndSupportsDescendingSort() {
        PageResponse<String> result = AdminContentPageSupport.page(
                List.of("a", "c", "b"),
                -2,
                0,
                null,
                "name,desc",
                value -> value);

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getItems()).containsExactly("c");
    }
}
