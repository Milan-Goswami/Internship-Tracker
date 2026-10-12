package com.interntrack.api.service;

import com.interntrack.api.dto.skill.AddUserSkillRequest;
import com.interntrack.api.dto.skill.OpportunitySkillMatchResponse;
import com.interntrack.api.dto.skill.SkillResponse;
import com.interntrack.api.dto.skill.UserSkillResponse;
import com.interntrack.api.entity.*;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.ProficiencyLevel;
import com.interntrack.api.entity.enums.SkillCategory;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.DuplicateSkillException;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.OpportunitySkillRepository;
import com.interntrack.api.repository.SkillRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private OpportunitySkillRepository opportunitySkillRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SkillMatchingService skillMatchingService;

    @InjectMocks
    private SkillService skillService;

    private User testUser;
    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "hash", "John Doe", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(testUser, "id", 100L);

        testSkill = new Skill("Java", SkillCategory.LANGUAGE);
        ReflectionTestUtils.setField(testSkill, "id", 10L);
    }

    @Test
    @DisplayName("searchSkills with empty query returns all skills")
    void searchSkills_emptyQuery_returnsAll() {
        when(skillRepository.findAll()).thenReturn(List.of(testSkill));

        List<SkillResponse> res = skillService.searchSkills(null);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).name()).isEqualTo("Java");
        verify(skillRepository).findAll();
    }

    @Test
    @DisplayName("searchSkills with query calls findByNameContainingIgnoreCase")
    void searchSkills_withQuery_callsRepo() {
        when(skillRepository.findByNameContainingIgnoreCase("jav")).thenReturn(List.of(testSkill));

        List<SkillResponse> res = skillService.searchSkills("jav");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).canonicalName()).isEqualTo("java");
        verify(skillRepository).findByNameContainingIgnoreCase("jav");
    }

    @Test
    @DisplayName("getUserSkills returns user skills mapped to response")
    void getUserSkills_validUser_returnsMapped() {
        UserSkill us = new UserSkill(testUser, testSkill, ProficiencyLevel.ADVANCED, 3);
        when(userSkillRepository.findByUserIdFetchSkill(100L)).thenReturn(List.of(us));

        List<UserSkillResponse> res = skillService.getUserSkills(100L);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).skillName()).isEqualTo("Java");
        assertThat(res.get(0).proficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
        assertThat(res.get(0).yearsOfExperience()).isEqualTo(3);
    }

    @Test
    @DisplayName("addUserSkill successfully saves and returns response")
    void addUserSkill_valid_savesAndReturns() {
        AddUserSkillRequest req = new AddUserSkillRequest(10L, ProficiencyLevel.EXPERT, 4);

        when(skillRepository.findById(10L)).thenReturn(Optional.of(testSkill));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userSkillRepository.existsByUserIdAndSkillId(100L, 10L)).thenReturn(false);

        UserSkill saved = new UserSkill(testUser, testSkill, ProficiencyLevel.EXPERT, 4);
        when(userSkillRepository.save(any(UserSkill.class))).thenReturn(saved);

        UserSkillResponse res = skillService.addUserSkill(100L, req);

        assertThat(res.skillId()).isEqualTo(10L);
        assertThat(res.proficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT);
        assertThat(res.yearsOfExperience()).isEqualTo(4);
        verify(userSkillRepository).save(any(UserSkill.class));
    }

    @Test
    @DisplayName("addUserSkill on non-existent skill throws ResourceNotFoundException")
    void addUserSkill_skillNotFound_throws404() {
        AddUserSkillRequest req = new AddUserSkillRequest(999L, ProficiencyLevel.INTERMEDIATE, 1);
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.addUserSkill(100L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found");
    }

    @Test
    @DisplayName("addUserSkill with duplicate skill throws DuplicateSkillException")
    void addUserSkill_duplicate_throws409() {
        AddUserSkillRequest req = new AddUserSkillRequest(10L, ProficiencyLevel.INTERMEDIATE, 1);
        when(skillRepository.findById(10L)).thenReturn(Optional.of(testSkill));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userSkillRepository.existsByUserIdAndSkillId(100L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> skillService.addUserSkill(100L, req))
                .isInstanceOf(DuplicateSkillException.class)
                .hasMessageContaining("already added");
    }

    @Test
    @DisplayName("removeUserSkill successfully deletes existing skill")
    void removeUserSkill_existing_deletes() {
        UserSkill us = new UserSkill(testUser, testSkill, ProficiencyLevel.BEGINNER, 1);
        when(userSkillRepository.findByUserIdAndSkillId(100L, 10L)).thenReturn(Optional.of(us));

        skillService.removeUserSkill(100L, 10L);

        verify(userSkillRepository).delete(us);
    }

    @Test
    @DisplayName("removeUserSkill for skill not on profile throws ResourceNotFoundException")
    void removeUserSkill_notFound_throws404() {
        when(userSkillRepository.findByUserIdAndSkillId(100L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.removeUserSkill(100L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found on user profile");
    }

    @Test
    @DisplayName("getOpportunitySkillMatch for accessible opportunity computes match")
    void getOpportunitySkillMatch_accessibleOpp_computes() {
        Company company = new Company("Tech Corp", "techcorp.com", "Tech");
        Opportunity opp = new Opportunity(company, "SWE Intern", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        ReflectionTestUtils.setField(opp, "id", 50L);

        when(opportunityRepository.findById(50L)).thenReturn(Optional.of(opp));
        when(userSkillRepository.findByUserIdFetchSkill(100L)).thenReturn(Collections.emptyList());
        when(opportunitySkillRepository.findByOpportunityIdFetchSkill(50L)).thenReturn(Collections.emptyList());

        OpportunitySkillMatchResponse matchResponse = new OpportunitySkillMatchResponse(
                50L, null, "No Skills Specified", Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), 0, 0, true
        );
        when(skillMatchingService.calculateMatch(eq(50L), any(), any())).thenReturn(matchResponse);

        OpportunitySkillMatchResponse res = skillService.getOpportunitySkillMatch(100L, 50L);

        assertThat(res.opportunityId()).isEqualTo(50L);
        assertThat(res.zeroSkillsSpecified()).isTrue();
    }

    @Test
    @DisplayName("getOpportunitySkillMatch on another candidate's private opportunity throws ResourceNotFoundException")
    void getOpportunitySkillMatch_otherUserCustomOpp_throws404() {
        Company company = new Company("Tech Corp", "techcorp.com", "Tech");
        User otherUser = new User("other@example.com", "hash", "Other User", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(otherUser, "id", 999L);

        Opportunity customOpp = new Opportunity(company, "Private Intern", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        customOpp.setCreatedByUser(otherUser);
        ReflectionTestUtils.setField(customOpp, "id", 50L);

        when(opportunityRepository.findById(50L)).thenReturn(Optional.of(customOpp));

        assertThatThrownBy(() -> skillService.getOpportunitySkillMatch(100L, 50L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Opportunity not found");
    }
}