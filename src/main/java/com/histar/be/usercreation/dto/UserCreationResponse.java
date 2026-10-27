package com.histar.be.usercreation.dto;

import com.histar.be.usercreation.entity.UserCreation;
import java.time.Instant;
import java.util.UUID;

public record UserCreationResponse(
        UUID id, UUID frameId, String outputUrl, String variant, Instant createdAt, boolean shared,
        String kind, String siteCode, String stationCode, String roleId, Integer era, String source) {

    public static UserCreationResponse from(UserCreation creation) {
        return new UserCreationResponse(
                creation.getId(),
                creation.getFrameId(),
                creation.getOutputUrl(),
                creation.getVariant(),
                creation.getCreatedAt(),
                creation.getSharedAt() != null,
                creation.getKind(), creation.getSiteCode(), creation.getStationCode(), creation.getRoleId(), creation.getEra(), creation.getSource());
    }
}
