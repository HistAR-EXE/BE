package com.histar.be.quest.progress.service;

import com.histar.be.quest.progress.entity.UserQuestProgress;
import java.util.List;
import java.util.UUID;

public interface UserQuestProgressService {

    List<UserQuestProgress> findAll();

    UserQuestProgress findById(UUID id);

    UserQuestProgress save(UserQuestProgress entity);

    void deleteById(UUID id);

    long count();
}
