package com.interntrack.api.dto.skill;

import com.interntrack.api.entity.UserSkill;
import com.interntrack.api.entity.enums.ProficiencyLevel;
import com.interntrack.api.entity.enums.SkillCategory;

public record UserSkillResponse(
        Long skillId,
        String skillName,
        String canonicalName,
        SkillCategory category,
        ProficiencyLevel proficiencyLevel,
        Integer yearsOfExperience
) {
    public static UserSkillResponse from(UserSkill userSkill) {
        return new UserSkillResponse(
                userSkill.getSkill().getId(),
                userSkill.getSkill().getName(),
                userSkill.getSkill().getCanonicalName(),
                userSkill.getSkill().getCategory(),
                userSkill.getProficiencyLevel(),
                userSkill.getYearsOfExperience()
        );
    }
}