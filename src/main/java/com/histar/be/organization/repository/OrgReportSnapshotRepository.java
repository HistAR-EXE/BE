package com.histar.be.organization.repository;

import com.histar.be.organization.entity.OrgReportSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgReportSnapshotRepository extends JpaRepository<OrgReportSnapshot, UUID> {
    List<OrgReportSnapshot> findTop50ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
