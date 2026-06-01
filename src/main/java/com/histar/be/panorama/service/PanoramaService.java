package com.histar.be.panorama.service;

import com.histar.be.panorama.entity.Panorama;
import java.util.List;
import java.util.UUID;


public interface PanoramaService {

    List<Panorama> findAll();

    Panorama findById(UUID id);

    Panorama save(Panorama entity);

    void deleteById(UUID id);

    long count();
}
