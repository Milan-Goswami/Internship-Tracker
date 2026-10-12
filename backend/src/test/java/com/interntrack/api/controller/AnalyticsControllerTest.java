package com.interntrack.api.controller;

import com.interntrack.api.dto.analytics.DashboardSummaryResponse;
import com.interntrack.api.dto.analytics.FunnelAnalyticsResponse;
import com.interntrack.api.dto.analytics.FunnelStageDto;
import com.interntrack.api.dto.analytics.UpcomingInterviewDto;
import com.interntrack.api.entity.enums.InterviewRoundType;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_ANALYTICS_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class AnalyticsControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AnalyticsService analyticsService;

    private MockMvc mockMvc;
    private String userToken;
    private final Long USER_ID = 200L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        AuthenticatedUser authUser = new AuthenticatedUser(USER_ID, "candidate@example.com", "Candidate", UserRole.ROLE_USER, true);
        userToken = "Bearer " + jwtService.generateAccessToken(authUser);
    }

    @Test
    @DisplayName("GET /api/v1/analytics/summary returns 401 when unauthenticated")
    void getDashboardSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/analytics/summary returns 200 with dashboard metrics")
    void getDashboardSummary_authenticated_returns200() throws Exception {
        UpcomingInterviewDto interviewDto = new UpcomingInterviewDto(
                1L, 10L, "Stripe", "Backend Intern", 1, "OA Review",
                InterviewRoundType.LIVE_CODING, Instant.now().plusSeconds(7200), 45
        );

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                5, 3, 40.0, 1, 1,
                Map.of("APPLIED", 2, "INTERVIEWING", 1),
                List.of(),
                List.of(interviewDto)
        );

        when(analyticsService.getDashboardSummary(USER_ID)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalApplications", is(5)))
                .andExpect(jsonPath("$.data.activeApplications", is(3)))
                .andExpect(jsonPath("$.data.interviewRate", is(40.0)))
                .andExpect(jsonPath("$.data.upcomingInterviews[0].companyName", is("Stripe")));

        verify(analyticsService).getDashboardSummary(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/analytics/funnel returns 401 when unauthenticated")
    void getFunnelAnalytics_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/funnel")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/analytics/funnel returns 200 with conversion metrics")
    void getFunnelAnalytics_authenticated_returns200() throws Exception {
        List<FunnelStageDto> stages = List.of(
                new FunnelStageDto("SAVED", 10, 100.0),
                new FunnelStageDto("APPLIED", 8, 80.0),
                new FunnelStageDto("SCREENING", 4, 50.0),
                new FunnelStageDto("INTERVIEWING", 2, 50.0),
                new FunnelStageDto("OFFER", 1, 50.0)
        );

        FunnelAnalyticsResponse funnel = new FunnelAnalyticsResponse(
                10, 8, 4, 2, 1,
                80.0, 50.0, 50.0, 50.0, 12.5,
                stages
        );

        when(analyticsService.getFunnelAnalytics(USER_ID)).thenReturn(funnel);

        mockMvc.perform(get("/api/v1/analytics/funnel")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalSaved", is(10)))
                .andExpect(jsonPath("$.data.appliedConversionRate", is(80.0)))
                .andExpect(jsonPath("$.data.overallConversionRate", is(12.5)))
                .andExpect(jsonPath("$.data.stages", hasSize(5)));

        verify(analyticsService).getFunnelAnalytics(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/analytics/skill-deficits without token returns 401 Unauthorized")
    void getSkillDeficits_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/skill-deficits"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/analytics/skill-deficits with token returns 200 OK")
    void getSkillDeficits_withToken_returns200() throws Exception {
        com.interntrack.api.dto.skill.SkillDeficitDto deficit = new com.interntrack.api.dto.skill.SkillDeficitDto(
                "Kubernetes", "kubernetes", com.interntrack.api.entity.enums.SkillCategory.CLOUD, 3, 75.0
        );
        com.interntrack.api.dto.skill.SkillDeficitsResponse resp = new com.interntrack.api.dto.skill.SkillDeficitsResponse(
                4, 2, List.of(deficit)
        );

        when(analyticsService.getSkillDeficits(USER_ID)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/analytics/skill-deficits")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTrackedOpportunities").value(4))
                .andExpect(jsonPath("$.data.candidateSkillCount").value(2))
                .andExpect(jsonPath("$.data.topDeficits", hasSize(1)))
                .andExpect(jsonPath("$.data.topDeficits[0].skillName").value("Kubernetes"))
                .andExpect(jsonPath("$.data.topDeficits[0].frequencyPercentage").value(75.0))
                .andExpect(jsonPath("$.message").value("Skill deficits retrieved successfully"));
    }
}