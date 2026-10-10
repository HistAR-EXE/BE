package com.histar.be.chat.guided;

public record GuidedSafetyDecision(boolean blocked, String safeReply) {

    public static GuidedSafetyDecision allow() {
        return new GuidedSafetyDecision(false, null);
    }

    public static GuidedSafetyDecision block(String safeReply) {
        return new GuidedSafetyDecision(true, safeReply);
    }
}
