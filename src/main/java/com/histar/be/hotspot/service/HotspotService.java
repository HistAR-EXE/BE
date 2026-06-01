package com.histar.be.hotspot.service;

import com.histar.be.hotspot.entity.Hotspot;
import java.util.List;
import java.util.UUID;


public interface HotspotService {

    List<Hotspot> findAll();

    Hotspot findById(UUID id);

    Hotspot save(Hotspot entity);

    void deleteById(UUID id);

    long count();
}
