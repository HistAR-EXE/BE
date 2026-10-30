package com.histar.be.chat.knowledge;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure, deterministic routing. Retrieval and user-state interpretation happen downstream. */
public final class ChatKnowledgeRouteResolver {

    private static final Set<String> KNOWN_FEATURES = Set.of(
            "OVERVIEW",
            "NAVIGATION",
            "BADGES",
            "LEADERBOARD",
            "EXPLORE",
            "STATION_MEDIA",
            "STATION_CHECKIN",
            "QUESTS",
            "ARTIFACTS",
            "TOUR_360",
            "DIORAMA",
            "HISTORICAL_REENACTMENT",
            "TIME_PORTAL",
            "AR",
            "PHOTO_MEMORY",
            "CHAT_AI",
            "OFFLINE_LITE",
            "TROUBLESHOOTING");

    private static final Set<String> BARE_STATION_ONE = Set.of(
            "tram 1", "tram mot", "st1", "st01", "phong hop");

    private static final Pattern AR_TOKEN = Pattern.compile("(^|[^a-z0-9])ar([^a-z0-9]|$)");

    public ChatKnowledgeRoute resolve(
            String message, ChatPageContext pageContext, Map<String, Object> playerContext) {
        String text = normalize(message);
        boolean suppliedContextInvalid = pageContext != null && !pageContext.isStructurallyValid();
        ChatPageContext context = suppliedContextInvalid ? null : normalize(pageContext);

        if (isBareStationOne(text) && !hasSiteContext(context)) {
            return clarification("MISSING_SITE_CONTEXT");
        }

        if (isUserProgress(text)) {
            return route(ChatKnowledgeIntent.USER_PROGRESS, Set.of(ChatKnowledgeRoute.KnowledgeScope.USER_STATE),
                    null, context);
        }

        String featureKey = productFeature(text);
        boolean heritage = isHeritageQuestion(text, featureKey);
        if (featureKey != null && heritage) {
            return route(ChatKnowledgeIntent.MIXED,
                    Set.of(ChatKnowledgeRoute.KnowledgeScope.PLATFORM,
                            ChatKnowledgeRoute.KnowledgeScope.HERITAGE),
                    featureKey, context);
        }
        if (featureKey != null) {
            return route(ChatKnowledgeIntent.PRODUCT_HELP,
                    Set.of(ChatKnowledgeRoute.KnowledgeScope.PLATFORM), featureKey, context);
        }
        if (heritage) {
            return route(ChatKnowledgeIntent.HERITAGE_FACT,
                    Set.of(ChatKnowledgeRoute.KnowledgeScope.HERITAGE), null, context);
        }

        if (isDeictic(text)) {
            if (suppliedContextInvalid) {
                return clarification("INVALID_PAGE_CONTEXT");
            }
            String contextFeature = context == null ? null : context.featureKey();
            if (contextFeature == null) {
                return clarification("MISSING_FEATURE_CONTEXT");
            }
            if (!KNOWN_FEATURES.contains(contextFeature)) {
                return clarification("UNKNOWN_FEATURE_CONTEXT");
            }
            return route(ChatKnowledgeIntent.PRODUCT_HELP,
                    Set.of(ChatKnowledgeRoute.KnowledgeScope.PLATFORM), contextFeature, context);
        }

        return route(ChatKnowledgeIntent.OUT_OF_SCOPE, Set.of(), null, context);
    }

    private static ChatKnowledgeRoute route(
            ChatKnowledgeIntent intent,
            Set<ChatKnowledgeRoute.KnowledgeScope> scopes,
            String featureKey,
            ChatPageContext context) {
        return new ChatKnowledgeRoute(intent, scopes, featureKey, context, false, null);
    }

    private static ChatKnowledgeRoute clarification(String reason) {
        return new ChatKnowledgeRoute(
                ChatKnowledgeIntent.OUT_OF_SCOPE, Set.of(), null, null, true, reason);
    }

