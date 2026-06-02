package com.histar.be.secret.repository;

import com.histar.be.secret.entity.UserSecretUnlock;
import com.histar.be.secret.entity.UserSecretUnlockId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSecretUnlockRepository extends JpaRepository<UserSecretUnlock, UserSecretUnlockId> {

    boolean existsByUserIdAndLocationId(UUID userId, UUID locationId);
}
