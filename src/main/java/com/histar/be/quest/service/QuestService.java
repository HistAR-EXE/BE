package com.histar.be.quest.service;

import com.histar.be.quest.entity.Quest;
import java.util.List;
import java.util.UUID;
import java.util.List;

public interface QuestService {

    List<Quest> findAll();

    Quest findById(UUID id);

    Quest save(Quest entity);

    void deleteById(UUID id);

    long count();
    List<Quest> findByLocationId(UUID locationId);
}
