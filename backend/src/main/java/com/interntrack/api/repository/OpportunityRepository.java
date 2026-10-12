package com.interntrack.api.repository;

import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.OpportunityStatus;
import com.interntrack.api.entity.enums.SourceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    Optional<Opportunity> findBySourceProviderAndExternalSourceId(SourceProvider sourceProvider, String externalSourceId);

    Optional<Opportunity> findByRawHash(String rawHash);

    List<Opportunity> findByCreatedByUserId(Long userId);

    List<Opportunity> findByStatus(OpportunityStatus status);

    @Query("""
        SELECT o FROM Opportunity o
        LEFT JOIN FETCH o.company
        WHERE (o.createdByUser IS NULL OR o.createdByUser.id = :userId)
          AND (:locationType IS NULL OR o.locationType = :locationType)
          AND (:search IS NULL OR LOWER(o.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.locationCity) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.company.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY o.createdAt DESC
    """)
    List<Opportunity> findAccessibleOpportunities(
            @Param("userId") Long userId,
            @Param("locationType") LocationType locationType,
            @Param("search") String search
    );

    @Query("""
        SELECT o FROM Opportunity o
        LEFT JOIN FETCH o.company
        WHERE o.id = :id
          AND (o.createdByUser IS NULL OR o.createdByUser.id = :userId)
    """)
    Optional<Opportunity> findAccessibleById(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT o FROM Opportunity o LEFT JOIN FETCH o.company WHERE o.id = :id")
    Optional<Opportunity> findByIdWithCompany(@Param("id") Long id);
}
