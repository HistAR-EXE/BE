package com.histar.be.quest.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.QuestService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestServiceImpl implements QuestService {

    private final QuestRepository repository;

    @Override
    public List<Quest> findAll() {
        return repository.findAll();
    }

    @Override
    public Quest findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + id));
    }

    @Override
    public Quest save(Quest entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }
    @Override
    public List<Quest> findByLocationId(UUID locationId) {
        return repository.findByLocationId(locationId);
    }

}
