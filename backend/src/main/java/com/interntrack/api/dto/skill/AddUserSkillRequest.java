package com.interntrack.api.dto.skill;

import com.interntrack.api.entity.enums.ProficiencyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddUserSkillRequest(
        @NotNull(message = "Skill ID is required")
        Long skillId,

        ProficiencyLevel proficiencyLevel,

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 50, message = "Years of experience cannot exceed 50")
        Integer yearsOfExperience
) {
}