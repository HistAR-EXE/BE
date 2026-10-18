package com.histar.be.organization.repository;

import com.histar.be.organization.entity.OrganizationArCard;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationArCardRepository extends JpaRepository<OrganizationArCard, UUID> {

    List<OrganizationArCard> findByOrganizationIdOrderByIssuedAtDesc(UUID organizationId);

    Optional<OrganizationArCard> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndUserIdAndStatusIgnoreCase(
            UUID organizationId, UUID userId, String status);

    List<OrganizationArCard> findByOrganizationIdAndUserIdInAndStatusIgnoreCase(
            UUID organizationId, Collection<UUID> userIds, String status);
}
