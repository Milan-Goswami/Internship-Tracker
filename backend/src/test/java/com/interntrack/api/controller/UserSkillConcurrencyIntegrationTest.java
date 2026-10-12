package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.skill.AddUserSkillRequest;
import com.interntrack.api.entity.Skill;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.UserSkill;
import com.interntrack.api.entity.enums.ProficiencyLevel;
import com.interntrack.api.entity.enums.SkillCategory;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.SkillRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.repository.UserSkillRepository;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "security.jwt.secret=USERSKILL_CONCURRENCY_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class UserSkillConcurrencyIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String validToken;
    private User testUser;
    private Skill testSkill;

    private static final String TEST_USER_EMAIL = "userskill_concurrency@example.com";
    private static final String TEST_SKILL_CANONICAL = "concurrentskill";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanDatabase();

        testUser = new User(TEST_USER_EMAIL, "pwd123456", "Concurrency Candidate", UserRole.ROLE_USER);
        testUser = userRepository.save(testUser);

        testSkill = new Skill("Concurrent Skill", SkillCategory.LANGUAGE);
        testSkill = skillRepository.save(testSkill);

        AuthenticatedUser principal = new AuthenticatedUser(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getFullName(),
                testUser.getRole(),
                true
        );
        validToken = "Bearer " + jwtService.generateAccessToken(principal);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        userRepository.findByEmail(TEST_USER_EMAIL).ifPresent(u -> {
            List<UserSkill> userSkills = userSkillRepository.findByUserIdFetchSkill(u.getId());
            userSkillRepository.deleteAll(userSkills);
            userSkillRepository.flush();

            userRepository.delete(u);
            userRepository.flush();
        });

        skillRepository.findByCanonicalName(TEST_SKILL_CANONICAL).ifPresent(s -> {
            skillRepository.delete(s);
            skillRepository.flush();
        });
    }

    @Test
    @DisplayName("Concurrent duplicate user skill additions: exactly one 201 Created, one 409 Conflict, zero 500s, and exactly 1 DB row")
    void concurrentAddUserSkill_exactlyOneSucceeds() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AddUserSkillRequest request = new AddUserSkillRequest(testSkill.getId(), ProficiencyLevel.EXPERT, 3);
        String requestJson = objectMapper.writeValueAsString(request);

        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                MvcResult result = mockMvc.perform(post("/api/v1/skills/user")
                                .header("Authorization", validToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andReturn();

                return result.getResponse().getStatus();
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statusCodes.add(future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(statusCodes).containsExactlyInAnyOrder(201, 409);

        List<UserSkill> persisted = userSkillRepository.findByUserIdFetchSkill(testUser.getId());
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getSkill().getId()).isEqualTo(testSkill.getId());
    }
}