package com.histar.be.quest.service;

import java.util.UUID;

public interface QuestStepProgressService {

    /** Advance quest step when user records a matching discovery key. */
    void onDiscoveryRecorded(UUID userId, String unlockKey);
}
