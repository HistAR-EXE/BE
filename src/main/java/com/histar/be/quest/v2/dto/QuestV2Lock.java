package com.histar.be.quest.v2.dto;

import java.util.List;

/**
 * Lý do nhiệm vụ đang khóa.
 *
 * @param reason AFTER_QUEST · DISCOVERY_KEYS
 */
public record QuestV2Lock(String reason, String message, QuestV2QuestRef afterQuest, List<QuestV2LockKey> keys) {}
