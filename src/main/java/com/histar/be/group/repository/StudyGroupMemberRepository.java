package com.histar.be.group.repository;

import com.histar.be.group.entity.StudyGroupMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, UUID> {

    List<StudyGroupMember> findByUserId(UUID userId);

    List<StudyGroupMember> findByGroupId(UUID groupId);

    Optional<StudyGroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    long countByUserId(UUID userId);
}
