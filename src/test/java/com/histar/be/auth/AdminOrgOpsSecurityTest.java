package com.histar.be.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.AccountStatus;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOrgOpsSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String userToken;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Profile user = profileRepository.save(Profile.builder()
                .email("org-ops-user@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Org Ops User")
                .provider("local")
                .role(UserRole.USER.name())
                .accountStatus(AccountStatus.ACTIVE.name())
                .emailVerified(true)
                .emailVerifiedAt(Instant.now())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), UserRole.USER.name());

        Organization org = organizationRepository.save(Organization.builder()
                .name("Org Ops Security School")
                .slug("org-ops-security-" + UUID.randomUUID().toString().substring(0, 8))
                .plan("STANDARD")
                .planType("STANDARD")
                .status("ACTIVE")
                .contactEmail("ops@school.test")
                .createdAt(Instant.now())
                .build());
        orgId = org.getId();
    }

    @Test
    void nonAdminCannotListContracts() throws Exception {
        mockMvc.perform(get("/api/admin/organizations/" + orgId + "/contracts")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotSyncCrm() throws Exception {
        mockMvc.perform(post("/api/admin/organizations/" + orgId + "/sync-crm")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotIssueArCards() throws Exception {
        mockMvc.perform(post("/api/admin/organizations/" + orgId + "/ar-cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotBulkCertificates() throws Exception {
        mockMvc.perform(post("/api/admin/organizations/" + orgId + "/certificates/bulk")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Chứng nhận\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotGetArReport() throws Exception {
        mockMvc.perform(get("/api/admin/organizations/" + orgId + "/ar-report")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotGetActivityLogs() throws Exception {
        mockMvc.perform(get("/api/admin/organizations/" + orgId + "/activity-logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
