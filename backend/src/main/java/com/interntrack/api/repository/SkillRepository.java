package com.interntrack.api.repository;

import com.interntrack.api.entity.Skill;
import com.interntrack.api.entity.enums.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByCanonicalName(String canonicalName);

    boolean existsByCanonicalName(String canonicalName);

    List<Skill> findByCategory(SkillCategory category);

    List<Skill> findByNameContainingIgnoreCase(String name);
}