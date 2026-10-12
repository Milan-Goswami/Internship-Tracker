package com.interntrack.api.dto.skill;

import java.util.List;

public record SkillDeficitsResponse(
        int totalTrackedOpportunities,
        int candidateSkillCount,
        List<SkillDeficitDto> topDeficits
) {
}