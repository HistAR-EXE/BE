package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.mail.HistarEmailService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class B2b2cInquiryPagingTest {
    @Mock HeritageDigitizationInquiryRepository repository;
    @Mock HistarEmailService email;
    @Mock HistarOrgProperties properties;
    @Mock AdminAuditService audit;
    @Mock CurrentUserAccessor currentUser;
    @InjectMocks B2b2cInquiryService service;

    @Test
    void returnsFilteredPageContractWhenPagingRequested() {
        var inquiry = HeritageDigitizationInquiry.builder()
                .siteName("Hue").contactName("Lan").contactEmail("lan@hue.vn")
                .packageType("PILOT").status("NEW").createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inquiry)));

        var result = service.listPaged(0, 20, "NEW", "hue", null, null);

        assertThat(result.getItems()).singleElement().extracting("contactEmail").isEqualTo("lan@hue.vn");
        assertThat(result.getTotalItems()).isEqualTo(1);
    }
}
