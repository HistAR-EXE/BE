package com.histar.be.profile.repository;

import com.histar.be.profile.entity.XpLedgerEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XpLedgerRepository extends JpaRepository<XpLedgerEntry, UUID> {
    boolean existsByUserIdAndEventKey(UUID userId, String eventKey);
}
