package com.histar.be.organization.repository;

import com.histar.be.organization.entity.Organization;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByInviteCodeIgnoreCase(String inviteCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Organization o WHERE o.id = :id")
    Optional<Organization> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Plan filter matches either {@code plan} or {@code planType}. Seed data often has
     * plan={@code org_basic} with plan_type={@code MICRO} — both must count as ORG_BASIC.
     */
    @Query(
            """
            SELECT o FROM Organization o
            WHERE (:q = '' OR LOWER(o.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(o.slug, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (
                :plan = ''
                OR UPPER(COALESCE(o.plan, '')) = :plan
                OR UPPER(COALESCE(o.planType, '')) = :plan
                OR (
                  (:plan = 'ORG_BASIC' OR :plan = 'MICRO' OR :plan = 'BASIC')
                  AND (
                    UPPER(COALESCE(o.plan, '')) = 'ORG_BASIC'
                    OR UPPER(COALESCE(o.plan, '')) = 'MICRO'
                    OR UPPER(COALESCE(o.plan, '')) = 'BASIC'
                    OR UPPER(COALESCE(o.plan, '')) = 'TRIAL'
                    OR UPPER(COALESCE(o.planType, '')) = 'ORG_BASIC'
                    OR UPPER(COALESCE(o.planType, '')) = 'MICRO'
                    OR UPPER(COALESCE(o.planType, '')) = 'BASIC'
                  )
                )
                OR (
                  (:plan = 'STANDARD' OR :plan = 'ORG_PRO' OR :plan = 'PRO' OR :plan = 'B2B' OR :plan = 'B2B2C')
                  AND (
                    UPPER(COALESCE(o.plan, '')) = 'STANDARD'
                    OR UPPER(COALESCE(o.plan, '')) = 'ORG_PRO'
                    OR UPPER(COALESCE(o.plan, '')) = 'PRO'
                    OR UPPER(COALESCE(o.plan, '')) = 'B2B'
                    OR UPPER(COALESCE(o.plan, '')) = 'B2B2C'
                    OR UPPER(COALESCE(o.planType, '')) = 'STANDARD'
                    OR UPPER(COALESCE(o.planType, '')) = 'ORG_PRO'
                    OR UPPER(COALESCE(o.planType, '')) = 'PRO'
                    OR UPPER(COALESCE(o.planType, '')) = 'B2B'
                    OR UPPER(COALESCE(o.planType, '')) = 'B2B2C'
                  )
                )
                OR (
                  :plan = 'PREMIUM'
                  AND (
                    UPPER(COALESCE(o.plan, '')) = 'PREMIUM'
                    OR UPPER(COALESCE(o.planType, '')) = 'PREMIUM'
                  )
                )
              )
              AND (:status = '' OR UPPER(COALESCE(o.status, '')) = :status)
            """)
    Page<Organization> findAdminOrganizations(
            @Param("q") String q,
            @Param("plan") String plan,
            @Param("status") String status,
            Pageable pageable);
}
