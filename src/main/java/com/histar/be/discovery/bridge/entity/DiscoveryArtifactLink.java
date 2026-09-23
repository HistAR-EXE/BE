package com.histar.be.discovery.bridge.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "discovery_artifact_links")
@IdClass(DiscoveryArtifactLink.DiscoveryArtifactLinkId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryArtifactLink {

    @Id
    private String discoveryUnlockKey;

    @Id
    private String artifactUnlockKey;

    private UUID locationId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class DiscoveryArtifactLinkId implements Serializable {
        private String discoveryUnlockKey;
        private String artifactUnlockKey;
    }
}
