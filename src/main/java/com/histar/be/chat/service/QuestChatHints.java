package com.histar.be.chat.service;

import java.util.Locale;
import java.util.Set;

/** Exact quest prompts. Curated and FAQ answers for these do not use the daily chat quota. */
public final class QuestChatHints {

    private static final Locale VI = Locale.forLanguageTag("vi");
    private static final Set<String> PROMPTS = Set.of(
            normalize("Khói bếp Hoàng Cầm đi qua đâu trước khi ra mặt đất?"),
            normalize("Nắp hầm Củ Chi được ngụy trang thế nào?"),
            normalize("Ba tầng địa đạo Củ Chi chịu được những gì?"),
            normalize("Chrono ơi, địa đạo Củ Chi có mấy tầng, tầng sâu nhất sâu bao nhiêu?"),
            normalize("Chrono ơi, Bếp Hoàng Cầm dẫn khói theo rãnh và lọc qua đất ẩm như thế nào?"));

    private QuestChatHints() {}

    public static boolean isExempt(String message) {
        return message != null && PROMPTS.contains(normalize(message));
    }

    static String normalize(String message) {
        return message.trim().toLowerCase(VI).replaceAll("\\s+", " ");
    }
}
