package com.interntrack.api.dto.skill;

import com.interntrack.api.entity.enums.SkillCategory;

public record SkillDeficitDto(
        Long skillId,
        String skillName,
        String canonicalName,
        SkillCategory category,
        int opportunityCount,
        double frequencyPercentage
) {
    public SkillDeficitDto(String skillName, String canonicalName, SkillCategory category, int opportunityCount, double frequencyPercentage) {
        this(null, skillName, canonicalName, category, opportunityCount, frequencyPercentage);
    }
}
