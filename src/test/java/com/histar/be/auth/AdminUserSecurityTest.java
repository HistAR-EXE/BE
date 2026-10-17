package com.histar.be.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AdminUserSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String userToken;
    private String adminToken;
    private UUID regularUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Profile user = profileRepository.save(Profile.builder()
                .email("regular@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Regular User")
                .provider("local")
                .role(UserRole.USER.name())
                .accountStatus(AccountStatus.ACTIVE.name())
                .emailVerified(true)
                .emailVerifiedAt(Instant.now())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        regularUserId = user.getId();
        userToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), UserRole.USER.name());

        Profile admin = profileRepository.save(Profile.builder()
                .email("admin@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Admin User")
                .provider("local")
                .role(UserRole.ADMIN.name())
                .accountStatus(AccountStatus.ACTIVE.name())
                .emailVerified(true)
                .emailVerifiedAt(Instant.now())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), UserRole.ADMIN.name());
    }

    @Test
    void userCannotListAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void adminCanUpdateUserRole() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"reason\":\"Promote for console operations\",\"expectedRole\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        mockMvc.perform(get("/api/admin/audit")
                        .param("entityType", "USER")
                        .param("entityId", regularUserId.toString())
                        .param("action", "ROLE_CHANGED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].actorId").value(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void userCannotUpdateRoles() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/role")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"reason\":\"Promote for console operations\",\"expectedRole\":\"USER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotUseNewIamEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users/export.csv").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"status\":\"SUSPENDED\",\"reason\":\"Khóa tạm thử nghiệm bảo mật\",\"expectedStatus\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/bulk")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[\""
                                + regularUserId
                                + "\"],\"action\":\"SUSPEND\",\"reason\":\"Khóa tạm thử nghiệm bảo mật\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSuspendAndSuspendedUserCannotUseJwt() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"status\":\"SUSPENDED\",\"reason\":\"Khóa tạm sau báo cáo lạm dụng\",\"expectedStatus\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"));

        // Suspended JWT is ignored by filter → unauthenticated → 401
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"regular@test.local\",\"password\":\"Test1234!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanExportUsersCsv() throws Exception {
        mockMvc.perform(get("/api/admin/users/export.csv").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUseCanonicalAndLegacyB2b2cInquiryRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/b2b2c-inquiries")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/billing/admin/b2b2c-inquiries")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotUseCanonicalB2b2cInquiryRoute() throws Exception {
        mockMvc.perform(get("/api/admin/b2b2c-inquiries")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionReplayEndpointIsNotAvailable() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/sessions/" + UUID.randomUUID() + "/replay")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminOperationalEndpointsAreProtectedAndAvailable() throws Exception {
        for (String path : new String[] {
            "/api/admin/trash", "/api/admin/audit", "/api/admin/dashboard", "/api/admin/content-reports"
        }) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(path).header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }
}
