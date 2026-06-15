package com.histar.be.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserHeritageOnsiteBonusId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "location_id")
    private UUID locationId;
}
