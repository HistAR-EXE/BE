package com.histar.be.billing.repository;

import com.histar.be.billing.entity.BillingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingSettingRepository extends JpaRepository<BillingSetting, String> {}
