package com.histar.be.chat.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChatKnowledgeRouteResolverTest {

    private final ChatKnowledgeRouteResolver resolver = new ChatKnowledgeRouteResolver();

    @ParameterizedTest(name = "{0} routes to {1}")
    @MethodSource("productAliases")
    void productAliasesSelectTheirFeature(String message, String featureKey) {
        var route = resolver.resolve(message, null, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.PRODUCT_HELP);
        assertThat(route.scopes()).containsExactly(ChatKnowledgeRoute.KnowledgeScope.PLATFORM);
        assertThat(route.featureKey()).isEqualTo(featureKey);
        assertThat(route.requiresClarification()).isFalse();
    }

    private static Stream<Arguments> productAliases() {
        return Stream.of(
                Arguments.of("TimeLens có 10 tab nào?", "OVERVIEW"),
                Arguments.of("Hệ sinh thái timelens có gì?", "OVERVIEW"),
                Arguments.of("Có bao nhiêu huy hiệu?", "BADGES"),
                Arguments.of("Cho tôi biết có bao nhiêu huy hiệu", "BADGES"),
                Arguments.of("gồm những huy nghiệp nào", "BADGES"),
                Arguments.of("Cách nhận Nhãn Quan Xuyên Thấu?", "BADGES"),
                Arguments.of("Bảo tàng 3D có gì?", "ARTIFACTS"),
                Arguments.of("bao tang co bao nhieu mo hinh", "ARTIFACTS"),
                Arguments.of("Bảng xếp hạng có tính lại mỗi tuần không?", "LEADERBOARD"),
                Arguments.of("Hóa thân 1968 có vai nào?", "PHOTO_MEMORY"),
                Arguments.of("Ảnh và chia sẻ dùng sao?", "PHOTO_MEMORY"),
                Arguments.of("Mật thư Chiến Khu có gì?", "EXPLORE"),
                Arguments.of("Hồ sơ Đất Thép có mấy bộ?", "EXPLORE"),
                Arguments.of("Xưa và nay có mấy điểm?", "TIME_PORTAL"),
                Arguments.of("Trợ lý AI có thể giúp gì?", "CHAT_AI"),
                Arguments.of("Các trạm hoạt động thế nào?", "STATION_MEDIA"),
                Arguments.of("Cổ vật dùng để làm gì?", "ARTIFACTS"),
                Arguments.of("Bộ sưu tập ở đâu?", "ARTIFACTS"),
                Arguments.of("Quest có tác dụng gì?", "QUESTS"),
                Arguments.of("Thử thách hoạt động ra sao?", "QUESTS"),
                Arguments.of("Sa bàn là gì?", "DIORAMA"),
                Arguments.of("Mô hình trận địa ở đâu?", "DIORAMA"),
                Arguments.of("Phim 3D khác gì Tour 360?", "HISTORICAL_REENACTMENT"),
                Arguments.of("Khám phá địa điểm bằng cách nào?", "EXPLORE"),
                Arguments.of("Quét mã QR như thế nào?", "STATION_CHECKIN"),
                Arguments.of("Check-in hoạt động ra sao?", "STATION_CHECKIN"),
                Arguments.of("AR tại nhà có tính check-in không?", "AR"),
                Arguments.of("Tour 360 dùng để làm gì?", "TOUR_360"),
                Arguments.of("Cổng thời gian nằm ở đâu?", "TIME_PORTAL"),
                Arguments.of("Ảnh kỷ niệm lưu ở đâu?", "PHOTO_MEMORY"),
                Arguments.of("Gói offline Lite dùng thế nào?", "OFFLINE_LITE"));
    }

    @Test
    void deicticQuestionUsesOnlyAKnownFeatureKey() {
        var context = new ChatPageContext("ARTIFACTS", "/collection", "cu-chi", null);

        var route = resolver.resolve("Cái này dùng để làm gì?", context, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.PRODUCT_HELP);
        assertThat(route.featureKey()).isEqualTo("ARTIFACTS");
        assertThat(route.requiresClarification()).isFalse();
    }

    @Test
    void unknownFeatureDoesNotFallBackToAFeatureLookingRoute() {
        var context = new ChatPageContext("ADMIN_DASHBOARD", "/artifacts", "cu-chi", null);

        var route = resolver.resolve("Cái này dùng để làm gì?", context, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.OUT_OF_SCOPE);
        assertThat(route.scopes()).isEmpty();
        assertThat(route.featureKey()).isNull();
        assertThat(route.requiresClarification()).isTrue();
        assertThat(route.clarificationReason()).isEqualTo("UNKNOWN_FEATURE_CONTEXT");
    }

    @Test
    void invalidRouteCannotSupplyFeatureContext() {
        var context = new ChatPageContext(null, "https://example.com/sites/cu-chi/stations/ST01", "cu-chi", "ST01");

        var route = resolver.resolve("Cái này dùng để làm gì?", context, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.OUT_OF_SCOPE);
        assertThat(route.requiresClarification()).isTrue();
        assertThat(route.clarificationReason()).isEqualTo("INVALID_PAGE_CONTEXT");
    }

    @Test
    void routeTextCannotOverrideExplicitSiteOrStation() {
        var context = new ChatPageContext(
                "STATION_MEDIA", "/sites/cu-chi/stations/ST01/media", "hue", "ST02");

        var route = resolver.resolve("Cái này dùng để làm gì?", context, Map.of());

        assertThat(route.pageContext().siteCode()).isEqualTo("hue");
        assertThat(route.pageContext().stationCode()).isEqualTo("ST02");
    }

    @ParameterizedTest
    @MethodSource("stationOneAliases")
    void bareStationOneWithoutSiteContextIsAmbiguous(String alias) {
        var route = resolver.resolve(alias, null, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.OUT_OF_SCOPE);
        assertThat(route.scopes()).isEmpty();
        assertThat(route.requiresClarification()).isTrue();
        assertThat(route.clarificationReason()).isEqualTo("MISSING_SITE_CONTEXT");
        assertThat(route.pageContext()).isNull();
    }

    private static Stream<String> stationOneAliases() {
        return Stream.of("trạm 1", "Trạm một", "ST1", "st01", "phòng họp");
    }

    @Test
    void bareStationOneUsesCurrentSiteWithoutInventingOne() {
        var context = new ChatPageContext("STATION_MEDIA", "/sites/hue/stations/ST01", "hue", "ST01");

        var route = resolver.resolve("trạm 1", context, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.PRODUCT_HELP);
        assertThat(route.pageContext().siteCode()).isEqualTo("hue");
        assertThat(route.pageContext().stationCode()).isEqualTo("ST01");
        assertThat(route.pageContext().siteCode()).isNotEqualTo("cu-chi");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("intentCases")
    void classifiesKnowledgeIntent(String message, ChatKnowledgeIntent intent,
            Set<ChatKnowledgeRoute.KnowledgeScope> scopes) {
        var route = resolver.resolve(message, null, Map.of("remainingQuests", 2));

        assertThat(route.intent()).isEqualTo(intent);
        assertThat(route.scopes()).isEqualTo(scopes);
    }

    private static Stream<Arguments> intentCases() {
        return Stream.of(
                Arguments.of("Địa đạo được xây dựng vào năm nào?", ChatKnowledgeIntent.HERITAGE_FACT,
                        Set.of(ChatKnowledgeRoute.KnowledgeScope.HERITAGE)),
                Arguments.of("Tôi còn nhiệm vụ nào?", ChatKnowledgeIntent.USER_PROGRESS,
                        Set.of(ChatKnowledgeRoute.KnowledgeScope.USER_STATE)),
                Arguments.of("Tôi đã nhận bao nhiêu huy hiệu?", ChatKnowledgeIntent.USER_PROGRESS,
                        Set.of(ChatKnowledgeRoute.KnowledgeScope.USER_STATE)),
                Arguments.of("Huy hiệu của mình đã đủ chưa?", ChatKnowledgeIntent.USER_PROGRESS,
                        Set.of(ChatKnowledgeRoute.KnowledgeScope.USER_STATE)),
                Arguments.of("Tôi đã check-in trạm này chưa?", ChatKnowledgeIntent.USER_PROGRESS,
                        Set.of(ChatKnowledgeRoute.KnowledgeScope.USER_STATE)),
                Arguments.of("Cách xem phim 3D về trận Cedar Falls diễn ra thế nào?", ChatKnowledgeIntent.MIXED,
                        Set.of(ChatKnowledgeRoute.KnowledgeScope.PLATFORM,
                                ChatKnowledgeRoute.KnowledgeScope.HERITAGE)),
                Arguments.of("Chỉ tôi cách nấu phở", ChatKnowledgeIntent.OUT_OF_SCOPE, Set.of()));
    }

    @Test
    void navigationContextNeverProvesCheckIn() {
        var context = new ChatPageContext(
                "STATION_CHECKIN", "/sites/cu-chi/stations/ST01/check-in", "cu-chi", "ST01");

        var route = resolver.resolve("Tôi đã check-in chưa?", context, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.USER_PROGRESS);
        assertThat(route.scopes()).containsExactly(ChatKnowledgeRoute.KnowledgeScope.USER_STATE);
    }

    @ParameterizedTest
    @MethodSource("invalidContexts")
    void invalidPageContextIsDiscardedRatherThanGuessed(ChatPageContext context) {
        var route = resolver.resolve("Cái này dùng để làm gì?", context, Map.of());

        assertThat(route.intent()).isEqualTo(ChatKnowledgeIntent.OUT_OF_SCOPE);
        assertThat(route.pageContext()).isNull();
        assertThat(route.requiresClarification()).isTrue();
        assertThat(route.clarificationReason()).isEqualTo("INVALID_PAGE_CONTEXT");
    }

    private static Stream<ChatPageContext> invalidContexts() {
        return Stream.of(
                new ChatPageContext("A".repeat(65), "/collection", "cu-chi", null),
                new ChatPageContext("ARTIFACTS", "/" + "a".repeat(256), "cu-chi", null),
                new ChatPageContext("ARTIFACTS", "javascript:alert(1)", "cu-chi", null));
    }
}
