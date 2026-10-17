package com.histar.be.organization.repository;

import com.histar.be.organization.entity.OrganizationCertificate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationCertificateRepository extends JpaRepository<OrganizationCertificate, UUID> {

    List<OrganizationCertificate> findByOrganizationIdOrderByIssuedAtDesc(UUID organizationId);
}
