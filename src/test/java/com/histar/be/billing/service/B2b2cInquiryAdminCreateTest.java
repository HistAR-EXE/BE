package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.billing.dto.AdminCreateB2b2cInquiryRequest;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.mail.HistarEmailService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class B2b2cInquiryAdminCreateTest {
    @Mock HeritageDigitizationInquiryRepository repository;
    @Mock HistarEmailService email;
    @Mock HistarOrgProperties properties;
    @Mock AdminAuditService audit;
    @Mock CurrentUserAccessor currentUser;
    @InjectMocks B2b2cInquiryService service;

    @Test
    void createAsAdminPersistsLeadAndAudits() {
        UUID actor = UUID.randomUUID();
        when(currentUser.getUserId()).thenReturn(Optional.of(actor));
        when(repository.save(any(HeritageDigitizationInquiry.class))).thenAnswer(invocation -> {
            HeritageDigitizationInquiry inquiry = invocation.getArgument(0);
            inquiry.setId(UUID.randomUUID());
            return inquiry;
        });

        var item = service.createAsAdmin(new AdminCreateB2b2cInquiryRequest(
                " Bảo tàng Demo ",
                " Lan ",
                "Lan@Demo.vn",
                "0901",
                "edu_k12",
                "Muốn demo",
                "Ưu tiên Q2",
                "Hoa-Lo",
                "CONTACTED"));

        assertThat(item.siteName()).isEqualTo("Bảo tàng Demo");
        assertThat(item.contactEmail()).isEqualTo("lan@demo.vn");
        assertThat(item.packageType()).isEqualTo("EDU_K12");
        assertThat(item.status()).isEqualTo("CONTACTED");
        assertThat(item.interestSiteCode()).isEqualTo("hoa-lo");
        assertThat(item.adminNotes()).isEqualTo("Ưu tiên Q2");
        assertThat(item.contactedAt()).isNotNull();

        ArgumentCaptor<AdminAuditCommand> auditCaptor = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("CREATED");
    }
}
