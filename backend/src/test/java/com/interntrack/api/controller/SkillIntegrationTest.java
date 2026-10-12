package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.skill.AddUserSkillRequest;
import com.interntrack.api.entity.*;
import com.interntrack.api.entity.enums.*;
import com.interntrack.api.repository.*;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.SkillMatchingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=SKILL_INTEGRATION_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class SkillIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private OpportunitySkillRepository opportunitySkillRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private SkillMatchingService skillMatchingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String userToken;
    private User testUser;
    private Company testCompany;
    private Opportunity testOpportunity;
    private Skill pythonSkill;
    private Skill reactSkill;

    private static final String TEST_USER_EMAIL = "skill_integration_user@example.com";
    private static final String TEST_COMPANY_CANONICAL = "skillintegrationcompany";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanDatabase();

        testUser = new User(TEST_USER_EMAIL, "pwd123456", "Skill Candidate", UserRole.ROLE_USER);
        testUser = userRepository.save(testUser);

        testCompany = new Company("Skill Integration Company", "https://skilltest.com", "Tech");
        testCompany = companyRepository.save(testCompany);

        testOpportunity = new Opportunity(testCompany, "Full Stack Engineer Intern", LocationType.HYBRID, SourceProvider.USER_MANUAL);
        testOpportunity.setCreatedByUser(testUser);
        testOpportunity = opportunityRepository.save(testOpportunity);

        pythonSkill = new Skill("Python", SkillCategory.LANGUAGE);
        pythonSkill = skillRepository.save(pythonSkill);

        reactSkill = new Skill("React", SkillCategory.FRAMEWORK);
        reactSkill = skillRepository.save(reactSkill);

        AuthenticatedUser principal = new AuthenticatedUser(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getFullName(),
                testUser.getRole(),
                true
        );
        userToken = "Bearer " + jwtService.generateAccessToken(principal);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        userRepository.findByEmail(TEST_USER_EMAIL).ifPresent(u -> {
            applicationRepository.deleteAll(applicationRepository.findByUserId(u.getId()));
            applicationRepository.flush();

            opportunitySkillRepository.deleteAll();
            opportunitySkillRepository.flush();

            userSkillRepository.deleteAll(userSkillRepository.findByUserIdFetchSkill(u.getId()));
            userSkillRepository.flush();

            opportunityRepository.findByCreatedByUserId(u.getId()).forEach(opportunityRepository::delete);
            opportunityRepository.flush();

            userRepository.delete(u);
            userRepository.flush();
        });

        companyRepository.findByCanonicalName(TEST_COMPANY_CANONICAL).ifPresent(c -> {
            companyRepository.delete(c);
            companyRepository.flush();
        });

        skillRepository.findByCanonicalName("python").ifPresent(s -> {
            skillRepository.delete(s);
            skillRepository.flush();
        });

        skillRepository.findByCanonicalName("react").ifPresent(s -> {
            skillRepository.delete(s);
            skillRepository.flush();
        });
    }

    @Test
    @DisplayName("End-to-End Skill Taxonomy Search, Profile CRUD, Skill Matching, and Deficit Analytics")
    void endToEndSkillLifecycle() throws Exception {
        // 1. Autocomplete Search
        mockMvc.perform(get("/api/v1/skills/search?q=pyth")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Python"))
                .andExpect(jsonPath("$.data[0].canonicalName").value("python"));

        // 2. Add Skill to User Profile
        AddUserSkillRequest addReq = new AddUserSkillRequest(pythonSkill.getId(), ProficiencyLevel.ADVANCED, 2);
        mockMvc.perform(post("/api/v1/skills/user")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skillName").value("Python"))
                .andExpect(jsonPath("$.data.proficiencyLevel").value("ADVANCED"))
                .andExpect(jsonPath("$.data.yearsOfExperience").value(2));

        // 3. Duplicate Skill Rejection -> 409
        mockMvc.perform(post("/api/v1/skills/user")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Skill is already added to user profile"));

        // 4. List User Skills -> 200 with 1 skill
        mockMvc.perform(get("/api/v1/skills/user")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].skillName").value("Python"));

        // Configure Opportunity Requirements: Python (required = true), React (required = false, preferred)
        OpportunitySkill os1 = new OpportunitySkill(testOpportunity, pythonSkill, true);
        OpportunitySkill os2 = new OpportunitySkill(testOpportunity, reactSkill, false);
        opportunitySkillRepository.saveAll(List.of(os1, os2));
        opportunitySkillRepository.flush();

        // 5. Compute Opportunity Skill Match
        // Candidate has Python (1/1 req = 100% * 0.75 = 75%), missing React (0/1 pref = 0%) -> Total 75.0% -> MEDIUM
        mockMvc.perform(get("/api/v1/opportunities/" + testOpportunity.getId() + "/skill-match")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.opportunityId").value(testOpportunity.getId()))
                .andExpect(jsonPath("$.data.matchPercentage").value(75.0))
                .andExpect(jsonPath("$.data.matchLabel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.matchedRequiredSkills", contains("Python")))
                .andExpect(jsonPath("$.data.missingPreferredSkills", contains("React")));

        // 6. Track Opportunity via Application
        Application application = new Application(testUser, testOpportunity, ApplicationStatus.APPLIED);
        applicationRepository.save(application);
        applicationRepository.flush();

        // 7. Aggregate Skill Deficits
        mockMvc.perform(get("/api/v1/analytics/skill-deficits")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTrackedOpportunities").value(1))
                .andExpect(jsonPath("$.data.candidateSkillCount").value(1))
                .andExpect(jsonPath("$.data.topDeficits", hasSize(1)))
                .andExpect(jsonPath("$.data.topDeficits[0].skillName").value("React"))
                .andExpect(jsonPath("$.data.topDeficits[0].opportunityCount").value(1))
                .andExpect(jsonPath("$.data.topDeficits[0].frequencyPercentage").value(100.0));

        // 8. Delete User Skill -> 204
        mockMvc.perform(delete("/api/v1/skills/user/" + pythonSkill.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        // Subsequent delete -> 404
        mockMvc.perform(delete("/api/v1/skills/user/" + pythonSkill.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("In-Memory Skill Matching Benchmark executes well below the 10ms threshold")
    void skillMatchingBenchmark() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, pythonSkill, true),
                new OpportunitySkill(testOpportunity, reactSkill, false)
        );
        Set<String> candidateSkills = Set.of("python");

        // Warmup
        for (int i = 0; i < 5000; i++) {
            skillMatchingService.calculateMatch(testOpportunity.getId(), candidateSkills, oppSkills);
        }

        long start = System.nanoTime();
        int iterations = 10000;
        for (int i = 0; i < iterations; i++) {
            skillMatchingService.calculateMatch(testOpportunity.getId(), candidateSkills, oppSkills);
        }
        long durationNs = System.nanoTime() - start;
        double avgMs = (double) durationNs / (iterations * 1_000_000.0);

        assertThat(avgMs).isLessThan(10.0);
    }
}