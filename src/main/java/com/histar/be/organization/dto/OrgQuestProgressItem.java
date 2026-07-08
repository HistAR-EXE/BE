package com.histar.be.organization.dto;

import java.util.UUID;

public record OrgQuestProgressItem(UUID questId, String title, int completionPct) {}
