package com.histar.be.squad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "squad_members")
@IdClass(SquadMember.SquadMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SquadMember {

    @Id
    @Column(name = "squad_id", nullable = false)
    private UUID squadId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SquadMemberId implements Serializable {
        private UUID squadId;
        private UUID userId;
    }
}
