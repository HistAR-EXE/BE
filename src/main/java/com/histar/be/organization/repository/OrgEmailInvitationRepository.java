package com.histar.be.organization.repository;

import com.histar.be.organization.entity.OrgEmailInvitation;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrgEmailInvitationRepository extends JpaRepository<OrgEmailInvitation, UUID> {
    Optional<OrgEmailInvitation> findByOrganizationIdAndEmail(UUID organizationId, String email);
    List<OrgEmailInvitation> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    @Query("select i from OrgEmailInvitation i where i.email = :email and i.status = 'PENDING' order by i.createdAt")
    List<OrgEmailInvitation> findPendingForEmail(@Param("email") String email);
}
