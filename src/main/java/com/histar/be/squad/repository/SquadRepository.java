package com.histar.be.squad.repository;

import com.histar.be.squad.entity.Squad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadRepository extends JpaRepository<Squad, UUID> {

    Optional<Squad> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
