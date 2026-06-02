package com.histar.be.photoframe.repository;

import com.histar.be.photoframe.entity.PhotoFrame;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoFrameRepository extends JpaRepository<PhotoFrame, UUID> {

    java.util.List<PhotoFrame> findAllByOrderBySortOrderAsc();
}
