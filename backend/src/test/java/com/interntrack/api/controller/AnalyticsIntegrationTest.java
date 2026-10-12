package com.interntrack.api.controller;

import com.interntrack.api.entity.*;
import com.interntrack.api.entity.enums.*;
import com.interntrack.api.repository.*;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=ANALYTICS_INTEGRATION_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class AnalyticsIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsIntegrationTest.class);

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

    @Autowired
    private ReminderRepository reminderRepository;

    private MockMvc mockMvc;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Opportunity opp1;
    private Opportunity opp2;
    private Application app1;
    private Application app2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long now = System.currentTimeMillis();

        userA = userRepository.save(new User("ana_a_" + now + "@test.com", "hashA", "Candidate A", UserRole.ROLE_USER));
        userB = userRepository.save(new User("ana_b_" + now + "@test.com", "hashB", "Candidate B", UserRole.ROLE_USER));

        AuthenticatedUser authA = new AuthenticatedUser(userA.getId(), userA.getEmail(), userA.getFullName(), userA.getRole(), true);
        AuthenticatedUser authB = new AuthenticatedUser(userB.getId(), userB.getEmail(), userB.getFullName(), userB.getRole(), true);

        tokenA = "Bearer " + jwtService.generateAccessToken(authA);
        tokenB = "Bearer " + jwtService.generateAccessToken(authB);

        Company comp1 = companyRepository.save(new Company("Meta_" + now, "meta.com", "Tech"));
        Company comp2 = companyRepository.save(new Company("Apple_" + now, "apple.com", "Hardware"));

        opp1 = opportunityRepository.save(new Opportunity(comp1, "Software Intern", LocationType.REMOTE, SourceProvider.SYSTEM));
        opp2 = opportunityRepository.save(new Opportunity(comp2, "iOS Intern", LocationType.ONSITE, SourceProvider.SYSTEM));

        // User A Applications:
        // App 1: SAVED -> APPLIED -> SCREENING -> INTERVIEWING -> OFFER -> ACCEPTED
        app1 = new Application(userA, opp1, ApplicationStatus.ACCEPTED);
        app1.setAppliedAt(Instant.now().minusSeconds(10000));
        app1.setScreeningAt(Instant.now().minusSeconds(8000));
        app1.setInterviewingAt(Instant.now().minusSeconds(5000));
        app1.setClosedAt(Instant.now().minusSeconds(1000));
        app1 = applicationRepository.save(app1);

        // App 2: SAVED -> APPLIED -> SCREENING -> INTERVIEWING (Active)
        app2 = new Application(userA, opp2, ApplicationStatus.INTERVIEWING);
        app2.setAppliedAt(Instant.now().minusSeconds(7000));
        app2.setScreeningAt(Instant.now().minusSeconds(4000));
        app2.setInterviewingAt(Instant.now().minusSeconds(2000));
        app2 = applicationRepository.save(app2);

        // User A Interview for App 2
        Interview int1 = new Interview(app2, 1, "System Design", InterviewRoundType.SYSTEM_DESIGN, Instant.now().plusSeconds(86400));
        int1.setDurationMinutes(60);
        interviewRepository.save(int1);

        // User A Reminders
        reminderRepository.save(new Reminder(userA, app2, "Prepare system design", Instant.now().plusSeconds(7200), ReminderType.INTERVIEW_PREP));
        reminderRepository.save(new Reminder(userA, app1, "Submit onboarding docs", Instant.now().plusSeconds(14400), ReminderType.CUSTOM));
        reminderRepository.save(new Reminder(userA, null, "Complete OA for Stripe", Instant.now().plusSeconds(3600), ReminderType.OA_DEADLINE));

        // User B Application (1 SAVED application)
        applicationRepository.save(new Application(userB, opp1, ApplicationStatus.SAVED));
    }

    @AfterEach
    void tearDown() {
        reminderRepository.deleteAll();
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        opportunityRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Multi-tenant isolation and accurate metric aggregation")
    void multiTenantAnalyticsIsolation() throws Exception {
        // User A Dashboard Summary
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalApplications", is(2)))
                .andExpect(jsonPath("$.data.activeApplications", is(1))) // only app2 is active (ACCEPTED is closed/terminal)
                .andExpect(jsonPath("$.data.interviewRate", is(100.0))) // both applied applications reached interviewing
                .andExpect(jsonPath("$.data.pendingOAs", is(1))) // 1 OA_DEADLINE reminder
                .andExpect(jsonPath("$.data.offersReceived", is(1))) // 1 ACCEPTED
                .andExpect(jsonPath("$.data.urgentReminders", hasSize(3)))
                .andExpect(jsonPath("$.data.upcomingInterviews", hasSize(1)))
                .andExpect(jsonPath("$.data.upcomingInterviews[0].companyName", containsString("Apple_")));

        // User B Dashboard Summary (isolated from User A)
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalApplications", is(1)))
                .andExpect(jsonPath("$.data.activeApplications", is(1))) // SAVED is active
                .andExpect(jsonPath("$.data.interviewRate", is(0.0)))
                .andExpect(jsonPath("$.data.pendingOAs", is(0)))
                .andExpect(jsonPath("$.data.offersReceived", is(0)))
                .andExpect(jsonPath("$.data.urgentReminders", hasSize(0)))
                .andExpect(jsonPath("$.data.upcomingInterviews", hasSize(0)));

        // User A Funnel
        mockMvc.perform(get("/api/v1/analytics/funnel")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalSaved", is(2)))
                .andExpect(jsonPath("$.data.totalApplied", is(2)))
                .andExpect(jsonPath("$.data.totalScreening", is(2)))
                .andExpect(jsonPath("$.data.totalInterviewing", is(2)))
                .andExpect(jsonPath("$.data.totalOffer", is(1)))
                .andExpect(jsonPath("$.data.appliedConversionRate", is(100.0)))
                .andExpect(jsonPath("$.data.overallConversionRate", is(50.0))); // 1 offer / 2 applied = 50%
    }

    @Test
    @DisplayName("Performance benchmark: /api/v1/analytics/summary responds well under 250ms target")
    void dashboardPerformanceBenchmark() throws Exception {
        // Warm up iteration
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk());

        List<Long> latencies = new ArrayList<>();
        int iterations = 5;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            mockMvc.perform(get("/api/v1/analytics/summary")
                            .header("Authorization", tokenA))
                    .andExpect(status().isOk());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            latencies.add(elapsedMs);
        }

        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0L);

        log.info("Dashboard KPI Performance Benchmark over {} iterations: Avg={}ms, Max={}ms (Target: < 250ms)",
                iterations, String.format("%.2f", avgLatency), maxLatency);

        // Validate healthy performance (comfortably below 250ms target)
        assertThat(avgLatency).isLessThan(250.0);
    }

    @Test
    @DisplayName("Upcoming interviews filter: only future PENDING interviews returned; PASSED, FAILED, RESCHEDULED and past excluded; user ownership preserved")
    void upcomingInterviews_outcomeAndFutureFilter_andUserIsolation() throws Exception {
        // Clear setup interviews to isolate test conditions
        interviewRepository.deleteAll();

        // 1. Future PENDING interview for User A -> MUST BE RETURNED
        Interview intFuturePending = new Interview(app2, 1, "Future Pending Round", InterviewRoundType.LIVE_CODING, Instant.now().plusSeconds(3600));
        intFuturePending.setOutcome(InterviewOutcome.PENDING);

        // 2. Future PASSED interview for User A -> MUST BE EXCLUDED
        Interview intFuturePassed = new Interview(app2, 2, "Future Passed Round", InterviewRoundType.LIVE_CODING, Instant.now().plusSeconds(7200));
        intFuturePassed.setOutcome(InterviewOutcome.PASSED);

        // 3. Future FAILED interview for User A -> MUST BE EXCLUDED
        Interview intFutureFailed = new Interview(app2, 3, "Future Failed Round", InterviewRoundType.LIVE_CODING, Instant.now().plusSeconds(10800));
        intFutureFailed.setOutcome(InterviewOutcome.FAILED);

        // 4. Future RESCHEDULED interview for User A -> MUST BE EXCLUDED
        Interview intFutureRescheduled = new Interview(app2, 4, "Future Rescheduled Round", InterviewRoundType.LIVE_CODING, Instant.now().plusSeconds(14400));
        intFutureRescheduled.setOutcome(InterviewOutcome.RESCHEDULED);

        // 5. Past PENDING interview for User A -> MUST BE EXCLUDED
        Interview intPastPending = new Interview(app2, 5, "Past Pending Round", InterviewRoundType.LIVE_CODING, Instant.now().minusSeconds(3600));
        intPastPending.setOutcome(InterviewOutcome.PENDING);

        interviewRepository.saveAll(List.of(intFuturePending, intFuturePassed, intFutureFailed, intFutureRescheduled, intPastPending));

        // Create an application and interview for User B
        Application appB = applicationRepository.save(new Application(userB, opp2, ApplicationStatus.INTERVIEWING));
        Interview intUserBFuturePending = new Interview(appB, 1, "User B Future Pending", InterviewRoundType.BEHAVIORAL, Instant.now().plusSeconds(5000));
        intUserBFuturePending.setOutcome(InterviewOutcome.PENDING);
        interviewRepository.save(intUserBFuturePending);

        // User A Dashboard Summary:
        // Must contain exactly 1 upcoming interview (intFuturePending)
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.upcomingInterviews", hasSize(1)))
                .andExpect(jsonPath("$.data.upcomingInterviews[0].roundTitle", is("Future Pending Round")))
                .andExpect(jsonPath("$.data.upcomingInterviews[0].companyName", containsString("Apple_")));

        // User B Dashboard Summary:
        // Must contain exactly 1 upcoming interview (intUserBFuturePending)
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.upcomingInterviews", hasSize(1)))
                .andExpect(jsonPath("$.data.upcomingInterviews[0].roundTitle", is("User B Future Pending")));
    }
}
