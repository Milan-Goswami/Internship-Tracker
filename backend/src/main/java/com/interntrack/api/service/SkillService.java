package com.interntrack.api.service;

import com.interntrack.api.dto.skill.AddUserSkillRequest;
import com.interntrack.api.dto.skill.OpportunitySkillMatchResponse;
import com.interntrack.api.dto.skill.SkillResponse;
import com.interntrack.api.dto.skill.UserSkillResponse;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.OpportunitySkill;
import com.interntrack.api.entity.Skill;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.UserSkill;
import com.interntrack.api.entity.enums.ProficiencyLevel;
import com.interntrack.api.exception.DuplicateSkillException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.OpportunitySkillRepository;
import com.interntrack.api.repository.SkillRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.repository.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final OpportunitySkillRepository opportunitySkillRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final SkillMatchingService skillMatchingService;

    public SkillService(SkillRepository skillRepository,
                        UserSkillRepository userSkillRepository,
                        OpportunitySkillRepository opportunitySkillRepository,
                        OpportunityRepository opportunityRepository,
                        UserRepository userRepository,
                        SkillMatchingService skillMatchingService) {
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.opportunitySkillRepository = opportunitySkillRepository;
        this.opportunityRepository = opportunityRepository;
        this.userRepository = userRepository;
        this.skillMatchingService = skillMatchingService;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> searchSkills(String query) {
        List<Skill> skills;
        if (query == null || query.isBlank()) {
            skills = skillRepository.findAll();
        } else {
            skills = skillRepository.findByNameContainingIgnoreCase(query.trim());
        }
        return skills.stream().map(SkillResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserSkillResponse> getUserSkills(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        List<UserSkill> userSkills = userSkillRepository.findByUserIdFetchSkill(userId);
        return userSkills.stream().map(UserSkillResponse::from).toList();
    }

    @Transactional
    public UserSkillResponse addUserSkill(Long userId, AddUserSkillRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + request.skillId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (userSkillRepository.existsByUserIdAndSkillId(userId, skill.getId())) {
            throw new DuplicateSkillException("Skill is already added to user profile");
        }

        ProficiencyLevel level = request.proficiencyLevel() != null ? request.proficiencyLevel() : ProficiencyLevel.INTERMEDIATE;

        UserSkill userSkill = new UserSkill(user, skill, level, request.yearsOfExperience());
        UserSkill saved = userSkillRepository.save(userSkill);

        return UserSkillResponse.from(saved);
    }

    @Transactional
    public void removeUserSkill(Long userId, Long skillId) {
        if (userId == null || skillId == null) {
            throw new IllegalArgumentException("User ID and Skill ID cannot be null");
        }
        UserSkill userSkill = userSkillRepository.findByUserIdAndSkillId(userId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found on user profile"));

        userSkillRepository.delete(userSkill);
    }

    @Transactional(readOnly = true)
    public OpportunitySkillMatchResponse getOpportunitySkillMatch(Long userId, Long opportunityId) {
        if (userId == null || opportunityId == null) {
            throw new IllegalArgumentException("User ID and Opportunity ID cannot be null");
        }
        Opportunity opp = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));
        if (opp.getCreatedByUser() != null && !opp.getCreatedByUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Opportunity not found with id: " + opportunityId);
        }

        List<UserSkill> candidateSkills = userSkillRepository.findByUserIdFetchSkill(userId);
        Set<String> candidateCanonicalSkills = candidateSkills.stream()
                .map(us -> us.getSkill().getCanonicalName())
                .collect(Collectors.toSet());

        List<OpportunitySkill> oppSkills = opportunitySkillRepository.findByOpportunityIdFetchSkill(opportunityId);

        return skillMatchingService.calculateMatch(opportunityId, candidateCanonicalSkills, oppSkills);
    }
}