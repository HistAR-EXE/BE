package com.histar.be.discovery.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_discoveries")
@IdClass(UserDiscoveryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDiscovery {

    @Id
    private UUID userId;

    @Id
    private UUID locationId;

    @Id
    private String discoveryKey;

    private Instant discoveredAt;
}
