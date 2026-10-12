package com.interntrack.api.controller;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.InterviewOutcome;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.CompanyRepository;
import com.interntrack.api.repository.InterviewRepository;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=INTERVIEW_CASCADE_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class InterviewCascadeIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    private MockMvc mockMvc;
    private User testUser;
    private Company testCompany;
    private Opportunity testOpportunity;
    private Application testApplication;
    private String userToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanDatabase();

        testUser = userRepository.save(new User(
                "interview_tester@example.com",
                "$2a$12$DummyHashDummyHashDummyHashDummyHashDummyHashDummyHash",
                "Interview Tester",
                UserRole.ROLE_USER
        ));

        AuthenticatedUser authUser = new AuthenticatedUser(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getFullName(),
                testUser.getRole(),
                true
        );
        userToken = "Bearer " + jwtService.generateAccessToken(authUser);

        testCompany = companyRepository.save(new Company(
                "Interview Corp",
                "interviewcorp.com",
                "Technology"
        ));

        testOpportunity = opportunityRepository.save(new Opportunity(
                testCompany,
                "Backend Engineer Intern",
                LocationType.REMOTE,
                SourceProvider.SYSTEM
        ));

        testApplication = applicationRepository.save(new Application(
                testUser,
                testOpportunity,
                ApplicationStatus.APPLIED
        ));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        opportunityRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify interview scheduling, chronological ordering, outcome recording, and application cascade deletion")
    void testInterviewLifecycleAndCascadeDeletion() throws Exception {
        Long appId = testApplication.getId();

        // 1. Schedule Round 2 (scheduled further in future: 2026-10-15)
        String reqRound2 = """
                {
                    "roundNumber": 2,
                    "roundTitle": "System Design Round",
                    "roundType": "SYSTEM_DESIGN",
                    "scheduledAt": "2026-10-15T16:00:00Z",
                    "durationMinutes": 60,
                    "interviewerNames": "Lead Architect",
                    "prepNotes": "Review caching and database sharding"
                }
                """;

        mockMvc.perform(post("/api/v1/applications/" + appId + "/interviews")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqRound2))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/interviews/")));

        // 2. Schedule Round 1 (scheduled earlier: 2026-10-05)
        String reqRound1 = """
                {
                    "roundNumber": 1,
                    "roundTitle": "Recruiter Screen",
                    "roundType": "RECRUITER_SCREEN",
                    "scheduledAt": "2026-10-05T14:00:00Z",
                    "durationMinutes": 30,
                    "interviewerNames": "Recruiter Jane",
                    "prepNotes": "Review resume and background"
                }
                """;

        mockMvc.perform(post("/api/v1/applications/" + appId + "/interviews")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqRound1))
                .andExpect(status().isCreated());

        // 3. GET all rounds for application and assert chronological ordering (Round 1 first, then Round 2)
        mockMvc.perform(get("/api/v1/applications/" + appId + "/interviews")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].roundTitle").value("Recruiter Screen"))
                .andExpect(jsonPath("$.data[0].roundNumber").value(1))
                .andExpect(jsonPath("$.data[1].roundTitle").value("System Design Round"))
                .andExpect(jsonPath("$.data[1].roundNumber").value(2));

        // 4. Update outcome of Round 1
        List<Interview> persistedInterviews = interviewRepository.findByApplicationIdOrderByScheduledAtAsc(appId);
        assertThat(persistedInterviews).hasSize(2);
        Interview round1 = persistedInterviews.get(0);

        String outcomeReq = """
                {
                    "outcome": "PASSED",
                    "reflectionNotes": "Smooth screening, moving forward"
                }
                """;

        mockMvc.perform(patch("/api/v1/interviews/" + round1.getId() + "/outcome")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outcomeReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("PASSED"))
                .andExpect(jsonPath("$.data.reflectionNotes").value("Smooth screening, moving forward"));

        Interview updatedRound1 = interviewRepository.findById(round1.getId()).orElseThrow();
        assertThat(updatedRound1.getOutcome()).isEqualTo(InterviewOutcome.PASSED);

        // 5. Delete parent application and verify CASCADE deletion of both interview records
        mockMvc.perform(delete("/api/v1/applications/" + appId)
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        // Verify application is deleted
        assertThat(applicationRepository.findById(appId)).isEmpty();

        // Verify interviews are CASCADE deleted
        assertThat(interviewRepository.findByApplicationId(appId)).isEmpty();
    }
}
