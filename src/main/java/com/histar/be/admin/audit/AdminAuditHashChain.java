package com.histar.be.admin.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/** Deterministic SHA-256 hash chain for admin audit events. */
public final class AdminAuditHashChain {

    public static final String GENESIS_PREV_HASH = "0".repeat(64);

    private AdminAuditHashChain() {}

    public static String canonicalPayload(
            UUID id,
            Instant occurredAt,
            UUID actorId,
            String entityType,
            UUID entityId,
            String action,
            Long oldRowVersion,
            Long newRowVersion,
            String reason,
            String idempotencyKey,
            String metadataJson,
            String prevEventHash) {
        return String.join(
                "|",
                empty(id),
                empty(occurredAt),
                empty(actorId),
                empty(entityType),
                empty(entityId),
                empty(action),
                empty(oldRowVersion),
                empty(newRowVersion),
                empty(reason),
                empty(idempotencyKey),
                empty(metadataJson),
                prevEventHash == null || prevEventHash.isBlank() ? GENESIS_PREV_HASH : prevEventHash.trim());
    }

    public static String computeEventHash(
            UUID id,
            Instant occurredAt,
            UUID actorId,
            String entityType,
            UUID entityId,
            String action,
            Long oldRowVersion,
            Long newRowVersion,
            String reason,
            String idempotencyKey,
            String metadataJson,
            String prevEventHash) {
        String canonical = canonicalPayload(
                id,
                occurredAt,
                actorId,
                entityType,
                entityId,
                action,
                oldRowVersion,
                newRowVersion,
                reason,
                idempotencyKey,
                metadataJson,
                prevEventHash);
        return sha256Hex(canonical);
    }

    public static String computeEventHash(AdminAuditEvent event, String prevEventHash) {
        return computeEventHash(
                event.getId(),
                event.getOccurredAt(),
                event.getActorId(),
                event.getEntityType(),
                event.getEntityId(),
                event.getAction(),
                event.getOldRowVersion(),
                event.getNewRowVersion(),
                event.getReason(),
                event.getIdempotencyKey(),
                event.getMetadataJson(),
                prevEventHash);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String empty(Object value) {
        return value == null ? "" : value.toString();
    }
}
