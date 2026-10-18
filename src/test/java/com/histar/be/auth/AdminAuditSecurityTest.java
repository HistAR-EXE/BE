package com.histar.be.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.histar.be.profile.entity.AccountStatus;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.security.JwtService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminAuditSecurityTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Profile user = profileRepository.save(Profile.builder()
                .email("audit-user@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Audit User")
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
    }

    @Test
    void nonAdminCannotVerifyIntegrity() throws Exception {
        mockMvc.perform(get("/api/admin/audit/integrity").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotExportAudit() throws Exception {
        mockMvc.perform(get("/api/admin/audit/export.json").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
