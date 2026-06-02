package com.histar.be.common.gamification;

import java.util.UUID;

public record QrPayload(QrPayloadType type, UUID locationId) {}
