package com.histar.be.organization.repository;

import com.histar.be.organization.entity.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByInviteCodeIgnoreCase(String inviteCode);
}
