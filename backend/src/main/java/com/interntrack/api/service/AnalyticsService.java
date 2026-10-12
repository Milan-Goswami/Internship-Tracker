package com.interntrack.api.service;

import com.interntrack.api.dto.analytics.DashboardSummaryResponse;
import com.interntrack.api.dto.analytics.FunnelAnalyticsResponse;
import com.interntrack.api.dto.analytics.FunnelStageCounts;
import com.interntrack.api.dto.analytics.FunnelStageDto;
import com.interntrack.api.dto.analytics.UpcomingInterviewDto;
import com.interntrack.api.dto.reminder.ReminderResponse;
import com.interntrack.api.dto.skill.SkillDeficitDto;
import com.interntrack.api.dto.skill.SkillDeficitsResponse;
import com.interntrack.api.entity.Interview;
import com.interntrack.api.entity.OpportunitySkill;
import com.interntrack.api.entity.Reminder;
import com.interntrack.api.entity.Skill;
import com.interntrack.api.entity.UserSkill;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.ReminderType;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.InterviewRepository;
import com.interntrack.api.repository.OpportunitySkillRepository;
import com.interntrack.api.repository.ReminderRepository;
import com.interntrack.api.repository.UserSkillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Set<ApplicationStatus> ACTIVE_STATUSES = Set.of(
            ApplicationStatus.SAVED,
            ApplicationStatus.APPLIED,
            ApplicationStatus.SCREENING,
            ApplicationStatus.INTERVIEWING
    );

    private static final Set<ApplicationStatus> OFFER_STATUSES = Set.of(
            ApplicationStatus.OFFER,
            ApplicationStatus.ACCEPTED
    );

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final ReminderRepository reminderRepository;
    private final UserSkillRepository userSkillRepository;
    private final OpportunitySkillRepository opportunitySkillRepository;

    public AnalyticsService(ApplicationRepository applicationRepository,
                            InterviewRepository interviewRepository,
                            ReminderRepository reminderRepository,
                            UserSkillRepository userSkillRepository,
                            OpportunitySkillRepository opportunitySkillRepository) {
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.reminderRepository = reminderRepository;
        this.userSkillRepository = userSkillRepository;
        this.opportunitySkillRepository = opportunitySkillRepository;
    }

    /**
     * Computes the command center dashboard KPIs for the authenticated candidate.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Status counts breakdown and derived status-based totals
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            statusCounts.put(status.name(), 0);
        }
        List<Object[]> groupedCounts = applicationRepository.countGroupByStatusForUser(userId);
        int totalApplications = 0;
        int activeApplications = 0;
        int offersReceived = 0;
        for (Object[] row : groupedCounts) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            int count = ((Number) row[1]).intValue();
            statusCounts.put(status.name(), count);
            totalApplications += count;
            if (ACTIVE_STATUSES.contains(status)) {
                activeApplications += count;
            }
            if (OFFER_STATUSES.contains(status)) {
                offersReceived += count;
            }
        }

        long appliedCount = applicationRepository.countByUserIdAndAppliedAtIsNotNull(userId);
        long interviewingCount = applicationRepository.countByUserIdAndInterviewingAtIsNotNull(userId);

        double interviewRate = 0.0;
        if (appliedCount > 0) {
            interviewRate = round((double) interviewingCount / appliedCount * 100.0);
        }

        int pendingOAs = (int) reminderRepository.countByUserIdAndCompletedFalseAndReminderType(userId, ReminderType.OA_DEADLINE);

        // Urgent reminders (next 5 incomplete, ordered by dueAt ASC)
        List<Reminder> reminders = reminderRepository.findTop5ByUserIdAndCompletedFalseOrderByDueAtAsc(userId);
        List<ReminderResponse> urgentReminders = reminders.stream()
                .map(ReminderResponse::from)
                .toList();

        // Upcoming interviews (next 3 scheduled at >= NOW, ordered by scheduledAt ASC)
        List<Interview> upcoming = interviewRepository.findUpcomingByUserId(userId, Instant.now(), PageRequest.of(0, 3));
        List<UpcomingInterviewDto> upcomingInterviews = upcoming.stream()
                .map(UpcomingInterviewDto::from)
                .toList();

        return new DashboardSummaryResponse(
                totalApplications,
                activeApplications,
                interviewRate,
                pendingOAs,
                offersReceived,
                statusCounts,
                urgentReminders,
                upcomingInterviews
        );
    }

    /**
     * Computes conversion funnel metrics based on first-entry stage timestamps and terminal offer statuses.
     */
    @Transactional(readOnly = true)
    public FunnelAnalyticsResponse getFunnelAnalytics(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        FunnelStageCounts counts = applicationRepository.getFunnelStageCounts(userId, OFFER_STATUSES);
        int totalSaved = counts != null ? (int) counts.totalSaved() : 0;
        int totalApplied = counts != null ? (int) counts.totalApplied() : 0;
        int totalScreening = counts != null ? (int) counts.totalScreening() : 0;
        int totalInterviewing = counts != null ? (int) counts.totalInterviewing() : 0;
        int totalOffer = counts != null ? (int) counts.totalOffer() : 0;

        double appliedConversion = totalSaved > 0 ? round((double) totalApplied / totalSaved * 100.0) : 0.0;
        double screeningConversion = totalApplied > 0 ? round((double) totalScreening / totalApplied * 100.0) : 0.0;
        double interviewingConversion = totalScreening > 0 ? round((double) totalInterviewing / totalScreening * 100.0) : 0.0;
        double offerConversion = totalInterviewing > 0 ? round((double) totalOffer / totalInterviewing * 100.0) : 0.0;
        double overallConversion = totalApplied > 0 ? round((double) totalOffer / totalApplied * 100.0) : 0.0;

        List<FunnelStageDto> stages = List.of(
                new FunnelStageDto("SAVED", totalSaved, 100.0),
                new FunnelStageDto("APPLIED", totalApplied, appliedConversion),
                new FunnelStageDto("SCREENING", totalScreening, screeningConversion),
                new FunnelStageDto("INTERVIEWING", totalInterviewing, interviewingConversion),
                new FunnelStageDto("OFFER", totalOffer, offerConversion)
        );

        return new FunnelAnalyticsResponse(
                totalSaved,
                totalApplied,
                totalScreening,
                totalInterviewing,
                totalOffer,
                appliedConversion,
                screeningConversion,
                interviewingConversion,
                offerConversion,
                overallConversion,
                stages
        );
    }

    /**
     * Computes top skill deficits across all tracked opportunities for the authenticated candidate.
     */
    @Transactional(readOnly = true)
    public SkillDeficitsResponse getSkillDeficits(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        List<Long> oppIds = applicationRepository.findDistinctOpportunityIdsByUserId(userId);

        List<UserSkill> userSkills = userSkillRepository.findByUserIdFetchSkill(userId);
        Set<String> candidateCanonicalSkills = userSkills.stream()
                .map(us -> us.getSkill().getCanonicalName())
                .collect(Collectors.toSet());

        int totalTrackedOpportunities = oppIds.size();
        int candidateSkillCount = candidateCanonicalSkills.size();

        if (oppIds.isEmpty()) {
            return new SkillDeficitsResponse(0, candidateSkillCount, Collections.emptyList());
        }

        List<OpportunitySkill> oppSkills = opportunitySkillRepository.findByOpportunityIdInFetchSkill(oppIds);

        Map<String, Skill> skillMap = new HashMap<>();
        Map<String, Set<Long>> oppCountMap = new HashMap<>();

        for (OpportunitySkill os : oppSkills) {
            Skill s = os.getSkill();
            String canonical = s.getCanonicalName();
            if (!candidateCanonicalSkills.contains(canonical)) {
                skillMap.putIfAbsent(canonical, s);
                oppCountMap.computeIfAbsent(canonical, k -> new HashSet<>()).add(os.getOpportunity().getId());
            }
        }

        List<SkillDeficitDto> deficits = new ArrayList<>();
        for (Map.Entry<String, Set<Long>> entry : oppCountMap.entrySet()) {
            String canonical = entry.getKey();
            int oppCount = entry.getValue().size();
            double freqPercent = round(((double) oppCount / totalTrackedOpportunities) * 100.0);
            Skill s = skillMap.get(canonical);
            deficits.add(new SkillDeficitDto(
                    s.getId(),
                    s.getName(),
                    canonical,
                    s.getCategory(),
                    oppCount,
                    freqPercent
            ));
        }

        deficits.sort(Comparator.comparingInt(SkillDeficitDto::opportunityCount).reversed()
                .thenComparing(SkillDeficitDto::canonicalName));

        List<SkillDeficitDto> top10 = deficits.stream().limit(10).toList();

        return new SkillDeficitsResponse(totalTrackedOpportunities, candidateSkillCount, top10);
    }

    private static double round(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return 0.0;
        }
        double bounded = Math.max(0.0, Math.min(100.0, val));
        return BigDecimal.valueOf(bounded)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
