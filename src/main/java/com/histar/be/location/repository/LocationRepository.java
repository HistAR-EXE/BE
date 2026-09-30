package com.histar.be.location.repository;

import com.histar.be.location.entity.Location;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LocationRepository extends JpaRepository<Location, UUID>, JpaSpecificationExecutor<Location> {

    List<Location> findByUnlockPrerequisiteQuestIdIn(List<UUID> questIds);

    Optional<Location> findBySiteCodeIgnoreCase(String siteCode);

    List<Location> findBySiteCodeIsNotNullOrderByNameAsc();
}
