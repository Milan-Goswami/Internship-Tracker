package com.interntrack.api.service;

import com.interntrack.api.dto.analytics.DashboardSummaryResponse;
import com.interntrack.api.dto.analytics.FunnelAnalyticsResponse;
import com.interntrack.api.dto.analytics.FunnelStageCounts;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.OpportunitySkill;
import com.interntrack.api.entity.Skill;
import com.interntrack.api.entity.UserSkill;
import com.interntrack.api.entity.Reminder;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.InterviewRoundType;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.ReminderType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.InterviewRepository;
import com.interntrack.api.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private com.interntrack.api.repository.UserSkillRepository userSkillRepository;

    @Mock
    private com.interntrack.api.repository.OpportunitySkillRepository opportunitySkillRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User sampleUser;
    private Application sampleApplication;
    private Interview sampleInterview;
    private Reminder sampleReminder;

    @BeforeEach
    void setUp() {
        sampleUser = new User("candidate@example.com", "hash", "Candidate", UserRole.ROLE_USER);
        Company company = new Company("Google", "google.com", "Tech");
        Opportunity opp = new Opportunity(company, "SWE Intern", LocationType.REMOTE, SourceProvider.SYSTEM);
        sampleApplication = new Application(sampleUser, opp, ApplicationStatus.APPLIED);

        sampleInterview = new Interview(
                sampleApplication,
                1,
                "Technical Screen",
                InterviewRoundType.LIVE_CODING,
                Instant.now().plusSeconds(86400)
        );
        sampleInterview.setDurationMinutes(60);

        sampleReminder = new Reminder(
                sampleUser,
                sampleApplication,
                "Submit OA",
                Instant.now().plusSeconds(3600),
                ReminderType.OA_DEADLINE
        );
    }

    @Test
    @DisplayName("getDashboardSummary returns zero counts and 0.0 rates on empty dataset")
    void getDashboardSummary_empty_returnsZeros() {
        when(applicationRepository.countByUserIdAndAppliedAtIsNotNull(1L)).thenReturn(0L);
        when(applicationRepository.countByUserIdAndInterviewingAtIsNotNull(1L)).thenReturn(0L);
        when(reminderRepository.countByUserIdAndCompletedFalseAndReminderType(1L, ReminderType.OA_DEADLINE)).thenReturn(0L);
        when(applicationRepository.countGroupByStatusForUser(1L)).thenReturn(List.of());
        when(reminderRepository.findTop5ByUserIdAndCompletedFalseOrderByDueAtAsc(1L)).thenReturn(List.of());
        when(interviewRepository.findUpcomingByUserId(eq(1L), any(Instant.class), any(Pageable.class))).thenReturn(List.of());

        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(1L);

        assertThat(summary).isNotNull();
        assertThat(summary.totalApplications()).isEqualTo(0);
        assertThat(summary.activeApplications()).isEqualTo(0);
        assertThat(summary.interviewRate()).isEqualTo(0.0);
        assertThat(summary.pendingOAs()).isEqualTo(0);
        assertThat(summary.offersReceived()).isEqualTo(0);
        assertThat(summary.urgentReminders()).isEmpty();
        assertThat(summary.upcomingInterviews()).isEmpty();
        assertThat(summary.statusCounts()).containsEntry("SAVED", 0);
        assertThat(summary.statusCounts()).containsEntry("APPLIED", 0);
    }

    @Test
    @DisplayName("getDashboardSummary computes correct metrics on populated data")
    void getDashboardSummary_populated_returnsComputedMetrics() {
        when(applicationRepository.countByUserIdAndAppliedAtIsNotNull(1L)).thenReturn(8L);
        when(applicationRepository.countByUserIdAndInterviewingAtIsNotNull(1L)).thenReturn(4L);
        when(reminderRepository.countByUserIdAndCompletedFalseAndReminderType(1L, ReminderType.OA_DEADLINE)).thenReturn(2L);

        List<Object[]> statusRows = List.<Object[]>of(
                new Object[]{ApplicationStatus.APPLIED, 4L},
                new Object[]{ApplicationStatus.INTERVIEWING, 4L},
                new Object[]{ApplicationStatus.OFFER, 2L}
        );
        when(applicationRepository.countGroupByStatusForUser(1L)).thenReturn(statusRows);
        when(reminderRepository.findTop5ByUserIdAndCompletedFalseOrderByDueAtAsc(1L)).thenReturn(List.of(sampleReminder));
        when(interviewRepository.findUpcomingByUserId(eq(1L), any(Instant.class), any(Pageable.class))).thenReturn(List.of(sampleInterview));

        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(1L);

        assertThat(summary.totalApplications()).isEqualTo(10);
        assertThat(summary.activeApplications()).isEqualTo(8);
        assertThat(summary.interviewRate()).isEqualTo(50.0); // 4 / 8 * 100
        assertThat(summary.pendingOAs()).isEqualTo(2);
        assertThat(summary.offersReceived()).isEqualTo(2);
        assertThat(summary.urgentReminders()).hasSize(1);
        assertThat(summary.upcomingInterviews()).hasSize(1);
        assertThat(summary.upcomingInterviews().get(0).companyName()).isEqualTo("Google");
        assertThat(summary.statusCounts().get("APPLIED")).isEqualTo(4);
        assertThat(summary.statusCounts().get("INTERVIEWING")).isEqualTo(4);
        assertThat(summary.statusCounts().get("OFFER")).isEqualTo(2);
        assertThat(summary.statusCounts().get("REJECTED")).isEqualTo(0);
    }

    @Test
    @DisplayName("getFunnelAnalytics correctly calculates conversions from first-entry timestamps")
    void getFunnelAnalytics_calculatesConversions() {
        // Saved=10, Applied=8, Screening=6, Interviewing=3, Offer=1
        when(applicationRepository.getFunnelStageCounts(eq(1L), anyCollection()))
                .thenReturn(new FunnelStageCounts(10L, 8L, 6L, 3L, 1L));

        FunnelAnalyticsResponse funnel = analyticsService.getFunnelAnalytics(1L);

        assertThat(funnel.totalSaved()).isEqualTo(10);
        assertThat(funnel.totalApplied()).isEqualTo(8);
        assertThat(funnel.totalScreening()).isEqualTo(6);
        assertThat(funnel.totalInterviewing()).isEqualTo(3);
        assertThat(funnel.totalOffer()).isEqualTo(1);

        assertThat(funnel.appliedConversionRate()).isEqualTo(80.0); // 8/10 * 100
        assertThat(funnel.screeningConversionRate()).isEqualTo(75.0); // 6/8 * 100
        assertThat(funnel.interviewingConversionRate()).isEqualTo(50.0); // 3/6 * 100
        assertThat(funnel.offerConversionRate()).isEqualTo(33.33); // 1/3 * 100
        assertThat(funnel.overallConversionRate()).isEqualTo(12.5); // 1/8 * 100

        assertThat(funnel.stages()).hasSize(5);
        assertThat(funnel.stages().get(0).stageName()).isEqualTo("SAVED");
        assertThat(funnel.stages().get(4).stageName()).isEqualTo("OFFER");
    }

    @Test
    @DisplayName("getFunnelAnalytics preserves prior immutable timestamps for rejected application without incrementing offer")
    void getFunnelAnalytics_rejectedApplication_preservesPriorTimestamps() {
        // Application reached INTERVIEWING and then was REJECTED:
        // savedAt != null, appliedAt != null, screeningAt != null, interviewingAt != null
        // but status == REJECTED (so offer stage count = 0)
        when(applicationRepository.getFunnelStageCounts(eq(1L), anyCollection()))
                .thenReturn(new FunnelStageCounts(1L, 1L, 1L, 1L, 0L));

        FunnelAnalyticsResponse funnel = analyticsService.getFunnelAnalytics(1L);

        // All prior stages count 1 because first-entry timestamps are immutable
        assertThat(funnel.totalSaved()).isEqualTo(1);
        assertThat(funnel.totalApplied()).isEqualTo(1);
        assertThat(funnel.totalScreening()).isEqualTo(1);
        assertThat(funnel.totalInterviewing()).isEqualTo(1);
        // Current status is REJECTED, so offer count is 0
        assertThat(funnel.totalOffer()).isEqualTo(0);
        assertThat(funnel.offerConversionRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getFunnelAnalytics includes ACCEPTED as offer-stage conversion")
    void getFunnelAnalytics_acceptedApplication_countsAsOffer() {
        // Application received offer and was ACCEPTED
        when(applicationRepository.getFunnelStageCounts(eq(1L), anyCollection()))
                .thenReturn(new FunnelStageCounts(1L, 1L, 1L, 1L, 1L));

        FunnelAnalyticsResponse funnel = analyticsService.getFunnelAnalytics(1L);

        assertThat(funnel.totalOffer()).isEqualTo(1);
        assertThat(funnel.offerConversionRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getFunnelAnalytics handles empty data and zero denominators safely")
    void getFunnelAnalytics_emptyData_returnsZeros() {
        when(applicationRepository.getFunnelStageCounts(eq(1L), anyCollection()))
                .thenReturn(new FunnelStageCounts(0L, 0L, 0L, 0L, 0L));

        FunnelAnalyticsResponse funnel = analyticsService.getFunnelAnalytics(1L);

        assertThat(funnel.totalSaved()).isEqualTo(0);
        assertThat(funnel.totalApplied()).isEqualTo(0);
        assertThat(funnel.totalScreening()).isEqualTo(0);
        assertThat(funnel.totalInterviewing()).isEqualTo(0);
        assertThat(funnel.totalOffer()).isEqualTo(0);
        assertThat(funnel.appliedConversionRate()).isEqualTo(0.0);
        assertThat(funnel.screeningConversionRate()).isEqualTo(0.0);
        assertThat(funnel.interviewingConversionRate()).isEqualTo(0.0);
        assertThat(funnel.offerConversionRate()).isEqualTo(0.0);
        assertThat(funnel.overallConversionRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Methods throw IllegalArgumentException when userId is null")
    void methods_whenUserIdNull_throwException() {
        assertThatThrownBy(() -> analyticsService.getDashboardSummary(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> analyticsService.getFunnelAnalytics(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getSkillDeficits with no applications returns zero tracked and empty list")
    void getSkillDeficits_emptyApplications_returnsEmpty() {
        when(applicationRepository.findDistinctOpportunityIdsByUserId(100L)).thenReturn(List.of());
        when(userSkillRepository.findByUserIdFetchSkill(100L)).thenReturn(List.of());

        com.interntrack.api.dto.skill.SkillDeficitsResponse res = analyticsService.getSkillDeficits(100L);

        assertThat(res.totalTrackedOpportunities()).isEqualTo(0);
        assertThat(res.candidateSkillCount()).isEqualTo(0);
        assertThat(res.topDeficits()).isEmpty();
    }

    @Test
    @DisplayName("getSkillDeficits aggregates missing skills ranked by frequency descending")
    void getSkillDeficits_withMissingSkills_calculatesRankedFrequencies() {
        User testUser = new User("cand@example.com", "hash", "Cand", com.interntrack.api.entity.enums.UserRole.ROLE_USER);
        org.springframework.test.util.ReflectionTestUtils.setField(testUser, "id", 100L);
        Company comp = new Company("Tech Corp", "techcorp.com", "Tech");
        Opportunity opp1 = new Opportunity(comp, "Role 1", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        org.springframework.test.util.ReflectionTestUtils.setField(opp1, "id", 10L);

        Opportunity opp2 = new Opportunity(comp, "Role 2", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        org.springframework.test.util.ReflectionTestUtils.setField(opp2, "id", 20L);

        when(applicationRepository.findDistinctOpportunityIdsByUserId(100L)).thenReturn(List.of(10L, 20L));

        Skill javaSkill = new Skill("Java", com.interntrack.api.entity.enums.SkillCategory.LANGUAGE);
        Skill k8sSkill = new Skill("Kubernetes", com.interntrack.api.entity.enums.SkillCategory.CLOUD);
        Skill dockerSkill = new Skill("Docker", com.interntrack.api.entity.enums.SkillCategory.TOOL);

        // Candidate has Java
        UserSkill candidateSkill = new UserSkill(testUser, javaSkill, com.interntrack.api.entity.enums.ProficiencyLevel.ADVANCED, 2);
        when(userSkillRepository.findByUserIdFetchSkill(100L)).thenReturn(List.of(candidateSkill));

        // Opp1 needs Java and Kubernetes; Opp2 needs Kubernetes and Docker
        OpportunitySkill os1 = new OpportunitySkill(opp1, javaSkill, true);
        OpportunitySkill os2 = new OpportunitySkill(opp1, k8sSkill, true);
        OpportunitySkill os3 = new OpportunitySkill(opp2, k8sSkill, true);
        OpportunitySkill os4 = new OpportunitySkill(opp2, dockerSkill, false);

        when(opportunitySkillRepository.findByOpportunityIdInFetchSkill(List.of(10L, 20L)))
                .thenReturn(List.of(os1, os2, os3, os4));

        com.interntrack.api.dto.skill.SkillDeficitsResponse res = analyticsService.getSkillDeficits(100L);

        assertThat(res.totalTrackedOpportunities()).isEqualTo(2);
        assertThat(res.candidateSkillCount()).isEqualTo(1);
        assertThat(res.topDeficits()).hasSize(2);

        // Kubernetes is in both opps (2/2 = 100%), Docker is in one opp (1/2 = 50%)
        assertThat(res.topDeficits().get(0).canonicalName()).isEqualTo("kubernetes");
        assertThat(res.topDeficits().get(0).opportunityCount()).isEqualTo(2);
        assertThat(res.topDeficits().get(0).frequencyPercentage()).isEqualTo(100.0);

        assertThat(res.topDeficits().get(1).canonicalName()).isEqualTo("docker");
        assertThat(res.topDeficits().get(1).opportunityCount()).isEqualTo(1);
        assertThat(res.topDeficits().get(1).frequencyPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getDashboardSummary with all statuses correctly aggregates active and terminal counts")
    void getDashboardSummary_allStatuses_correctlyAggregatesActiveAndTerminal() {
        when(applicationRepository.countByUserIdAndAppliedAtIsNotNull(1L)).thenReturn(7L);
        when(applicationRepository.countByUserIdAndInterviewingAtIsNotNull(1L)).thenReturn(3L);
        when(reminderRepository.countByUserIdAndCompletedFalseAndReminderType(1L, ReminderType.OA_DEADLINE)).thenReturn(1L);

        List<Object[]> statusRows = List.<Object[]>of(
                new Object[]{ApplicationStatus.SAVED, 1L},
                new Object[]{ApplicationStatus.APPLIED, 2L},
                new Object[]{ApplicationStatus.SCREENING, 1L},
                new Object[]{ApplicationStatus.INTERVIEWING, 1L},
                new Object[]{ApplicationStatus.OFFER, 1L},
                new Object[]{ApplicationStatus.ACCEPTED, 1L},
                new Object[]{ApplicationStatus.REJECTED, 3L},
                new Object[]{ApplicationStatus.WITHDRAWN, 2L}
        );
        when(applicationRepository.countGroupByStatusForUser(1L)).thenReturn(statusRows);
        when(reminderRepository.findTop5ByUserIdAndCompletedFalseOrderByDueAtAsc(1L)).thenReturn(List.of());
        when(interviewRepository.findUpcomingByUserId(eq(1L), any(Instant.class), any(Pageable.class))).thenReturn(List.of());

        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(1L);

        // Total = 1 + 2 + 1 + 1 + 1 + 1 + 3 + 2 = 12
        assertThat(summary.totalApplications()).isEqualTo(12);
        // Active = SAVED (1) + APPLIED (2) + SCREENING (1) + INTERVIEWING (1) = 5
        assertThat(summary.activeApplications()).isEqualTo(5);
        // Offers = OFFER (1) + ACCEPTED (1) = 2
        assertThat(summary.offersReceived()).isEqualTo(2);
        assertThat(summary.interviewRate()).isEqualTo(42.86); // 3/7 * 100
        assertThat(summary.statusCounts().get("REJECTED")).isEqualTo(3);
        assertThat(summary.statusCounts().get("WITHDRAWN")).isEqualTo(2);
    }
}