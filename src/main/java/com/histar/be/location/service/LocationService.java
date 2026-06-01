package com.histar.be.location.service;

import com.histar.be.location.entity.Location;
import java.util.List;
import java.util.UUID;


public interface LocationService {

    List<Location> findAll();

    Location findById(UUID id);

    Location save(Location entity);

    void deleteById(UUID id);

    long count();
}
