package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.histar.be.config.*;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.organization.dto.*;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrgInviteEmailServiceTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void invitationStatusMatchesActualMailTransport(boolean enabled) {
        UUID orgId = UUID.randomUUID(), actor = UUID.randomUUID();
        var codes = mock(OrgInviteService.class);
        var access = mock(OrgAccessService.class);
        var organizations = mock(OrganizationRepository.class);
        var profiles = mock(ProfileRepository.class);
        var mail = mock(HistarEmailService.class);
        var invitations = mock(OrgEmailInvitationService.class);
        var app = new HistarAppProperties(); app.setFrontendUrl("http://localhost:5173");
        var mailProperties = new HistarMailProperties(); mailProperties.setEnabled(enabled);
        var service = new OrgInviteEmailService(codes, access, organizations, profiles, mail, app,
                mailProperties, new TestHookProperties(), invitations,
                mock(OrganizationEntitlementService.class), mock(jakarta.persistence.EntityManager.class));
        when(profiles.findByIdForUpdate(actor)).thenReturn(Optional.of(Profile.builder().id(actor).build()));
        when(organizations.findByIdForUpdate(orgId)).thenReturn(Optional.of(Organization.builder().id(orgId)
                .inviteCode("ABCDEF").name("Local school").build()));
        when(codes.getInviteCode(orgId)).thenReturn(new OrgInviteCodeResponse("ABCDEF", Instant.now().plusSeconds(3600), orgId, "/join?code=ABCDEF"));
        String delivery = enabled ? "SENT" : "LOCAL_ONLY";
        var response = new OrgEmailInvitationResponse("student@example.test", "PENDING", delivery, Instant.now().plusSeconds(3600));
        when(invitations.record(orgId, actor, "student@example.test", delivery)).thenReturn(response);
        assertThat(service.sendInviteEmail(orgId, actor, new OrgInviteEmailRequest("student@example.test", "<Student>")))
                .isEqualTo(response);
        if (enabled) verify(mail).sendHtml(eq("student@example.test"), anyString(), argThat(html ->
                html.contains("&lt;Student&gt;") && !html.contains("<Student>")));
        else verifyNoInteractions(mail);
    }
}
