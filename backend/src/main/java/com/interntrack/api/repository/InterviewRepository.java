package com.interntrack.api.repository;

import com.interntrack.api.entity.Interview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationId(Long applicationId);

    List<Interview> findByApplicationIdOrderByScheduledAtAsc(Long applicationId);

    @Query("SELECT i FROM Interview i LEFT JOIN FETCH i.application a LEFT JOIN FETCH a.user u WHERE i.id = :id")
    Optional<Interview> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT i FROM Interview i JOIN FETCH i.application a JOIN FETCH a.opportunity o JOIN FETCH o.company c " +
           "WHERE a.user.id = :userId AND i.scheduledAt >= :now AND i.outcome = com.interntrack.api.entity.enums.InterviewOutcome.PENDING " +
           "ORDER BY i.scheduledAt ASC")
    List<Interview> findUpcomingByUserId(@Param("userId") Long userId, @Param("now") Instant now, Pageable pageable);
}