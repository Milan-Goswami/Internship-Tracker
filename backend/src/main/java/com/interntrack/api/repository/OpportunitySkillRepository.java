package com.interntrack.api.repository;

import com.interntrack.api.entity.OpportunitySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OpportunitySkillRepository extends JpaRepository<OpportunitySkill, Long> {

    @Query("SELECT os FROM OpportunitySkill os JOIN FETCH os.skill WHERE os.opportunity.id = :opportunityId")
    List<OpportunitySkill> findByOpportunityIdFetchSkill(@Param("opportunityId") Long opportunityId);

    @Query("SELECT os FROM OpportunitySkill os JOIN FETCH os.skill JOIN FETCH os.opportunity WHERE os.opportunity.id IN :opportunityIds")
    List<OpportunitySkill> findByOpportunityIdInFetchSkill(@Param("opportunityIds") Collection<Long> opportunityIds);

    boolean existsByOpportunityIdAndSkillId(Long opportunityId, Long skillId);
}