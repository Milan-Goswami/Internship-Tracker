package com.interntrack.api.dto.skill;

import com.interntrack.api.entity.Skill;
import com.interntrack.api.entity.enums.SkillCategory;

public record SkillResponse(
        Long id,
        String name,
        String canonicalName,
        SkillCategory category
) {
    public static SkillResponse from(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getCanonicalName(),
                skill.getCategory()
        );
    }
}