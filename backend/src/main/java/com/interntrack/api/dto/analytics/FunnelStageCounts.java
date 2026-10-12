package com.interntrack.api.dto.analytics;

/**
 * Aggregated first-entry stage timestamp counts and offer stage count
 * for computing candidate conversion funnels in a single query.
 */
public record FunnelStageCounts(
        long totalSaved,
        long totalApplied,
        long totalScreening,
        long totalInterviewing,
        long totalOffer
) {
}
