package com.histar.be.siteworkspace.repository;

import com.histar.be.siteworkspace.entity.SiteOnboardingRequest;
import com.histar.be.siteworkspace.entity.SiteOnboardingStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteOnboardingRequestRepository extends JpaRepository<SiteOnboardingRequest, UUID> {

    List<SiteOnboardingRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<SiteOnboardingRequest> findByStatusOrderByCreatedAtAsc(SiteOnboardingStatus status);

    Optional<SiteOnboardingRequest> findByRequesterIdAndSiteCodeIgnoreCaseAndStatus(
            UUID requesterId, String siteCode, SiteOnboardingStatus status);

    boolean existsByRequesterIdAndSiteCodeIgnoreCaseAndStatus(
            UUID requesterId, String siteCode, SiteOnboardingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SiteOnboardingRequest r WHERE r.id = :id")
    Optional<SiteOnboardingRequest> findByIdForUpdate(@Param("id") UUID id);
}
