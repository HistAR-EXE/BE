package com.histar.be.chat.service;

import java.util.Map;
import java.util.UUID;

public interface PlayerStoryContextService {

    Map<String, Object> build(UUID userId, UUID locationId);

    Map<String, Object> build(UUID userId, UUID locationId, String currentUnlockKey);
}
