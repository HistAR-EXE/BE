package com.histar.be.character.repository;

import com.histar.be.character.entity.CharacterEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<CharacterEntity, UUID> {

    List<CharacterEntity> findByLocationId(UUID locationId);

    List<CharacterEntity> findByLocationIdAndChatEnabledTrueOrderByNameAsc(UUID locationId);

    java.util.Optional<CharacterEntity> findByIdAndChatEnabledTrue(UUID id);

}
