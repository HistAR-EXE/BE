package com.histar.be.character.dto;

import com.histar.be.character.entity.CharacterEntity;
import java.util.UUID;

public record CharacterResponse(UUID id, UUID locationId, String name, String era, String portraitUrl) {

    public static CharacterResponse from(CharacterEntity entity) {
        return new CharacterResponse(
                entity.getId(), entity.getLocationId(), entity.getName(), entity.getEra(), entity.getPortraitUrl());
    }
}
