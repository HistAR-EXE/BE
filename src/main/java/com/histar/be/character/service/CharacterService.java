package com.histar.be.character.service;

import com.histar.be.character.entity.CharacterEntity;
import java.util.List;
import java.util.UUID;
import java.util.List;

public interface CharacterService {

    List<CharacterEntity> findAll();

    CharacterEntity findById(UUID id);

    CharacterEntity save(CharacterEntity entity);

    void deleteById(UUID id);

    long count();
    List<CharacterEntity> findByLocationId(UUID locationId);
}
