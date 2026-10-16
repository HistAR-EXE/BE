package com.histar.be.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.billing.dto.AdminRecentB2cPaymentItem;
import org.junit.jupiter.api.Test;

class AdminRecentB2cPaymentItemContractTest {

    @Test
    void emitsCanonicalPaymentFieldsAndLegacyAliases() {
        AdminRecentB2cPaymentItem item = new AdminRecentB2cPaymentItem(
                "ORDER-1", "PAID", "payer@example.com", 49_000, null, "ORDER1", 49_000);

        var json = new ObjectMapper().findAndRegisterModules().valueToTree(item);
        assertThat(json.path("payerEmail").asText()).isEqualTo("payer@example.com");
        assertThat(json.path("userEmail").asText()).isEqualTo("payer@example.com");
        assertThat(json.path("amountVnd").asInt()).isEqualTo(49_000);
        assertThat(json.path("amount").asInt()).isEqualTo(49_000);
        assertThat(item.userEmail()).isEqualTo(item.payerEmail());
        assertThat(item.amount()).isEqualTo(item.amountVnd());
    }
}
