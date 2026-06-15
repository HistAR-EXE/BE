package com.histar.be.gamification.repository;

import com.histar.be.gamification.entity.UserHeritageOnsiteBonus;
import com.histar.be.gamification.entity.UserHeritageOnsiteBonusId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHeritageOnsiteBonusRepository
        extends JpaRepository<UserHeritageOnsiteBonus, UserHeritageOnsiteBonusId> {

    boolean existsByIdUserIdAndIdLocationId(UUID userId, UUID locationId);
}
