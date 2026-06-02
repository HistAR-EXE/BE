package com.histar.be.location.service;

import com.histar.be.location.dto.LocationResponse;
import com.histar.be.location.entity.Location;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface LocationService {

    List<Location> findAll();

    Page<LocationResponse> search(
            String city,
            String search,
            Double nearLat,
            Double nearLng,
            Double maxDistanceKm,
            Pageable pageable);

    Location findById(UUID id);

    Location save(Location entity);

    void deleteById(UUID id);

    long count();
}
