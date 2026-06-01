package com.histar.be.character.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.character.entity.CharacterEntity;
import com.histar.be.character.repository.CharacterRepository;
import com.histar.be.character.service.CharacterService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharacterServiceImpl implements CharacterService {

    private final CharacterRepository repository;

    @Override
    public List<CharacterEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public CharacterEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CharacterEntity not found: " + id));
    }

    @Override
    public CharacterEntity save(CharacterEntity entity) {
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
    public List<CharacterEntity> findByLocationId(UUID locationId) {
        return repository.findByLocationId(locationId);
    }

}
