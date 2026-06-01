package com.histar.be.quest.repository;

import com.histar.be.quest.entity.Quest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRepository extends JpaRepository<Quest, UUID> {

    List<Quest> findByLocationId(UUID locationId);
}
