package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.billing.dto.B2b2cInquiryRequest;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.mail.HistarEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class B2b2cInquirySubmissionTest {
    @Mock HeritageDigitizationInquiryRepository repository;
    @Mock HistarEmailService email;
    @Mock HistarOrgProperties properties;
    @Mock AdminAuditService audit;
    @Mock CurrentUserAccessor currentUser;
    @InjectMocks B2b2cInquiryService service;

    @Test
    void savesFreeTextPilotSiteAndConsultationDetailsForSales() {
        when(repository.save(any(HeritageDigitizationInquiry.class))).thenAnswer(call -> call.getArgument(0));
        service.submit(new B2b2cInquiryRequest(
                " Khu di tích Nhà tù Hỏa Lò ", " Lan ", "Lan@example.vn", " 0901234567 ",
                "UNDECIDED", "Tour tự hướng dẫn cho khách quốc tế", null, null, null,
                " Ban quản lý di tích ", " Phòng truyền thông ", " Hà Nội ",
                "Ảnh tư liệu và bản đồ cũ", " Quý III/2027 "));

        var saved = ArgumentCaptor.forClass(HeritageDigitizationInquiry.class);
        org.mockito.Mockito.verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSiteName()).isEqualTo("Khu di tích Nhà tù Hỏa Lò");
        assertThat(saved.getValue().getOrganizationName()).isEqualTo("Ban quản lý di tích");
        assertThat(saved.getValue().getContactRole()).isEqualTo("Phòng truyền thông");
        assertThat(saved.getValue().getSiteLocation()).isEqualTo("Hà Nội");
        assertThat(saved.getValue().getAvailableMaterials()).isEqualTo("Ảnh tư liệu và bản đồ cũ");
        assertThat(saved.getValue().getTargetStart()).isEqualTo("Quý III/2027");
        assertThat(saved.getValue().getPackageType()).isEqualTo("UNDECIDED");
        when(repository.findAll()).thenReturn(List.of(saved.getValue()));
        var adminItem = service.listAll().get(0);
        assertThat(adminItem.organizationName()).isEqualTo("Ban quản lý di tích");
        assertThat(adminItem.siteName()).isEqualTo("Khu di tích Nhà tù Hỏa Lò");
        assertThat(adminItem.availableMaterials()).isEqualTo("Ảnh tư liệu và bản đồ cũ");
        assertThat(adminItem.targetStart()).isEqualTo("Quý III/2027");
    }

    @Test
    void mailFailureDoesNotRejectNewLead() {
        when(repository.save(any(HeritageDigitizationInquiry.class))).thenAnswer(call -> call.getArgument(0));
        when(properties.getSalesInbox()).thenReturn("sales@example.invalid");
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(email).sendHtml(org.mockito.ArgumentMatchers.eq("lan@example.vn"),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        var response = service.submit(new B2b2cInquiryRequest(
                "Di tích", "Lan", "Lan@example.vn", null, "UNDECIDED", "Tư vấn",
                null, null, null, null, null, null, null, null));

        assertThat(response.status()).isEqualTo("NEW");
        verify(repository).save(any(HeritageDigitizationInquiry.class));
        verify(email).sendHtml(org.mockito.ArgumentMatchers.eq("sales@example.invalid"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
