package com.histar.be.billing.repository;

import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HeritageDigitizationInquiryRepository extends JpaRepository<HeritageDigitizationInquiry, UUID>,
        JpaSpecificationExecutor<HeritageDigitizationInquiry> {}
