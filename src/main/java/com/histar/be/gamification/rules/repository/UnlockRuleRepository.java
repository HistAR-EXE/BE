package com.histar.be.gamification.rules.repository;

import com.histar.be.gamification.rules.entity.UnlockRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnlockRuleRepository extends JpaRepository<UnlockRule, UUID> {

    List<UnlockRule> findByLocationIdAndTriggerTypeAndEnabledTrueOrderBySortOrderAsc(
            UUID locationId, String triggerType);
}
