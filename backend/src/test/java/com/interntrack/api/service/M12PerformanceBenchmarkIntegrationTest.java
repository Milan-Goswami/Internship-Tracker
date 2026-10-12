package com.interntrack.api.service;

import com.interntrack.api.dto.analytics.DashboardSummaryResponse;
import com.interntrack.api.dto.analytics.FunnelAnalyticsResponse;
import com.interntrack.api.dto.analytics.FunnelStageCounts;
import com.interntrack.api.dto.skill.OpportunitySkillMatchResponse;
import com.interntrack.api.dto.skill.SkillDeficitsResponse;
import com.interntrack.api.entity.*;
import com.interntrack.api.entity.enums.*;
import com.interntrack.api.repository.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "security.jwt.secret=M12_PERFORMANCE_BENCHMARK_SECRET_KEY_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class M12PerformanceBenchmarkIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(M12PerformanceBenchmarkIntegrationTest.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private SkillMatchingService skillMatchingService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private OpportunitySkillRepository opportunitySkillRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Long candidateUserId;
    private static Long isolatedUserId;
    private static Long benchmarkOppId;
    private static boolean dataSeeded = false;

    @BeforeEach
    void seedDataIfNecessary() {
        if (dataSeeded) {
            return;
        }

        // Clean any existing rows
        reminderRepository.deleteAll();
        interviewRepository.deleteAll();
        opportunitySkillRepository.deleteAll();
        userSkillRepository.deleteAll();
        applicationRepository.deleteAll();
        opportunityRepository.deleteAll();
        companyRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();

        long nowEpoch = System.currentTimeMillis();

        // 1. Users
        User candidate = userRepository.save(new User(
                "candidate_" + nowEpoch + "@benchmark.com",
                "hash123",
                "Benchmark Candidate",
                UserRole.ROLE_USER
        ));
        candidateUserId = candidate.getId();

        User isolatedUser = userRepository.save(new User(
                "isolated_" + nowEpoch + "@benchmark.com",
                "hash456",
                "Isolated User",
                UserRole.ROLE_USER
        ));
        isolatedUserId = isolatedUser.getId();

        // 2. Representative Skill Dataset (50 skills across categories)
        List<Skill> skills = new ArrayList<>();
        SkillCategory[] categories = SkillCategory.values();
        for (int i = 1; i <= 50; i++) {
            SkillCategory cat = categories[i % categories.length];
            skills.add(skillRepository.save(new Skill("Skill_" + i, cat)));
        }

        // Candidate has 15 user skills
        for (int i = 0; i < 15; i++) {
            userSkillRepository.save(new UserSkill(
                    candidate,
                    skills.get(i),
                    ProficiencyLevel.ADVANCED,
                    3
            ));
        }

        // 3. Companies & Opportunities (50 distinct opportunities across 10 companies)
        List<Company> companies = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            companies.add(companyRepository.save(new Company("Company_" + i + "_" + nowEpoch, "comp" + i + ".com", "Tech")));
        }

        List<Opportunity> opportunities = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            Company comp = companies.get(i % companies.size());
            Opportunity opp = opportunityRepository.save(new Opportunity(
                    comp,
                    "Software Engineer Intern " + (i + 1),
                    LocationType.REMOTE,
                    SourceProvider.SYSTEM
            ));
            opportunities.add(opp);

            // Add opportunity skills: 4 skills per opportunity (2 required, 2 preferred)
            int s0 = (i * 4) % 50;
            int s1 = (i * 4 + 1) % 50;
            int s2 = (i * 4 + 2) % 50;
            int s3 = (i * 4 + 3) % 50;
            opportunitySkillRepository.save(new OpportunitySkill(opp, skills.get(s0), true));
            opportunitySkillRepository.save(new OpportunitySkill(opp, skills.get(s1), true));
            opportunitySkillRepository.save(new OpportunitySkill(opp, skills.get(s2), false));
            opportunitySkillRepository.save(new OpportunitySkill(opp, skills.get(s3), false));
        }
        benchmarkOppId = opportunities.get(0).getId();

        // 4. Seed Exactly 300 Applications for Candidate User
        // Distribution:
        // 40 SAVED
        // 80 APPLIED
        // 60 SCREENING
        // 50 INTERVIEWING
        // 20 OFFER
        // 10 ACCEPTED
        // 30 REJECTED
        // 10 WITHDRAWN
        // Total = 300
        Instant baseTime = Instant.now().minusSeconds(86400 * 60);
        List<Application> candidateApps = new ArrayList<>();

        for (int i = 0; i < 300; i++) {
            Opportunity opp = opportunities.get(i);
            ApplicationStatus status;
            Instant savedAt = baseTime.plusSeconds(i * 3600);
            Instant appliedAt = null;
            Instant screeningAt = null;
            Instant interviewingAt = null;
            Instant closedAt = null;

            if (i < 40) {
                status = ApplicationStatus.SAVED;
            } else if (i < 120) {
                status = ApplicationStatus.APPLIED;
                appliedAt = savedAt.plusSeconds(7200);
            } else if (i < 180) {
                status = ApplicationStatus.SCREENING;
                appliedAt = savedAt.plusSeconds(7200);
                screeningAt = appliedAt.plusSeconds(14400);
            } else if (i < 230) {
                status = ApplicationStatus.INTERVIEWING;
                appliedAt = savedAt.plusSeconds(7200);
                screeningAt = appliedAt.plusSeconds(14400);
                interviewingAt = screeningAt.plusSeconds(28800);
            } else if (i < 250) {
                status = ApplicationStatus.OFFER;
                appliedAt = savedAt.plusSeconds(7200);
                screeningAt = appliedAt.plusSeconds(14400);
                interviewingAt = screeningAt.plusSeconds(28800);
            } else if (i < 260) {
                status = ApplicationStatus.ACCEPTED;
                appliedAt = savedAt.plusSeconds(7200);
                screeningAt = appliedAt.plusSeconds(14400);
                interviewingAt = screeningAt.plusSeconds(28800);
                closedAt = interviewingAt.plusSeconds(86400);
            } else if (i < 290) {
                status = ApplicationStatus.REJECTED;
                appliedAt = savedAt.plusSeconds(7200);
                if (i < 275) {
                    screeningAt = appliedAt.plusSeconds(14400);
                }
                if (i < 270) {
                    interviewingAt = screeningAt.plusSeconds(28800);
                }
                closedAt = savedAt.plusSeconds(172800);
            } else {
                status = ApplicationStatus.WITHDRAWN;
                appliedAt = savedAt.plusSeconds(7200);
                closedAt = savedAt.plusSeconds(86400);
            }

            Application app = new Application(candidate, opp, status);
            app.setSavedAt(savedAt);
            app.setAppliedAt(appliedAt);
            app.setScreeningAt(screeningAt);
            app.setInterviewingAt(interviewingAt);
            app.setClosedAt(closedAt);
            candidateApps.add(app);
        }
        candidateApps = applicationRepository.saveAll(candidateApps);

        // 5. Seed Exactly 20 Interview Rounds
        List<Interview> interviews = new ArrayList<>();
        InterviewRoundType[] roundTypes = InterviewRoundType.values();
        for (int i = 0; i < 20; i++) {
            Application app = candidateApps.get(180 + i); // from interviewing / offer group
            Interview round = new Interview(
                    app,
                    (i % 3) + 1,
                    "Round " + (i + 1),
                    roundTypes[i % roundTypes.length],
                    Instant.now().plusSeconds(86400 * (i + 1))
            );
            round.setDurationMinutes(45);
            interviews.add(round);
        }
        interviewRepository.saveAll(interviews);

        // 6. Seed Exactly 30 Reminders
        List<Reminder> reminders = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            ReminderType type = (i % 3 == 0) ? ReminderType.OA_DEADLINE : (i % 3 == 1 ? ReminderType.INTERVIEW_PREP : ReminderType.CUSTOM);
            boolean completed = (i % 2 == 0);
            Application app = candidateApps.get(i * 10);
            reminders.add(new Reminder(
                    candidate,
                    app,
                    "Reminder Task " + i,
                    Instant.now().plusSeconds(3600 * (i + 1)),
                    type
            ));
            reminders.get(i).setCompleted(completed);
        }
        reminderRepository.saveAll(reminders);

        // 7. Seed Isolated User with 5 applications to verify strict user isolation
        for (int i = 0; i < 5; i++) {
            Application isoApp = new Application(isolatedUser, opportunities.get(i), ApplicationStatus.APPLIED);
            isoApp.setAppliedAt(Instant.now());
            applicationRepository.save(isoApp);
        }

        dataSeeded = true;
        log.info("M12 benchmark fixture seeded: 300 candidate apps, 20 interviews, 30 reminders, 50 skills, 5 isolated apps.");
    }

    @Test
    @Order(1)
    @DisplayName("Verify Schema & Index Definitions")
    void testSchemaAndIndexIntegrity() {
        // Verify Application entity indexes
        jakarta.persistence.Table appTable = Application.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(appTable).isNotNull();
        boolean hasAppUpdatedIndex = Arrays.stream(appTable.indexes())
                .anyMatch(idx -> "idx_app_user_updated".equals(idx.name()) && "user_id, updated_at".equals(idx.columnList()));
        assertThat(hasAppUpdatedIndex)
                .as("Application entity must define idx_app_user_updated on (user_id, updated_at)")
                .isTrue();

        // Verify Reminder entity indexes
        jakarta.persistence.Table reminderTable = Reminder.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(reminderTable).isNotNull();
        boolean hasReminderTypeCompIndex = Arrays.stream(reminderTable.indexes())
                .anyMatch(idx -> "idx_reminder_user_type_comp".equals(idx.name()) && "user_id, completed, reminder_type".equals(idx.columnList()));
        assertThat(hasReminderTypeCompIndex)
                .as("Reminder entity must define idx_reminder_user_type_comp on (user_id, completed, reminder_type)")
                .isTrue();

        // Verify database index existence via JDBC
        List<Map<String, Object>> appIndexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(TABLE_NAME) = 'APPLICATIONS' AND UPPER(INDEX_NAME) = 'IDX_APP_USER_UPDATED'"
        );
        assertThat(appIndexes)
                .as("Database schema must contain index idx_app_user_updated")
                .isNotEmpty();

        List<Map<String, Object>> reminderIndexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(TABLE_NAME) = 'REMINDERS' AND UPPER(INDEX_NAME) = 'IDX_REMINDER_USER_TYPE_COMP'"
        );
        assertThat(reminderIndexes)
                .as("Database schema must contain index idx_reminder_user_type_comp")
                .isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("Verify Funnel Analytics Consolidated Query Correctness")
    void testFunnelAnalyticsCorrectness() {
        FunnelAnalyticsResponse funnel = analyticsService.getFunnelAnalytics(candidateUserId);

        assertThat(funnel).isNotNull();
        // Fixture: all 300 apps have savedAt
        assertThat(funnel.totalSaved()).isEqualTo(300);
        // 300 - 40 SAVED = 260 have appliedAt
        assertThat(funnel.totalApplied()).isEqualTo(260);
        // SCREENING (60) + INTERVIEWING (50) + OFFER (20) + ACCEPTED (10) + REJECTED with screening (15) = 155
        // Let's verify exact single query result:
        FunnelStageCounts counts = applicationRepository.getFunnelStageCounts(candidateUserId, Set.of(ApplicationStatus.OFFER, ApplicationStatus.ACCEPTED));
        assertThat(funnel.totalSaved()).isEqualTo((int) counts.totalSaved());
        assertThat(funnel.totalApplied()).isEqualTo((int) counts.totalApplied());
        assertThat(funnel.totalScreening()).isEqualTo((int) counts.totalScreening());
        assertThat(funnel.totalInterviewing()).isEqualTo((int) counts.totalInterviewing());
        assertThat(funnel.totalOffer()).isEqualTo((int) counts.totalOffer());
        assertThat(funnel.totalOffer()).isEqualTo(30); // 20 OFFER + 10 ACCEPTED

        // Check conversion rates formulas
        double expectedAppliedConv = Math.round((260.0 / 300.0 * 100.0) * 100.0) / 100.0;
        assertThat(funnel.appliedConversionRate()).isEqualTo(expectedAppliedConv);

        // Verify user isolation: isolated user has 5 applications, candidate has 300
        FunnelAnalyticsResponse isolatedFunnel = analyticsService.getFunnelAnalytics(isolatedUserId);
        assertThat(isolatedFunnel.totalSaved()).isEqualTo(5);
        assertThat(isolatedFunnel.totalApplied()).isEqualTo(5);
        assertThat(isolatedFunnel.totalOffer()).isEqualTo(0);
    }

    @Test
    @Order(3)
    @DisplayName("Verify Dashboard Summary Query Consolidation Correctness")
    void testDashboardSummaryCorrectness() {
        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(candidateUserId);

        assertThat(summary).isNotNull();
        assertThat(summary.totalApplications()).isEqualTo(300);
        // Active = SAVED (40) + APPLIED (80) + SCREENING (60) + INTERVIEWING (50) = 230
        assertThat(summary.activeApplications()).isEqualTo(230);
        // Offers = OFFER (20) + ACCEPTED (10) = 30
        assertThat(summary.offersReceived()).isEqualTo(30);

        // Breakdown matches all 8 statuses
        Map<String, Integer> counts = summary.statusCounts();
        assertThat(counts.get("SAVED")).isEqualTo(40);
        assertThat(counts.get("APPLIED")).isEqualTo(80);
        assertThat(counts.get("SCREENING")).isEqualTo(60);
        assertThat(counts.get("INTERVIEWING")).isEqualTo(50);
        assertThat(counts.get("OFFER")).isEqualTo(20);
        assertThat(counts.get("ACCEPTED")).isEqualTo(10);
        assertThat(counts.get("REJECTED")).isEqualTo(30);
        assertThat(counts.get("WITHDRAWN")).isEqualTo(10);

        // Sum of breakdown equals total
        int breakdownSum = counts.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(breakdownSum).isEqualTo(300);

        // Isolated user check
        DashboardSummaryResponse isoSummary = analyticsService.getDashboardSummary(isolatedUserId);
        assertThat(isoSummary.totalApplications()).isEqualTo(5);
        assertThat(isoSummary.activeApplications()).isEqualTo(5);
        assertThat(isoSummary.offersReceived()).isEqualTo(0);
    }

    @Test
    @Order(4)
    @DisplayName("Verify Skill Deficits ID Projection Correctness & User Isolation")
    void testSkillDeficitsProjectionCorrectness() {
        // Direct distinct opportunity IDs query
        List<Long> projectedOppIds = applicationRepository.findDistinctOpportunityIdsByUserId(candidateUserId);

        // 300 applications for candidate map to 300 distinct opportunities
        assertThat(projectedOppIds).hasSize(300);
        assertThat(projectedOppIds).doesNotHaveDuplicates();

        // Compare against full entity mapping to ensure exact duplicate elimination
        List<Long> entityOppIds = applicationRepository.findByUserId(candidateUserId).stream()
                .map(a -> a.getOpportunity().getId())
                .distinct()
                .toList();
        assertThat(projectedOppIds).containsExactlyInAnyOrderElementsOf(entityOppIds);

        // Compute deficits
        SkillDeficitsResponse deficits = analyticsService.getSkillDeficits(candidateUserId);
        assertThat(deficits).isNotNull();
        assertThat(deficits.totalTrackedOpportunities()).isEqualTo(300);
        assertThat(deficits.candidateSkillCount()).isEqualTo(15);
        assertThat(deficits.topDeficits()).isNotEmpty();
        assertThat(deficits.topDeficits().size()).isLessThanOrEqualTo(10);

        // Deficits are ordered descending by opportunityCount
        List<Integer> counts = deficits.topDeficits().stream().map(d -> d.opportunityCount()).toList();
        for (int i = 0; i < counts.size() - 1; i++) {
            assertThat(counts.get(i)).isGreaterThanOrEqualTo(counts.get(i + 1));
        }

        // Isolated user has distinct opp IDs
        List<Long> isoOppIds = applicationRepository.findDistinctOpportunityIdsByUserId(isolatedUserId);
        assertThat(isoOppIds).hasSize(5);
    }

    @Test
    @Order(5)
    @DisplayName("Benchmark Target 1: Dashboard KPI Execution (< 250ms)")
    void benchmarkDashboardKpiExecution() {
        int warmup = 5;
        int samples = 20;
        List<Long> durations = new ArrayList<>();

        // Warm up
        for (int i = 0; i < warmup; i++) {
            analyticsService.getDashboardSummary(candidateUserId);
        }

        // Timed runs
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            DashboardSummaryResponse summary = analyticsService.getDashboardSummary(candidateUserId);
            long end = System.nanoTime();
            assertThat(summary.totalApplications()).isEqualTo(300);
            durations.add((end - start) / 1_000_000); // ms
        }

        durations.sort(Long::compareTo);
        long min = durations.get(0);
        long median = durations.get(samples / 2);
        long max = durations.get(samples - 1);
        double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);

        log.info("[BENCHMARK REPORT] Dashboard KPI Execution over {} samples: min={}ms, median={}ms, avg={}ms, max={}ms (Target: < 250ms)",
                samples, min, median, String.format(Locale.ROOT, "%.2f", avg), max);

        assertThat(median)
                .as("Dashboard KPI median execution time must meet < 250ms target")
                .isLessThan(250L);
    }

    @Test
    @Order(6)
    @DisplayName("Benchmark Target 2: Paginated Application Retrieval for 50 records (< 150ms)")
    void benchmarkPaginatedApplicationRetrieval() {
        int warmup = 5;
        int samples = 20;
        List<Long> durations = new ArrayList<>();

        // Warm up
        for (int i = 0; i < warmup; i++) {
            List<Application> apps = applicationRepository.findByUserIdAndOptionalStatusWithDetails(candidateUserId, null);
            if (apps.size() > 50) {
                apps = apps.subList(0, 50);
            }
        }

        // Timed runs
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            List<Application> apps = applicationRepository.findByUserIdAndOptionalStatusWithDetails(candidateUserId, null);
            List<Application> first50 = apps.subList(0, Math.min(50, apps.size()));
            long end = System.nanoTime();
            assertThat(first50).hasSize(50);
            durations.add((end - start) / 1_000_000);
        }

        durations.sort(Long::compareTo);
        long min = durations.get(0);
        long median = durations.get(samples / 2);
        long max = durations.get(samples - 1);
        double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);

        log.info("[BENCHMARK REPORT] Paginated Application Retrieval (50 records) over {} samples: min={}ms, median={}ms, avg={}ms, max={}ms (Target: < 150ms)",
                samples, min, median, String.format(Locale.ROOT, "%.2f", avg), max);

        assertThat(median)
                .as("Paginated application retrieval median time must meet < 150ms target")
                .isLessThan(150L);
    }

    @Test
    @Order(7)
    @DisplayName("Benchmark Target 3: Skill-Match Calculation (< 10ms)")
    void benchmarkSkillMatchCalculation() {
        int warmup = 5;
        int samples = 20;
        List<Double> durationsMs = new ArrayList<>();

        // Warm up
        for (int i = 0; i < warmup; i++) {
            skillService.getOpportunitySkillMatch(candidateUserId, benchmarkOppId);
        }

        // Timed runs
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            OpportunitySkillMatchResponse match = skillService.getOpportunitySkillMatch(candidateUserId, benchmarkOppId);
            long end = System.nanoTime();
            assertThat(match).isNotNull();
            durationsMs.add((end - start) / 1_000_000.0); // ms as double
        }

        durationsMs.sort(Double::compareTo);
        double min = durationsMs.get(0);
        double median = durationsMs.get(samples / 2);
        double max = durationsMs.get(samples - 1);
        double avg = durationsMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        log.info("[BENCHMARK REPORT] Skill-Match Calculation over {} samples: min={}ms, median={}ms, avg={}ms, max={}ms (Target: < 10ms)",
                samples, String.format(Locale.ROOT, "%.3f", min), String.format(Locale.ROOT, "%.3f", median), String.format(Locale.ROOT, "%.3f", avg), String.format(Locale.ROOT, "%.3f", max));

        assertThat(median)
                .as("Skill-match calculation median time must meet < 10ms target")
                .isLessThan(10.0);
    }

    @Test
    @Order(8)
    @DisplayName("Benchmark Target 3b: Skill Deficits Distinct Projection (< 10ms)")
    void benchmarkSkillDeficitsDistinctProjection() {
        int warmup = 5;
        int samples = 20;
        List<Double> durationsMs = new ArrayList<>();

        // Warm up
        for (int i = 0; i < warmup; i++) {
            applicationRepository.findDistinctOpportunityIdsByUserId(candidateUserId);
        }

        // Timed runs
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            List<Long> ids = applicationRepository.findDistinctOpportunityIdsByUserId(candidateUserId);
            long end = System.nanoTime();
            assertThat(ids).hasSize(300);
            durationsMs.add((end - start) / 1_000_000.0);
        }

        durationsMs.sort(Double::compareTo);
        double min = durationsMs.get(0);
        double median = durationsMs.get(samples / 2);
        double max = durationsMs.get(samples - 1);
        double avg = durationsMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        log.info("[BENCHMARK REPORT] Skill Deficits Distinct Projection over {} samples: min={}ms, median={}ms, avg={}ms, max={}ms (Target: < 10ms)",
                samples, String.format(Locale.ROOT, "%.3f", min), String.format(Locale.ROOT, "%.3f", median), String.format(Locale.ROOT, "%.3f", avg), String.format(Locale.ROOT, "%.3f", max));

        assertThat(median)
                .as("Skill deficits distinct projection median time must meet < 10ms target")
                .isLessThan(10.0);
    }

    @AfterAll
    void cleanUp() {
        reminderRepository.deleteAll();
        interviewRepository.deleteAll();
        opportunitySkillRepository.deleteAll();
        userSkillRepository.deleteAll();
        applicationRepository.deleteAll();
        opportunityRepository.deleteAll();
        companyRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();
    }
}
