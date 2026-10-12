package com.interntrack.api.dto.skill;

import java.util.List;

public record OpportunitySkillMatchResponse(
        Long opportunityId,
        Double matchPercentage,
        String matchLabel,
        List<String> matchedRequiredSkills,
        List<String> missingRequiredSkills,
        List<String> matchedPreferredSkills,
        List<String> missingPreferredSkills,
        int totalRequiredSkills,
        int totalPreferredSkills,
        boolean zeroSkillsSpecified
) {
}