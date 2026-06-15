package com.histar.be.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_heritage_onsite_bonus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHeritageOnsiteBonus {

    @EmbeddedId
    private UserHeritageOnsiteBonusId id;

    @Column(name = "xp_awarded")
    private Integer xpAwarded;

    @Column(name = "awarded_at")
    private Instant awardedAt;
}
