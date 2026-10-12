package com.interntrack.api.repository;

import com.interntrack.api.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    @Query("SELECT us FROM UserSkill us JOIN FETCH us.skill WHERE us.user.id = :userId ORDER BY us.skill.name ASC")
    List<UserSkill> findByUserIdFetchSkill(@Param("userId") Long userId);

    Optional<UserSkill> findByUserIdAndSkillId(Long userId, Long skillId);

    boolean existsByUserIdAndSkillId(Long userId, Long skillId);

    void deleteByUserIdAndSkillId(Long userId, Long skillId);
}