package com.interntrack.api.service;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable value representation of a deterministic skill match calculation.
 */
public record SkillMatchResult(
        Integer matchPercentage,
        String matchLabel,
        Set<String> matchedRequiredSkills,
        Set<String> missingRequiredSkills,
        Set<String> matchedPreferredSkills,
        Set<String> missingPreferredSkills,
        int totalRequiredSkills,
        int totalPreferredSkills,
        boolean zeroSkillsSpecified
) {
    public SkillMatchResult {
        matchedRequiredSkills = matchedRequiredSkills != null ? Collections.unmodifiableSet(matchedRequiredSkills) : Collections.emptySet();
        missingRequiredSkills = missingRequiredSkills != null ? Collections.unmodifiableSet(missingRequiredSkills) : Collections.emptySet();
        matchedPreferredSkills = matchedPreferredSkills != null ? Collections.unmodifiableSet(matchedPreferredSkills) : Collections.emptySet();
        missingPreferredSkills = missingPreferredSkills != null ? Collections.unmodifiableSet(missingPreferredSkills) : Collections.emptySet();
    }
}