package com.histar.be.siteworkspace.repository;

import com.histar.be.siteworkspace.entity.SiteMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteMembershipRepository extends JpaRepository<SiteMembership, UUID> {

    List<SiteMembership> findByUserIdAndActiveTrue(UUID userId);

    List<SiteMembership> findBySiteCodeIgnoreCaseAndActiveTrue(String siteCode);

    Optional<SiteMembership> findBySiteCodeIgnoreCaseAndUserId(String siteCode, UUID userId);
}
