package com.histar.be.squad.repository;

import com.histar.be.squad.entity.SquadMember;
import com.histar.be.squad.entity.SquadMember.SquadMemberId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SquadMemberRepository extends JpaRepository<SquadMember, SquadMemberId> {

    List<SquadMember> findBySquadIdOrderByJoinedAtAsc(UUID squadId);

    boolean existsBySquadIdAndUserId(UUID squadId, UUID userId);

    Optional<SquadMember> findFirstByUserIdOrderByJoinedAtDesc(UUID userId);

    int countBySquadId(UUID squadId);
}
