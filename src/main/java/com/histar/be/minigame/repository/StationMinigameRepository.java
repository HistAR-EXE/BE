package com.histar.be.minigame.repository;

import com.histar.be.minigame.entity.StationMinigame;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationMinigameRepository extends JpaRepository<StationMinigame, UUID> {

    List<StationMinigame> findBySiteCodeAndStationCodeOrderBySortOrderAsc(String siteCode, String stationCode);

    Optional<StationMinigame> findByIdAndSiteCode(UUID id, String siteCode);
}
