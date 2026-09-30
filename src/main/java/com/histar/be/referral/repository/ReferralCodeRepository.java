package com.histar.be.referral.repository;

import com.histar.be.referral.entity.ReferralCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, UUID> {

    Optional<ReferralCode> findByCodeIgnoreCase(String code);
}
