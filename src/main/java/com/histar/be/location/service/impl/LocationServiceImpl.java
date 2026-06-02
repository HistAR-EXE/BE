package com.histar.be.location.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.gamification.GeoUtils;
import com.histar.be.location.dto.LocationResponse;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.location.service.LocationService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository repository;

    @Override
    public List<Location> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<LocationResponse> search(
            String city, String search, Double nearLat, Double nearLng, Double maxDistanceKm, Pageable pageable) {
        Specification<Location> spec = (root, query, cb) -> cb.conjunction();
        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
        }
        if (search != null && !search.isBlank()) {
            String keyword = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword)));
        }

        boolean hasNearby = nearLat != null && nearLng != null;
        if (!hasNearby) {
            return repository.findAll(spec, pageable).map(LocationResponse::from);
        }

        List<LocationResponse> filtered = repository.findAll(spec, pageable.getSort()).stream()
                .map(location -> {
                    Double distanceKm = null;
                    if (location.getLatitude() != null && location.getLongitude() != null) {
                        distanceKm = GeoUtils.distanceMeters(
                                        nearLat, nearLng, location.getLatitude(), location.getLongitude())
                                / 1000.0;
                    }
                    return LocationResponse.from(location, distanceKm);
                })
                .filter(item -> maxDistanceKm == null
                        || item.distanceKm() == null
                        || item.distanceKm() <= maxDistanceKm)
                .toList();

        int start = Math.min((int) pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Override
    public Location findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
    }

    @Override
    public Location save(Location entity) {
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
}
