package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminPaymentQueryServiceTest {
    @Mock B2cPaymentTransactionRepository b2c;
    @Mock OrgPaymentTransactionRepository b2b;
    @Mock ProfileRepository profiles;
    @InjectMocks AdminPaymentQueryService service;

    @Test
    void returnsUnifiedPagedLongAmountAndPayerEmail() {
        UUID userId = UUID.randomUUID();
        UUID b2cId = UUID.randomUUID();
        when(b2c.findAll()).thenReturn(List.of(B2cPaymentTransaction.builder()
                .id(b2cId).userId(userId).orderCode("B2C1").status("PAID")
                .amountVnd(Integer.MAX_VALUE).planType("JOURNEY_PASS")
                .paidAt(Instant.parse("2026-10-02T00:00:00Z")).createdAt(Instant.parse("2026-10-01T00:00:00Z")).build()));
        when(b2b.findAll()).thenReturn(List.of(OrgPaymentTransaction.builder()
                .id(UUID.randomUUID()).contactEmail("buyer@school.vn").orderCode("B2B1").status("PAID")
                .amountVnd(3_000_000_000L).planType("STANDARD")
                .paidAt(Instant.parse("2026-10-03T00:00:00Z")).createdAt(Instant.parse("2026-10-01T00:00:00Z")).build()));
        when(profiles.findAllById(List.of(userId))).thenReturn(List.of(Profile.builder().id(userId).email("user@x.vn").build()));

        var page = service.search(0, 10, null, "PAID", null, null, "amountVnd,desc");

        assertThat(page.getTotalItems()).isEqualTo(2);
        assertThat(page.getItems()).extracting("channel").containsExactly("B2B", "B2C");
        assertThat(page.getItems().get(0).amountVnd()).isEqualTo(3_000_000_000L);
        assertThat(page.getItems().get(0).payerEmail()).isEqualTo("buyer@school.vn");
        assertThat(page.getItems().get(1).payerEmail()).isEqualTo("user@x.vn");
    }

    @Test
    void filtersByChannelStatusAndPaidDate() {
        when(b2b.findAll()).thenReturn(List.of(OrgPaymentTransaction.builder()
                .id(UUID.randomUUID()).contactEmail("buyer@school.vn").orderCode("B2B1").status("PAID")
                .amountVnd(1L).paidAt(Instant.parse("2026-10-03T00:00:00Z")).createdAt(Instant.EPOCH).build()));

        var page = service.search(0, 10, "B2B", "PAID",
                Instant.parse("2026-10-02T00:00:00Z"), Instant.parse("2026-10-04T00:00:00Z"), "paidAt,asc");

        assertThat(page.getItems()).singleElement().extracting("channel").isEqualTo("B2B");
    }

    @Test
    void exportsCsvUsingTheSameFiltersAndEscapesValues() {
        UUID userId = UUID.randomUUID();
        when(b2c.countForAdminExport(eq("PAID"), any(), any())).thenReturn(1L);
        when(b2c.findForAdminExport(eq("PAID"), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(B2cPaymentTransaction.builder()
                .id(UUID.randomUUID()).userId(userId).orderCode("B2C,1").status("PAID")
                .amountVnd(49_000).planType("PREMIUM")
                .paidAt(Instant.parse("2026-10-03T00:00:00Z"))
                .createdAt(Instant.parse("2026-10-01T00:00:00Z")).build()));
        lenient().when(profiles.findAllById(List.of(userId))).thenReturn(List.of(
                Profile.builder().id(userId).email("user@x.vn").build()));

        String csv = service.exportCsv(
                "B2C", "PAID", Instant.parse("2026-10-02T00:00:00Z"),
                Instant.parse("2026-10-04T00:00:00Z"), "paidAt,asc");

        assertThat(csv).startsWith("id,channel,orderCode,status,payerEmail,amountVnd,planType,organizationName,paidAt,createdAt\r\n");
        assertThat(csv).contains("B2C,\"B2C,1\",PAID,user@x.vn,49000,PREMIUM");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(b2c).findForAdminExport(eq("PAID"), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
        verify(b2c, never()).findAll();
    }

    @Test
    void neutralizesSpreadsheetFormulaLeadingTextCells() {
        UUID userId = UUID.randomUUID();
        when(b2c.countForAdminExport("PAID", null, null)).thenReturn(1L);
        when(b2c.findForAdminExport(eq("PAID"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(B2cPaymentTransaction.builder()
                .id(UUID.randomUUID()).userId(userId).orderCode("=1+1").status("PAID")
                .amountVnd(49_000).planType("@SUM(A1:A2)")
                .createdAt(Instant.parse("2026-10-01T00:00:00Z")).build()));
        lenient().when(profiles.findAllById(List.of(userId))).thenReturn(List.of(
                Profile.builder().id(userId).email("+cmd@example.vn").build()));

        String csv = service.exportCsv("B2C", "PAID", null, null, "createdAt,asc");

        assertThat(csv).contains("B2C,'=1+1,PAID,'+cmd@example.vn,49000,'@SUM(A1:A2)");
    }

    @Test
    void rejectsCsvExportWhenFilteredResultExceedsTenThousand() {
        when(b2c.countForAdminExport("PAID", null, null)).thenReturn(10_001L);

        assertThatThrownBy(() -> service.exportCsv("B2C", "PAID", null, null, "createdAt,asc"))
                .isInstanceOf(BusinessRuleException.class);

        verify(b2c, never()).findAll();
        verify(b2c, never()).findForAdminExport(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void rejectsCsvExportWhenCombinedChannelsExceedTenThousand() {
        when(b2c.countForAdminExport(null, null, null)).thenReturn(6_000L);
        when(b2b.countForAdminExport(null, null, null)).thenReturn(4_001L);

        assertThatThrownBy(() -> service.exportCsv(null, null, null, null, "createdAt,desc"))
                .isInstanceOf(BusinessRuleException.class);

        verify(b2c, never()).findForAdminExport(any(), any(), any(), any(Pageable.class));
        verify(b2b, never()).findForAdminExport(any(), any(), any(), any(Pageable.class));
    }
}
