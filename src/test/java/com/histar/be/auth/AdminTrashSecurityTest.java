package com.histar.be.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminTrashSecurityTest {

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
                .email("trash-user@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Trash User")
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
    void nonAdminCannotPurge() throws Exception {
        mockMvc.perform(delete("/api/admin/trash/QUEST/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rowVersion\":1,\"reason\":\"Xóa vĩnh viễn kiểm thử\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotBulkTrash() throws Exception {
        mockMvc.perform(post("/api/admin/trash/bulk")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"action\":\"RESTORE\",\"items\":[{\"resourceType\":\"QUEST\",\"id\":\""
                                        + UUID.randomUUID()
                                        + "\",\"rowVersion\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotEmptyTrash() throws Exception {
        mockMvc.perform(post("/api/admin/trash/empty")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Dọn sạch thùng rác kiểm thử\"}"))
                .andExpect(status().isForbidden());
    }
}
