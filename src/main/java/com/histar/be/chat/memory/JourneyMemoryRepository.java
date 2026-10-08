package com.histar.be.chat.memory;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JourneyMemoryRepository extends JpaRepository<JourneyMemory, UUID> {

    Optional<JourneyMemory> findByUserIdAndLocationId(UUID userId, UUID locationId);

    void deleteByUserIdAndLocationId(UUID userId, UUID locationId);

    void deleteByUserId(UUID userId);
}
