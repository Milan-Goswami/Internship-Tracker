package com.interntrack.api.service;

import com.interntrack.api.dto.skill.OpportunitySkillMatchResponse;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.OpportunitySkill;
import com.interntrack.api.entity.Skill;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.SkillCategory;
import com.interntrack.api.entity.enums.SourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillMatchingServiceTest {

    private SkillMatchingService matchingService;
    private Opportunity testOpportunity;
    private Skill javaSkill;
    private Skill springSkill;
    private Skill dockerSkill;
    private Skill k8sSkill;

    @BeforeEach
    void setUp() {
        matchingService = new SkillMatchingService();

        Company company = new Company("Tech Corp", "techcorp.com", "Tech");
        testOpportunity = new Opportunity(company, "Software Engineer Intern", LocationType.HYBRID, SourceProvider.USER_MANUAL);

        javaSkill = new Skill("Java", SkillCategory.LANGUAGE);
        springSkill = new Skill("Spring Boot", SkillCategory.FRAMEWORK);
        dockerSkill = new Skill("Docker", SkillCategory.TOOL);
        k8sSkill = new Skill("Kubernetes", SkillCategory.CLOUD);
    }

    // ========================================================================
    // Case D: Zero Skills Specified (|S_req| = 0 and |S_pref| = 0)
    // ========================================================================

    @Test
    @DisplayName("Case D: 0 required and 0 preferred returns null matchPercentage, 'No Skills Specified' label, and zeroSkillsSpecified = true")
    void calculateMatch_caseD_zeroSkills_returnsNullAndNoSkillsSpecified() {
        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("java"), Collections.emptyList());

        assertThat(res.matchPercentage()).isNull();
        assertThat(res.matchLabel()).isEqualTo("No Skills Specified");
        assertThat(res.zeroSkillsSpecified()).isTrue();
        assertThat(res.totalRequiredSkills()).isEqualTo(0);
        assertThat(res.totalPreferredSkills()).isEqualTo(0);
        assertThat(res.matchedRequiredSkills()).isEmpty();
        assertThat(res.missingRequiredSkills()).isEmpty();
        assertThat(res.matchedPreferredSkills()).isEmpty();
        assertThat(res.missingPreferredSkills()).isEmpty();
    }

    @Test
    @DisplayName("Null inputs are handled gracefully and treated as Case D without NPE")
    void calculateMatch_nullInputs_handledGracefullyAsCaseD() {
        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, null, null);

        assertThat(res.matchPercentage()).isNull();
        assertThat(res.matchLabel()).isEqualTo("No Skills Specified");
        assertThat(res.zeroSkillsSpecified()).isTrue();
        assertThat(res.totalRequiredSkills()).isEqualTo(0);
        assertThat(res.totalPreferredSkills()).isEqualTo(0);
    }

    // ========================================================================
    // Case A: Both Required and Preferred (|S_req| > 0 and |S_pref| > 0)
    // Formula: OverallScore = round(0.75 * RequiredScore + 0.25 * PreferredScore) * 100
    // ========================================================================

    @Test
    @DisplayName("Case A: Both required (75% weight) and preferred (25% weight) computes 62.50%")
    void calculateMatch_caseA_bothRequiredAndPreferred_75_25_weighting() {
        // 2 required: Java (matched), Spring Boot (missing) -> 1/2 = 50% * 0.75 = 37.5%
        // 1 preferred: Docker (matched) -> 1/1 = 100% * 0.25 = 25.0%
        // Total = 37.5% + 25.0% = 62.50% -> MEDIUM
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, javaSkill, true),
                new OpportunitySkill(testOpportunity, springSkill, true),
                new OpportunitySkill(testOpportunity, dockerSkill, false)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("java", "docker"), oppSkills);

        assertThat(res.matchPercentage()).isEqualTo(62.50);
        assertThat(res.matchLabel()).isEqualTo("MEDIUM");
        assertThat(res.zeroSkillsSpecified()).isFalse();
        assertThat(res.totalRequiredSkills()).isEqualTo(2);
        assertThat(res.totalPreferredSkills()).isEqualTo(1);
        assertThat(res.matchedRequiredSkills()).containsExactly("Java");
        assertThat(res.missingRequiredSkills()).containsExactly("Spring Boot");
        assertThat(res.matchedPreferredSkills()).containsExactly("Docker");
        assertThat(res.missingPreferredSkills()).isEmpty();
    }

    @Test
    @DisplayName("Case A: 100% on both required and preferred computes 100.0% HIGH")
    void calculateMatch_caseA_allMatched_returns100() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, javaSkill, true),
                new OpportunitySkill(testOpportunity, dockerSkill, false)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("java", "docker"), oppSkills);

        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchLabel()).isEqualTo("HIGH");
        assertThat(res.zeroSkillsSpecified()).isFalse();
    }

    // ========================================================================
    // Case B: Required Only (|S_req| > 0 and |S_pref| = 0)
    // Formula: OverallScore = round(RequiredScore) * 100
    // ========================================================================

    @Test
    @DisplayName("Case B: Only required skills computes 100% when all matched")
    void calculateMatch_caseB_onlyRequired_allMatched() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, javaSkill, true),
                new OpportunitySkill(testOpportunity, springSkill, true)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("java", "springboot"), oppSkills);

        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchLabel()).isEqualTo("HIGH");
        assertThat(res.zeroSkillsSpecified()).isFalse();
        assertThat(res.matchedRequiredSkills()).containsExactlyInAnyOrder("Java", "Spring Boot");
        assertThat(res.missingRequiredSkills()).isEmpty();
        assertThat(res.totalRequiredSkills()).isEqualTo(2);
        assertThat(res.totalPreferredSkills()).isEqualTo(0);
    }

    @Test
    @DisplayName("Case B: Only required skills computes 50% when half matched")
    void calculateMatch_caseB_onlyRequired_halfMatched() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, javaSkill, true),
                new OpportunitySkill(testOpportunity, springSkill, true)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("java"), oppSkills);

        assertThat(res.matchPercentage()).isEqualTo(50.0);
        assertThat(res.matchLabel()).isEqualTo("MEDIUM");
        assertThat(res.matchedRequiredSkills()).containsExactly("Java");
        assertThat(res.missingRequiredSkills()).containsExactly("Spring Boot");
        assertThat(res.totalRequiredSkills()).isEqualTo(2);
        assertThat(res.totalPreferredSkills()).isEqualTo(0);
    }

    // ========================================================================
    // Case C: Preferred Only (|S_req| = 0 and |S_pref| > 0)
    // Formula: OverallScore = round(PreferredScore) * 100
    // ========================================================================

    @Test
    @DisplayName("Case C: Only preferred skills computes 50% when half matched")
    void calculateMatch_caseC_onlyPreferred_halfMatched() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, dockerSkill, false),
                new OpportunitySkill(testOpportunity, k8sSkill, false)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("docker"), oppSkills);

        assertThat(res.matchPercentage()).isEqualTo(50.0);
        assertThat(res.matchLabel()).isEqualTo("MEDIUM");
        assertThat(res.zeroSkillsSpecified()).isFalse();
        assertThat(res.totalRequiredSkills()).isEqualTo(0);
        assertThat(res.totalPreferredSkills()).isEqualTo(2);
        assertThat(res.matchedPreferredSkills()).containsExactly("Docker");
        assertThat(res.missingPreferredSkills()).containsExactly("Kubernetes");
    }

    @Test
    @DisplayName("Case C: Only preferred skills computes 100% when all matched")
    void calculateMatch_caseC_onlyPreferred_allMatched() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, dockerSkill, false),
                new OpportunitySkill(testOpportunity, k8sSkill, false)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("docker", "kubernetes"), oppSkills);

        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchLabel()).isEqualTo("HIGH");
    }

    // ========================================================================
    // Zero Matches (Skills exist, candidate has 0 matches)
    // ========================================================================

    @Test
    @DisplayName("Zero matches when skills exist returns 0.0% and NONE label")
    void calculateMatch_zeroMatches_returnsZeroAndNoneLabel() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, javaSkill, true),
                new OpportunitySkill(testOpportunity, dockerSkill, false)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("python"), oppSkills);

        assertThat(res.matchPercentage()).isEqualTo(0.0);
        assertThat(res.matchLabel()).isEqualTo("NONE");
        assertThat(res.zeroSkillsSpecified()).isFalse();
        assertThat(res.missingRequiredSkills()).containsExactly("Java");
        assertThat(res.missingPreferredSkills()).containsExactly("Docker");
    }

    // ========================================================================
    // Alias Canonicalization & Normalization (PRD Section 4.2)
    // ========================================================================

    @Test
    @DisplayName("Alias canonicalization matches C++ and Cpp to cpp")
    void aliasCanonicalization_cpp() {
        assertThat(Skill.normalizeCanonicalName("C++")).isEqualTo("cpp");
        assertThat(Skill.normalizeCanonicalName("c++")).isEqualTo("cpp");
        assertThat(Skill.normalizeCanonicalName("Cpp")).isEqualTo("cpp");
        assertThat(Skill.normalizeCanonicalName("cpp")).isEqualTo("cpp");

        // Opportunity requires C++, candidate has Cpp
        Skill cppSkill = new Skill("C++", SkillCategory.LANGUAGE);
        List<OpportunitySkill> oppSkills = List.of(new OpportunitySkill(testOpportunity, cppSkill, true));

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("Cpp"), oppSkills);
        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchedRequiredSkills()).containsExactly("C++");
    }

    @Test
    @DisplayName("Alias canonicalization matches Node.js, NodeJS, and Node to nodejs")
    void aliasCanonicalization_nodejs() {
        assertThat(Skill.normalizeCanonicalName("Node.js")).isEqualTo("nodejs");
        assertThat(Skill.normalizeCanonicalName("NodeJS")).isEqualTo("nodejs");
        assertThat(Skill.normalizeCanonicalName("Node")).isEqualTo("nodejs");
        assertThat(Skill.normalizeCanonicalName("node")).isEqualTo("nodejs");

        Skill nodeSkill = new Skill("Node.js", SkillCategory.FRAMEWORK);
        List<OpportunitySkill> oppSkills = List.of(new OpportunitySkill(testOpportunity, nodeSkill, true));

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("Node"), oppSkills);
        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchedRequiredSkills()).containsExactly("Node.js");
    }

    @Test
    @DisplayName("Alias canonicalization matches React.js, ReactJS, and React to react")
    void aliasCanonicalization_react() {
        assertThat(Skill.normalizeCanonicalName("React.js")).isEqualTo("react");
        assertThat(Skill.normalizeCanonicalName("ReactJS")).isEqualTo("react");
        assertThat(Skill.normalizeCanonicalName("React")).isEqualTo("react");
        assertThat(Skill.normalizeCanonicalName("react")).isEqualTo("react");

        Skill reactSkill = new Skill("React", SkillCategory.FRAMEWORK);
        List<OpportunitySkill> oppSkills = List.of(new OpportunitySkill(testOpportunity, reactSkill, true));

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("React.js"), oppSkills);
        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchedRequiredSkills()).containsExactly("React");
    }

    @Test
    @DisplayName("Alias canonicalization matches .NET Core, ASP.NET, and .NET to dotnetcore")
    void aliasCanonicalization_dotnetcore() {
        assertThat(Skill.normalizeCanonicalName(".NET Core")).isEqualTo("dotnetcore");
        assertThat(Skill.normalizeCanonicalName("ASP.NET")).isEqualTo("dotnetcore");
        assertThat(Skill.normalizeCanonicalName(".NET")).isEqualTo("dotnetcore");
        assertThat(Skill.normalizeCanonicalName("dotnetcore")).isEqualTo("dotnetcore");
        assertThat(Skill.normalizeCanonicalName("net")).isEqualTo("dotnetcore");

        Skill dotnetSkill = new Skill(".NET Core", SkillCategory.FRAMEWORK);
        List<OpportunitySkill> oppSkills = List.of(new OpportunitySkill(testOpportunity, dotnetSkill, true));

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("ASP.NET"), oppSkills);
        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchedRequiredSkills()).containsExactly(".NET Core");
    }

    @Test
    @DisplayName("Alias canonicalization matches Spring Boot and SpringBoot to springboot")
    void aliasCanonicalization_springboot() {
        assertThat(Skill.normalizeCanonicalName("Spring Boot")).isEqualTo("springboot");
        assertThat(Skill.normalizeCanonicalName("SpringBoot")).isEqualTo("springboot");
        assertThat(Skill.normalizeCanonicalName("springboot")).isEqualTo("springboot");

        Skill sbSkill = new Skill("Spring Boot", SkillCategory.FRAMEWORK);
        List<OpportunitySkill> oppSkills = List.of(new OpportunitySkill(testOpportunity, sbSkill, true));

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("SpringBoot"), oppSkills);
        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchedRequiredSkills()).containsExactly("Spring Boot");
    }

    @Test
    @DisplayName("Alias canonicalization matches PostgreSQL and Postgres to postgresql")
    void aliasCanonicalization_postgresql() {
        assertThat(Skill.normalizeCanonicalName("PostgreSQL")).isEqualTo("postgresql");
        assertThat(Skill.normalizeCanonicalName("Postgres")).isEqualTo("postgresql");
        assertThat(Skill.normalizeCanonicalName("postgresql")).isEqualTo("postgresql");

        Skill pgSkill = new Skill("PostgreSQL", SkillCategory.DATABASE);
        List<OpportunitySkill> oppSkills = List.of(new OpportunitySkill(testOpportunity, pgSkill, true));

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("Postgres"), oppSkills);
        assertThat(res.matchPercentage()).isEqualTo(100.0);
        assertThat(res.matchedRequiredSkills()).containsExactly("PostgreSQL");
    }

    // ========================================================================
    // Collision Safety (C vs C++ vs C#)
    // ========================================================================

    @Test
    @DisplayName("Collision safety: C, C++, and C# canonicalize to distinct non-colliding names")
    void collisionSafety_c_cpp_csharp() {
        String canonicalC = Skill.normalizeCanonicalName("C");
        String canonicalCpp = Skill.normalizeCanonicalName("C++");
        String canonicalCSharp = Skill.normalizeCanonicalName("C#");

        assertThat(canonicalC).isEqualTo("c");
        assertThat(canonicalCpp).isEqualTo("cpp");
        assertThat(canonicalCSharp).isEqualTo("csharp");

        assertThat(canonicalC).isNotEqualTo(canonicalCpp);
        assertThat(canonicalC).isNotEqualTo(canonicalCSharp);
        assertThat(canonicalCpp).isNotEqualTo(canonicalCSharp);

        // Candidate with only 'C' does not match opportunity requiring C++ or C#
        Skill cPlusPlus = new Skill("C++", SkillCategory.LANGUAGE);
        Skill cSharp = new Skill("C#", SkillCategory.LANGUAGE);
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, cPlusPlus, true),
                new OpportunitySkill(testOpportunity, cSharp, true)
        );

        OpportunitySkillMatchResponse res = matchingService.calculateMatch(1L, Set.of("C"), oppSkills);
        assertThat(res.matchPercentage()).isEqualTo(0.0);
        assertThat(res.matchedRequiredSkills()).isEmpty();
        assertThat(res.missingRequiredSkills()).containsExactlyInAnyOrder("C++", "C#");
    }

    // ========================================================================
    // Performance Benchmark (PRD Section 8: <10ms target)
    // ========================================================================

    @Test
    @DisplayName("Deterministic in-memory match calculation executes well under 10ms")
    void calculateMatch_performanceBenchmark() {
        List<OpportunitySkill> oppSkills = List.of(
                new OpportunitySkill(testOpportunity, javaSkill, true),
                new OpportunitySkill(testOpportunity, springSkill, true),
                new OpportunitySkill(testOpportunity, dockerSkill, false),
                new OpportunitySkill(testOpportunity, k8sSkill, false)
        );
        Set<String> candidateSkills = Set.of("java", "docker");

        // Warm up JIT
        for (int i = 0; i < 5000; i++) {
            matchingService.calculateMatch(1L, candidateSkills, oppSkills);
        }

        long start = System.nanoTime();
        int iterations = 10000;
        for (int i = 0; i < iterations; i++) {
            matchingService.calculateMatch(1L, candidateSkills, oppSkills);
        }
        long durationNs = System.nanoTime() - start;
        double avgMs = (double) durationNs / (iterations * 1_000_000.0);

        assertThat(avgMs).isLessThan(10.0);
    }
}