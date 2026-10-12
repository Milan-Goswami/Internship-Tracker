package com.interntrack.api.service;

import com.interntrack.api.dto.skill.OpportunitySkillMatchResponse;
import com.interntrack.api.entity.OpportunitySkill;
import com.interntrack.api.entity.Skill;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillMatchingService {

    public OpportunitySkillMatchResponse calculateMatch(
            Long opportunityId,
            Set<String> candidateCanonicalSkills,
            List<OpportunitySkill> opportunitySkills) {

        if (candidateCanonicalSkills == null) {
            candidateCanonicalSkills = Collections.emptySet();
        }
        if (opportunitySkills == null) {
            opportunitySkills = Collections.emptyList();
        }

        Set<String> normalizedCandidateSkills = candidateCanonicalSkills.stream()
                .filter(Objects::nonNull)
                .map(Skill::normalizeCanonicalName)
                .collect(Collectors.toSet());

        List<String> matchedRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        List<String> matchedPreferred = new ArrayList<>();
        List<String> missingPreferred = new ArrayList<>();

        for (OpportunitySkill oppSkill : opportunitySkills) {
            Skill skill = oppSkill.getSkill();
            String rawCanonical = skill != null ? skill.getCanonicalName() : null;
            String canonical = rawCanonical != null ? Skill.normalizeCanonicalName(rawCanonical)
                    : (skill != null ? Skill.normalizeCanonicalName(skill.getName()) : null);
            String displayName = skill != null ? skill.getName() : "Unknown Skill";
            boolean matched = canonical != null && normalizedCandidateSkills.contains(canonical);

            if (Boolean.TRUE.equals(oppSkill.getIsRequired())) {
                if (matched) {
                    matchedRequired.add(displayName);
                } else {
                    missingRequired.add(displayName);
                }
            } else {
                if (matched) {
                    matchedPreferred.add(displayName);
                } else {
                    missingPreferred.add(displayName);
                }
            }
        }

        int totalReq = matchedRequired.size() + missingRequired.size();
        int totalPref = matchedPreferred.size() + missingPreferred.size();

        if (totalReq == 0 && totalPref == 0) {
            return new OpportunitySkillMatchResponse(
                    opportunityId,
                    null,
                    "No Skills Specified",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    0,
                    true
            );
        }

        double score;
        if (totalPref == 0) {
            score = ((double) matchedRequired.size() / totalReq) * 100.0;
        } else if (totalReq == 0) {
            score = ((double) matchedPreferred.size() / totalPref) * 100.0;
        } else {
            double reqRatio = (double) matchedRequired.size() / totalReq;
            double prefRatio = (double) matchedPreferred.size() / totalPref;
            score = (reqRatio * 0.75 + prefRatio * 0.25) * 100.0;
        }

        double matchPercentage = round(score);
        String matchLabel;
        if (matchPercentage >= 80.0) {
            matchLabel = "HIGH";
        } else if (matchPercentage >= 50.0) {
            matchLabel = "MEDIUM";
        } else if (matchPercentage > 0.0) {
            matchLabel = "LOW";
        } else {
            matchLabel = "NONE";
        }

        return new OpportunitySkillMatchResponse(
                opportunityId,
                matchPercentage,
                matchLabel,
                matchedRequired,
                missingRequired,
                matchedPreferred,
                missingPreferred,
                totalReq,
                totalPref,
                false
        );
    }

    private static double round(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return 0.0;
        }
        double bounded = Math.max(0.0, Math.min(100.0, val));
        return BigDecimal.valueOf(bounded)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}