    private static String productFeature(String text) {
        if (containsAny(text, "huy hieu", "huy nghiep", "badge", "la ban khoi nguyen", "dau chan dat thep",
                "bac thay co yeu", "tham tu su lieu", "tri ky tien nhan", "nguoi giu kho bau",
                "tinh hoa bac hoc", "su gia thoi khong", "nhan quan xuyen thau")) {
            return "BADGES";
        }
        if (containsAny(text, "bang xep hang", "bang vang", "vinh danh", "leaderboard", "top 10", "thu hang")) {
            return "LEADERBOARD";
        }
        if (containsAny(text, "cong thoi gian", "time portal", "xua va nay", "xua & nay", "xua nay", "lat cat 3d")) {
            return "TIME_PORTAL";
        }
        if (containsAny(text, "hoa than", "nhap vai", "anh va chia se", "anh & chia se", "anh goc",
                "anh ky niem", "photo memory", "khung anh", "luu anh", "chia se anh")) {
            return "PHOTO_MEMORY";
        }
        if (containsAny(text, "mat thu", "ho so dat thep", "manh chung cu", "tang do sau", "thu vien video")) {
            return "EXPLORE";
        }
        if (containsAny(text, "bao tang", "co vat", "hien vat", "bo suu tap")) {
            return "ARTIFACTS";
        }
        if (containsAny(text, "tro ly ai", "tro ly lich su ai", "chrono", "chat ai")) {
            return "CHAT_AI";
        }
        if (containsAny(text, "video 3d", "phim 3d", "tai hien lich su")) {
            return "HISTORICAL_REENACTMENT";
        }
        if (containsAny(text, "tour 360", "tham quan 360", "360 do")) {
            return "TOUR_360";
        }
        if (containsAny(text, "nhiem vu", "quest", "thu thach")) {
            return "QUESTS";
        }
        if (containsAny(text, "sa ban", "mo hinh tran dia")) {
            return "DIORAMA";
        }
        if (AR_TOKEN.matcher(text).find()) {
            return "AR";
        }
        if (containsAny(text, "quet ma", " ma qr", "qr ", "qr?", "check-in", "check in", "checkin")) {
            return "STATION_CHECKIN";
        }
        if (containsAny(text, "cong thoi gian", "time portal")) {
            return "TIME_PORTAL";
        }
        if (containsAny(text, "anh ky niem", "photo memory", "khung anh")) {
            return "PHOTO_MEMORY";
        }
        if (containsAny(text, "offline lite", "goi lite", "lite offline", "ngoai tuyen")) {
            return "OFFLINE_LITE";
        }
        if (containsAny(text, "kham pha dia diem", "kham pha", "explore")) {
            return "EXPLORE";
        }
        if (containsAny(text, "tram ", "cac tram", "st01", "st1", "phong hop")) {
            return "STATION_MEDIA";
        }
        if (containsAny(text, "timelens", "time lens", "histar", "he sinh thai", "cac tab", "10 tab", "cac tinh nang")) {
            return "OVERVIEW";
        }
        return null;
    }

    private static boolean isUserProgress(String text) {
        boolean personal = containsAny(text, "cua toi", "cua minh")
                || Pattern.compile("(^|\\s)(toi|minh)\\s+(da|dang|con|chua|co|duoc|can|dat|nhan|mo khoa|xep)\\b")
                        .matcher(text).find();
        boolean stateQuestion = containsAny(text,
                "con ", "da ", "chua", "bao nhieu", "tien do", "mo khoa", "hoan thanh");
        boolean stateSubject = containsAny(text,
                "nhiem vu", "quest", "co vat", "hien vat", "bo suu tap",
                "check-in", "check in", "checkin", "tram", "mo khoa", "huy hieu", "huy nghiep", "thu hang", "xp");
        return personal && stateQuestion && stateSubject;
    }

    private static boolean isHeritageQuestion(String text, String featureKey) {
        String evidenceText = text;
        if ("TIME_PORTAL".equals(featureKey) && containsAny(text, "co may", "bao nhieu", "gom", "co gi")) {
            evidenceText = evidenceText.replace("thoi ky", "").replace("giai doan", "");
        }
        if ("CHAT_AI".equals(featureKey)) {
            evidenceText = evidenceText.replace("tro ly lich su ai", "");
        }
        if ("HISTORICAL_REENACTMENT".equals(featureKey)) {
            evidenceText = evidenceText
                    .replace("tai hien lich su", "")
                    .replace("video 3d", "")
                    .replace("phim 3d", "");
        }
        if ("DIORAMA".equals(featureKey)) {
            evidenceText = evidenceText
                    .replace("mo hinh tran dia", "")
                    .replace("sa ban", "");
        }
        return containsAny(evidenceText,
                "lich su", "di tich", "dia dao", "xay dung", "nam nao", "thoi ky",
                "su kien", "nhan vat", "cedar falls", "tran ", "chien dich", "ai la");
    }

    private static boolean isDeictic(String text) {
        return containsAny(text, "cai nay", "tinh nang nay", "trang nay", "cho nay");
    }

    private static boolean isBareStationOne(String text) {
        String stripped = text.replaceAll("[?!.,;:]+$", "").trim();
        return BARE_STATION_ONE.contains(stripped);
    }

    private static boolean hasSiteContext(ChatPageContext context) {
        return context != null && context.siteCode() != null;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static ChatPageContext normalize(ChatPageContext context) {
        return context == null ? null : context.normalized();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return decomposed.replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
