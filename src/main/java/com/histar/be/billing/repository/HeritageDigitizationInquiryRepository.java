package com.histar.be.billing.repository;

import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HeritageDigitizationInquiryRepository extends JpaRepository<HeritageDigitizationInquiry, UUID>,
        JpaSpecificationExecutor<HeritageDigitizationInquiry> {

    List<HeritageDigitizationInquiry> findByOrganizationId(UUID organizationId);

    @Query(
            """
            SELECT i FROM HeritageDigitizationInquiry i
            WHERE (:email <> '' AND LOWER(i.contactEmail) = LOWER(:email))
               OR (:name <> '' AND LOWER(i.siteName) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    List<HeritageDigitizationInquiry> findCrmMatchCandidates(
            @Param("email") String email, @Param("name") String name);
}
