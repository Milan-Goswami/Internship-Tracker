package com.interntrack.api.dto.analytics;

import java.util.List;

public record FunnelAnalyticsResponse(
        int totalSaved,
        int totalApplied,
        int totalScreening,
        int totalInterviewing,
        int totalOffer,
        double appliedConversionRate,
        double screeningConversionRate,
        double interviewingConversionRate,
        double offerConversionRate,
        double overallConversionRate,
        List<FunnelStageDto> stages
) {
}