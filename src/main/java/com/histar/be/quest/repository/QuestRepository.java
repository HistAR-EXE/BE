package com.histar.be.quest.repository;

import com.histar.be.quest.entity.Quest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface QuestRepository extends JpaRepository<Quest, UUID> {

    List<Quest> findByLocationIdAndDeletedAtIsNull(UUID locationId);

    default List<Quest> findByLocationId(UUID locationId) {
        return findByLocationIdAndDeletedAtIsNull(locationId);
    }

    @Override
    @Query("select q from Quest q where q.id = :id and q.deletedAt is null")
    Optional<Quest> findById(@Param("id") UUID id);

    @Query("select q from Quest q where q.id = :id")
    Optional<Quest> findIncludingDeletedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quest q where q.id = :id")
    Optional<Quest> findIncludingDeletedByIdForUpdate(@Param("id") UUID id);

    List<Quest> findByDeletedAtIsNull();

    List<Quest> findByDeletedAtIsNotNull();

    List<Quest> findByUnlockAfterQuestIdAndDeletedAtIsNull(UUID questId);

    List<Quest> findByUnlockAfterQuestId(UUID questId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quest q where q.unlockAfterQuestId = :questId and q.deletedAt is null order by q.id")
    List<Quest> findActiveChildrenForUpdate(@Param("questId") UUID questId);

    long countByDeletedAtIsNotNull();

    long countByDeletedAtIsNull();

    List<Quest> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    @Override
    @Query("select (count(q) > 0) from Quest q where q.id = :id and q.deletedAt is null")
    boolean existsById(@Param("id") UUID id);
}
