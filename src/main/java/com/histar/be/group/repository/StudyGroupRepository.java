package com.histar.be.group.repository;

import com.histar.be.group.entity.StudyGroup;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, UUID> {

    Optional<StudyGroup> findByCodeIgnoreCase(String code);
    List<StudyGroup> findByOrgIdAndQuestId(UUID orgId, UUID questId);
    List<StudyGroup> findByFieldTripId(UUID fieldTripId);
    long countByQuestIdAndExpiresAtGreaterThan(UUID questId, Instant now);
}
