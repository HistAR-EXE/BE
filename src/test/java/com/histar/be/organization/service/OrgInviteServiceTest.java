package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.service.BillingService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgInviteServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrgAccessService orgAccessService;

    private OrgInviteService orgInviteService;

    @BeforeEach
    void setUp() {
        HistarOrgProperties properties = new HistarOrgProperties();
        properties.getOrg().setInviteTtlDays(7);
        HistarAppProperties appProperties = new HistarAppProperties();
        appProperties.setFrontendUrl("http://localhost:5173");
        orgInviteService = new OrgInviteService(organizationRepository, orgAccessService, properties, appProperties);
    }

    @Test
    void generateInviteCode_returnsSixCharacterCodeAndJoinUrl() {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .name("THPT Nguyen Du")
                .status("ACTIVE")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(organizationRepository.findByInviteCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(organizationRepository.save(org)).thenReturn(org);

        var response = orgInviteService.generateInviteCode(orgId);

        assertThat(response.inviteCode()).hasSize(6);
        assertThat(response.inviteUrl()).isEqualTo("http://localhost:5173/join?code=" + response.inviteCode());
        verify(organizationRepository, never()).findByInviteCodeIgnoreCase(response.inviteCode().toLowerCase());
    }

    @Test
    void generateInviteCode_rejectsWhenTrialExpired() {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .name("Expired Trial School")
                .status(BillingService.ORG_STATUS_TRIAL_EXPIRED)
                .build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> orgInviteService.generateInviteCode(orgId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE);
                    assertThat(bre.getMessage()).contains("Không thể tạo mã mời mới");
                });
        verify(organizationRepository, never()).save(org);
    }

    @Test
    void generateInviteCode_rejectsWhenExpired() {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .name("Expired School")
                .status(BillingService.ORG_STATUS_EXPIRED)
                .build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> orgInviteService.generateInviteCode(orgId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hết hạn");
        verify(organizationRepository, never()).save(org);
    }
}
