package com.histar.be.organization.dto;

import com.histar.be.organization.entity.OrgEmailInvitation;
import java.time.Instant;

public record OrgEmailInvitationResponse(String email, String status, String deliveryStatus, Instant expiresAt) {
    public static OrgEmailInvitationResponse from(OrgEmailInvitation invitation) {
        String status = "PENDING".equals(invitation.getStatus()) && !invitation.getExpiresAt().isAfter(Instant.now())
                ? "EXPIRED" : invitation.getStatus();
        return new OrgEmailInvitationResponse(invitation.getEmail(), status,
                invitation.getDeliveryStatus(), invitation.getExpiresAt());
    }
}
