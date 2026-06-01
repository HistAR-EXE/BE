package com.histar.be.hotspot.repository;

import com.histar.be.hotspot.entity.Hotspot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotspotRepository extends JpaRepository<Hotspot, UUID> {
}
