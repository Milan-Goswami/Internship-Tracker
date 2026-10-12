package com.interntrack.api.repository;

import com.interntrack.api.dto.analytics.FunnelStageCounts;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserId(Long userId);

    Optional<Application> findByUserIdAndOpportunityId(Long userId, Long opportunityId);

    boolean existsByUserIdAndOpportunityId(Long userId, Long opportunityId);

    List<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.opportunity o LEFT JOIN FETCH o.company c WHERE a.user.id = :userId AND (:status IS NULL OR a.status = :status) ORDER BY a.updatedAt DESC")
    List<Application> findByUserIdAndOptionalStatusWithDetails(
            @Param("userId") Long userId,
            @Param("status") ApplicationStatus status
    );

    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.opportunity o LEFT JOIN FETCH o.company c WHERE a.id = :id")
    Optional<Application> findByIdWithDetails(@Param("id") Long id);

    long countByUserId(Long userId);

    long countByUserIdAndStatusIn(Long userId, Collection<ApplicationStatus> statuses);

    long countByUserIdAndSavedAtIsNotNull(Long userId);

    long countByUserIdAndAppliedAtIsNotNull(Long userId);

    long countByUserIdAndScreeningAtIsNotNull(Long userId);

    long countByUserIdAndInterviewingAtIsNotNull(Long userId);

    @Query("SELECT a.status, COUNT(a) FROM Application a WHERE a.user.id = :userId GROUP BY a.status")
    List<Object[]> countGroupByStatusForUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT a.opportunity.id FROM Application a WHERE a.user.id = :userId")
    List<Long> findDistinctOpportunityIdsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT new com.interntrack.api.dto.analytics.FunnelStageCounts(
            COUNT(CASE WHEN a.savedAt IS NOT NULL THEN 1L END),
            COUNT(CASE WHEN a.appliedAt IS NOT NULL THEN 1L END),
            COUNT(CASE WHEN a.screeningAt IS NOT NULL THEN 1L END),
            COUNT(CASE WHEN a.interviewingAt IS NOT NULL THEN 1L END),
            COUNT(CASE WHEN a.status IN :offerStatuses THEN 1L END)
        )
        FROM Application a
        WHERE a.user.id = :userId
    """)
    FunnelStageCounts getFunnelStageCounts(
            @Param("userId") Long userId,
            @Param("offerStatuses") Collection<ApplicationStatus> offerStatuses
    );
}