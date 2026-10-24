package com.histar.be.siteworkspace.repository;

import com.histar.be.siteworkspace.entity.SiteWorkspaceSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteWorkspaceSettingsRepository extends JpaRepository<SiteWorkspaceSettings, UUID> {

    Optional<SiteWorkspaceSettings> findBySiteCodeIgnoreCase(String siteCode);
}
