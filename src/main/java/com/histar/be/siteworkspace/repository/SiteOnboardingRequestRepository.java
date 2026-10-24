package com.histar.be.siteworkspace.repository;

import com.histar.be.siteworkspace.entity.SiteOnboardingRequest;
import com.histar.be.siteworkspace.entity.SiteOnboardingStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteOnboardingRequestRepository extends JpaRepository<SiteOnboardingRequest, UUID> {

    List<SiteOnboardingRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<SiteOnboardingRequest> findByStatusOrderByCreatedAtAsc(SiteOnboardingStatus status);

    Optional<SiteOnboardingRequest> findByRequesterIdAndSiteCodeIgnoreCaseAndStatus(
            UUID requesterId, String siteCode, SiteOnboardingStatus status);
}
