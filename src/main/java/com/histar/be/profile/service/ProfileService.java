package com.histar.be.profile.service;

import com.histar.be.profile.entity.Profile;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ProfileService {

    List<Profile> findAll();

    Profile findById(UUID id);

    Profile save(Profile entity);

    void deleteById(UUID id);

    long count();
    Optional<Profile> findByEmail(String email);
}
