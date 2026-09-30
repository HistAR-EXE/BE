package com.histar.be.stations.repository;

import com.histar.be.stations.entity.Station;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, UUID> {

    List<Station> findBySiteCodeAndActiveTrueOrderBySortOrderAsc(String siteCode);

    Optional<Station> findBySiteCodeAndCodeAndActiveTrue(String siteCode, String code);

    Optional<Station> findBySiteCodeAndCode(String siteCode, String code);

    /** Lookup by code only (station codes are unique per site; used by presence sequence scoring). */
    Optional<Station> findFirstByCodeIgnoreCaseAndActiveTrueOrderBySortOrderAsc(String code);
}
