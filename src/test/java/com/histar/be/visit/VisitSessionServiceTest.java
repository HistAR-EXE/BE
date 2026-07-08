package com.histar.be.visit;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.visit.entity.VisitSession;
import com.histar.be.visit.repository.VisitSessionRepository;
import com.histar.be.visit.service.VisitSessionService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VisitSessionServiceTest {

    @Autowired
    private VisitSessionService visitSessionService;

    @Autowired
    private VisitSessionRepository visitSessionRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID userId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        Profile profile = profileRepository.save(Profile.builder()
                .email("visit-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Visit Tester")
                .provider("local")
                .role(UserRole.USER.name())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
        locationId = UUID.randomUUID();
    }

    @Test
    void startSession_reusesPriorActiveSession() {
        UUID first = visitSessionService.startSession(userId, locationId, "online");
        UUID second = visitSessionService.startSession(userId, locationId, "online");

        assertThat(second).isEqualTo(first);

        VisitSession active = visitSessionRepository.findById(second).orElseThrow();
        assertThat(active.getEndedAt()).isNull();
        assertThat(active.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void endSession_marksClosed() {
        UUID sessionId = visitSessionService.startSession(userId, locationId, "offline");
        visitSessionService.endSession(userId, sessionId);

        VisitSession session = visitSessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus()).isEqualTo("CLOSED");
        assertThat(session.getEndedAt()).isNotNull();
    }
}
