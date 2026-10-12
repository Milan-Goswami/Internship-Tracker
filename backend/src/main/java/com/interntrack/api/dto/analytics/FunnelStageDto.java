package com.interntrack.api.dto.analytics;

public record FunnelStageDto(
        String stageName,
        int count,
        double conversionFromPrevious
) {
}