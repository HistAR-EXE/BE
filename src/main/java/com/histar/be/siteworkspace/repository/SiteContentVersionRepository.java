package com.histar.be.siteworkspace.repository;

import com.histar.be.siteworkspace.entity.ContentLifecycleStatus;
import com.histar.be.siteworkspace.entity.SiteContentKind;
import com.histar.be.siteworkspace.entity.SiteContentVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteContentVersionRepository extends JpaRepository<SiteContentVersion, UUID> {

    Optional<SiteContentVersion> findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndVersion(
            String siteCode, SiteContentKind contentKind, String resourceKey, int version);

    Optional<SiteContentVersion> findFirstBySiteCodeIgnoreCaseAndContentKindAndResourceKeyOrderByVersionDesc(
            String siteCode, SiteContentKind contentKind, String resourceKey);

    List<SiteContentVersion> findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
            String siteCode,
            SiteContentKind contentKind,
            String resourceKey,
            ContentLifecycleStatus status);
}
