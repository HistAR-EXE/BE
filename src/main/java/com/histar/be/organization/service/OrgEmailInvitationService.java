package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.dto.OrgEmailInvitationResponse;
import com.histar.be.organization.entity.OrgEmailInvitation;
import com.histar.be.organization.repository.OrgEmailInvitationRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class OrgEmailInvitationService {
    private final OrgEmailInvitationRepository invitations;
    private final OrganizationRepository organizations;
    private final OrgMembershipService memberships;
    private final ProfileRepository profiles;
    private final OrganizationMemberRepository members;
    private final EntityManager entityManager;

    @Transactional
    public OrgEmailInvitationResponse record(UUID orgId, UUID inviterId, String email, String deliveryStatus) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        // Sender locks the organization before sending, serializing duplicate sends.
        OrgEmailInvitation invite = invitations.findByOrganizationIdAndEmail(orgId, normalized)
                .orElseGet(() -> OrgEmailInvitation.builder().organizationId(orgId).email(normalized).build());
        invite.setInvitedBy(inviterId);
        invite.setCreatedAt(Instant.now());
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invite.setAcceptedAt(null);
        invite.setStatus("PENDING");
        invite.setDeliveryStatus(deliveryStatus);
        return OrgEmailInvitationResponse.from(invitations.save(invite));
    }

    @Transactional(readOnly = true)
    public List<OrgEmailInvitationResponse> list(UUID orgId) {
        return invitations.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(OrgEmailInvitationResponse::from).toList();
    }

    @Transactional
    public void acceptPending(Profile profile) {
        if (!Boolean.TRUE.equals(profile.getEmailVerified()) || profile.getEmail() == null) return;
        // Google account linking may just have verified this managed profile.
        entityManager.flush();
        var current = profiles.findByIdForUpdate(profile.getId()).orElseThrow();
        entityManager.refresh(current, LockModeType.PESSIMISTIC_WRITE);
        if (!Boolean.TRUE.equals(current.getEmailVerified())) return;
        List<OrgEmailInvitation> pending = invitations.findPendingForEmail(profile.getEmail().trim().toLowerCase(Locale.ROOT));
        Instant now = Instant.now();
        var eligible = pending.stream().filter(i -> "PENDING".equals(i.getStatus()))
                .filter(i -> i.getExpiresAt().isAfter(now))
                .filter(i -> current.getOrgId() == null || current.getOrgId().equals(i.getOrganizationId()))
                .filter(i -> organizations.findById(i.getOrganizationId())
                        .map(OrganizationEntitlementService::isActive).orElse(false)).toList();
        if (eligible.size() > 1) {
            // Login must remain available; the user can choose an organization via its invitation link.
            return;
        }
        for (OrgEmailInvitation invitation : eligible) {
            var org = organizations.findByIdForUpdate(invitation.getOrganizationId()).orElseThrow();
            entityManager.refresh(org, LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(invitation, LockModeType.PESSIMISTIC_WRITE);
            if (!OrganizationEntitlementService.isActive(org) || !"PENDING".equals(invitation.getStatus())
                    || !invitation.getExpiresAt().isAfter(Instant.now())) continue;
            boolean alreadyMember = members.findByOrganizationIdAndUserId(org.getId(), current.getId()).isPresent();
            int limit = org.getMaxVerifiedAccounts() == null ? 100 : org.getMaxVerifiedAccounts();
            if (!alreadyMember && members.countByOrganizationId(org.getId()) >= limit) continue;
            memberships.enrollUserInOrganization(profile.getId(), invitation.getOrganizationId());
            profile.setOrgId(org.getId());
            profile.setOrgSubscription(OrgSubscription.fromPlanType(
                    org.getPlanType() != null ? org.getPlanType() : org.getPlan()).name());
            if ("USER".equalsIgnoreCase(profile.getRole())) profile.setRole("ORG_MEMBER");
            invitation.setStatus("ACCEPTED");
            invitation.setAcceptedAt(now);
            invitations.save(invitation);
        }
    }
}
