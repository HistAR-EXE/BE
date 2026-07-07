package com.histar.be.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.recommendation.dto.RecommendationItem;
import com.histar.be.recommendation.dto.RecommendationsResponse;
import com.histar.be.visit.dto.StartVisitSessionRequest;
import com.histar.be.visit.service.VisitSessionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional // Tự động Rollback sau mỗi bài test để giữ sạch DB
class RecommendationServicePersonalizationTest {

    @Autowired
    private VisitSessionService visitSessionService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private DiscoveryPointRepository discoveryPointRepository;

    @Autowired
    private LocationRepository locationRepository;

    // INJECT JDBC TEMPLATE ĐỂ TRUY VẤN TRỰC TIẾP HOẶC TẠO USER ID HỢP LỆ TRONG BẢNG PROFILES
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID testUserId;
    private UUID testLocationId;

    @BeforeEach
    void setUp() {
        // 1. LẤY HOẶC TẠO USER ID HỢP LỆ TRONG BẢNG PROFILES (TRÁNH LỖI FOREIGN KEY)
        List<UUID> existingUsers = jdbcTemplate.queryForList("SELECT id FROM profiles LIMIT 1", UUID.class);
        if (!existingUsers.isEmpty()) {
            testUserId = existingUsers.get(0);
        } else {
            testUserId = UUID.randomUUID();
            try {
                jdbcTemplate.update("INSERT INTO profiles (id, display_name) VALUES (?, ?)", testUserId, "Test User HistAR");
            } catch (Exception e) {
                // Nếu bảng profiles có cấu trúc tối giản hơn
                jdbcTemplate.update("INSERT INTO profiles (id) VALUES (?)", testUserId);
            }
        }

        // 2. LẤY ID CỦA ĐỊA ĐIỂM ĐẦU TIÊN CÓ SẴN TRONG BẢNG LOCATIONS (VD: CỦ CHI)
        testLocationId = locationRepository.findAll().get(0).getId();

        // 3. TẠO 2 ĐIỂM POI GIẢ LẬP GẮN VÀO ĐỊA ĐIỂM TRÊN ĐỂ THUẬT TOÁN CÓ DỮ LIỆU TÍNH TOÁN
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);

        DiscoveryPoint poi1 = DiscoveryPoint.builder()
                .locationId(testLocationId)
                .unlockKey("test_poi_bep_" + randomSuffix)
                .name("Bếp Hoàng Cầm Test")
                .mapXPct(BigDecimal.valueOf(10.0))
                .mapYPct(BigDecimal.valueOf(10.0))
                .sortOrder(1)
                .build();

        DiscoveryPoint poi2 = DiscoveryPoint.builder()
                .locationId(testLocationId)
                .unlockKey("test_poi_ham_" + randomSuffix)
                .name("Hầm Chông Test")
                .mapXPct(BigDecimal.valueOf(20.0))
                .mapYPct(BigDecimal.valueOf(20.0))
                .sortOrder(2)
                .build();

        discoveryPointRepository.save(poi1);
        discoveryPointRepository.save(poi2);
    }

    @Test
    @DisplayName("TC1: Khi chọn Persona Goal là 'research' -> Thuật toán ưu tiên lý do Khảo cứu tư liệu")
    void testRecommendation_WhenGoalIsResearch() {
        StartVisitSessionRequest request = new StartVisitSessionRequest(
                testLocationId, "online", "research", "60", "modern"
        );
        visitSessionService.startSessionWithPersonalization(testUserId, request);

        RecommendationsResponse response = recommendationService.forUser(testUserId, testLocationId, null);

        assertThat(response.items()).isNotEmpty();
        RecommendationItem firstItem = response.items().get(0);

        assertThat(firstItem.reason()).containsAnyOf(
                "🔍 Khảo cứu tư liệu chuyên sâu",
                "📜 Khảo cứu kiến trúc"
        );
        assertThat(firstItem.triggerType()).contains("research");
    }

    @Test
    @DisplayName("TC2: Khi chọn Persona Goal là 'study' -> Thuật toán ưu tiên lý do Ôn tập & lấy XP")
    void testRecommendation_WhenGoalIsStudy() {
        StartVisitSessionRequest request = new StartVisitSessionRequest(
                testLocationId, "online", "study", "30", "heritage"
        );
        visitSessionService.startSessionWithPersonalization(testUserId, request);

        RecommendationsResponse response = recommendationService.forUser(testUserId, testLocationId, null);

        assertThat(response.items()).isNotEmpty();
        RecommendationItem firstItem = response.items().get(0);

        assertThat(firstItem.reason()).containsAnyOf(
                "⭐ Ôn tập & Tích lũy XP",
                "📚 Tiến độ học tập"
        );
        assertThat(firstItem.triggerType()).contains("study");
    }

    @Test
    @DisplayName("TC3: Khi chọn Persona Goal là 'travel' -> Thuật toán ưu tiên lý do Check-in du lịch")
    void testRecommendation_WhenGoalIsTravel() {
        StartVisitSessionRequest request = new StartVisitSessionRequest(
                testLocationId, "offline", "travel", "15", "heritage"
        );
        visitSessionService.startSessionWithPersonalization(testUserId, request);

        RecommendationsResponse response = recommendationService.forUser(testUserId, testLocationId, null);

        assertThat(response.items()).isNotEmpty();
        RecommendationItem firstItem = response.items().get(0);

        assertThat(firstItem.reason()).containsAnyOf(
                "🎒 Điểm tham quan nổi bật",
                "📸 Lộ trình check-in"
        );
        assertThat(firstItem.triggerType()).contains("travel");
    }
}