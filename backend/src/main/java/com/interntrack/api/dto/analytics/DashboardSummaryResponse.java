package com.interntrack.api.dto.analytics;

import com.interntrack.api.dto.reminder.ReminderResponse;

import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        int totalApplications,
        int activeApplications,
        double interviewRate,
        int pendingOAs,
        int offersReceived,
        Map<String, Integer> statusCounts,
        List<ReminderResponse> urgentReminders,
        List<UpcomingInterviewDto> upcomingInterviews
) {
}