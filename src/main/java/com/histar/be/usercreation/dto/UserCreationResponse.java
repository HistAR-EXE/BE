package com.histar.be.usercreation.dto;

import com.histar.be.usercreation.entity.UserCreation;
import java.time.Instant;
import java.util.UUID;

public record UserCreationResponse(
        UUID id, UUID frameId, String outputUrl, String variant, Instant createdAt, boolean shared) {

    public static UserCreationResponse from(UserCreation creation) {
        return new UserCreationResponse(
                creation.getId(),
                creation.getFrameId(),
                creation.getOutputUrl(),
                creation.getVariant(),
                creation.getCreatedAt(),
                creation.getSharedAt() != null);
    }
}
