package com.histar.be.group.repository;

import com.histar.be.group.entity.StudyGroup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, UUID> {

    Optional<StudyGroup> findByCodeIgnoreCase(String code);
}
