package com.histar.be.quest.v2.dto;

import java.util.UUID;

/** Nhiệm vụ cần hoàn thành trước. {@code status}: NOT_STARTED · IN_PROGRESS · COMPLETED. */
public record QuestV2QuestRef(UUID id, String title, String status) {}
