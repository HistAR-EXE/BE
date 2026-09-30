package com.histar.be.stations.repository;

import com.histar.be.stations.entity.StationContentBlock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationContentBlockRepository extends JpaRepository<StationContentBlock, UUID> {

    List<StationContentBlock> findByStationIdInOrderBySortOrderAsc(Collection<UUID> stationIds);

    List<StationContentBlock> findByStationIdOrderBySortOrderAsc(UUID stationId);

    void deleteByStationId(UUID stationId);
}
