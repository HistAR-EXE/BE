package com.histar.be.checkin.repository;

import com.histar.be.checkin.entity.Checkin;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {
}
