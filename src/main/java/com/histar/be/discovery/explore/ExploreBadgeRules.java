package com.histar.be.discovery.explore;

import java.util.*;

/** The same server rules drive award decisions and the progress displayed by the dashboard. */
public final class ExploreBadgeRules {
    public static final List<String> IDS = List.of("badge_compass_pioneer", "badge_steel_footprint",
            "badge_cipher_master", "badge_relic_detective", "badge_ancestor_soulmate",
            "badge_treasure_keeper", "badge_scholar_mind", "badge_time_envoy", "badge_all_seeing_eye");
    public record Context(int totalPoints, Set<String> keys, boolean chat, boolean artifacts) {}
    public record Progress(String id, boolean eligible, int percent, String text) {}
    private ExploreBadgeRules() {}

    public static UUID databaseId(int index) {
        return UUID.fromString("b0000001-0000-4000-8000-" + String.format("%012d", 201 + index));
    }

    public static List<Progress> evaluate(Context c) {
        Set<String> keys = c.keys();
        int depths = (int) ExploreChallengeCatalog.QUIZZES.keySet().stream()
                .filter(k -> k.startsWith("depth_quiz:") && keys.contains(k)).count();
        int ciphers = (int) ExploreChallengeCatalog.QUIZZES.keySet().stream()
                .filter(k -> k.startsWith("cipher:") && keys.contains(k)).count();
        int dossiers = (int) ExploreChallengeCatalog.QUIZZES.keySet().stream()
                .filter(k -> k.startsWith("dossier:") && keys.contains(k)).count();
        long clues = ExploreChallengeCatalog.CLUES.stream().filter(keys::contains).count();
        boolean conclusion = keys.contains(ExploreChallengeCatalog.CONCLUSION);
        boolean tour = keys.stream().anyMatch(k -> k.startsWith("scene:") || k.equals("tour:tram-xa"));
        boolean diorama = keys.contains("feature:diorama");
        int xp = c.totalPoints();
        return List.of(
                p(0, xp >= 50 || depths >= 1, Math.max(r(xp, 50), r(depths, 1)), xp + "/50 XP hoặc " + depths + "/1 thử thách tầng"),
                p(1, depths == 4, r(depths, 4), depths + "/4 thử thách tầng"),
                p(2, ciphers == 3, r(ciphers, 3), ciphers + "/3 mật thư"),
                p(3, clues == 4 && conclusion, (clues + (conclusion ? 1 : 0)) / 5.0, clues + "/4 chứng cứ · " + (conclusion ? "Đã kết luận" : "Chưa kết luận")),
                p(4, c.chat() && xp >= 300 && dossiers >= 2, ((c.chat() ? 1 : 0) + r(xp, 300) + r(dossiers, 2)) / 3, (c.chat() ? "Đã chat" : "Chưa chat") + " · " + dossiers + "/2 hồ sơ · " + xp + "/300 XP"),
                p(5, c.artifacts() && xp >= 500 && ciphers == 3, ((c.artifacts() ? 1 : 0) + r(xp, 500) + r(ciphers, 3)) / 3, (c.artifacts() ? "Đã mở cổ vật" : "Chưa mở cổ vật") + " · " + ciphers + "/3 mật thư · " + xp + "/500 XP"),
                p(6, dossiers == 6, r(dossiers, 6), dossiers + "/6 hồ sơ"),
                p(7, tour && diorama && xp >= 600, ((tour ? 1 : 0) + (diorama ? 1 : 0) + r(xp, 600)) / 3, "360°: " + tour + " · Sa bàn: " + diorama + " · " + xp + "/600 XP"),
                p(8, xp >= 1130 && tour && diorama && c.chat() && c.artifacts(), (r(xp, 1130) + (tour ? 1 : 0) + (diorama ? 1 : 0) + (c.chat() ? 1 : 0) + (c.artifacts() ? 1 : 0)) / 5, xp + "/1.130 XP · 360°: " + tour + " · Sa bàn: " + diorama + " · Chat: " + c.chat() + " · Cổ vật: " + c.artifacts()));
    }

    private static double r(long value, int target) { return Math.min(1, Math.max(0, value / (double) target)); }
    private static Progress p(int i, boolean eligible, double fraction, String text) {
        return new Progress(IDS.get(i), eligible, eligible ? 100 : Math.min(99, (int) Math.round(fraction * 100)), text);
    }
}
