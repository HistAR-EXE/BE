package com.histar.be.checkin.service;

import com.histar.be.checkin.entity.Checkin;
import java.util.List;
import java.util.UUID;


public interface CheckinService {

    List<Checkin> findAll();

    Checkin findById(UUID id);

    Checkin save(Checkin entity);

    void deleteById(UUID id);

    long count();
}
