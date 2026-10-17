package com.histar.be.organization.repository;

import com.histar.be.organization.entity.OrganizationContract;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationContractRepository extends JpaRepository<OrganizationContract, UUID> {

    List<OrganizationContract> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);

    Optional<OrganizationContract> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndContractCodeIgnoreCase(UUID organizationId, String contractCode);
}
