package com.histar.be.quest.narrative;
import com.histar.be.quest.narrative.entity.UserNarrativeInvestigationProgress;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
public interface UserNarrativeInvestigationProgressRepository extends JpaRepository<UserNarrativeInvestigationProgress, UUID> {
 Optional<UserNarrativeInvestigationProgress> findByUserIdAndInvestigationId(UUID userId, UUID investigationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 Optional<UserNarrativeInvestigationProgress> findWithLockByUserIdAndInvestigationId(UUID userId, UUID investigationId);
}
