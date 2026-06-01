package com.histar.be.usercreation.service;

import com.histar.be.usercreation.entity.UserCreation;
import java.util.List;
import java.util.UUID;


public interface UserCreationService {

    List<UserCreation> findAll();

    UserCreation findById(UUID id);

    UserCreation save(UserCreation entity);

    void deleteById(UUID id);

    long count();
}
