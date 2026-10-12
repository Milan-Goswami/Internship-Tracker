package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.application.CreateApplicationRequest;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.CompanyRepository;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.UserRepository;
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
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "security.jwt.secret=APPLICATION_CONCURRENCY_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class ApplicationConcurrencyIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String validToken;
    private User testUser;
    private Company testCompany;
    private Opportunity testOpportunity;

    private static final String TEST_USER_EMAIL = "app_concurrency@example.com";
    private static final String TEST_COMPANY_CANONICAL = "concurrentappcompany";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanDatabase();

        testUser = new User(TEST_USER_EMAIL, "pwd123456", "Concurrency Candidate", UserRole.ROLE_USER);
        testUser = userRepository.save(testUser);

        testCompany = new Company("Concurrent App Company", "https://example.com", "Tech");
        testCompany = companyRepository.save(testCompany);

        testOpportunity = new Opportunity(testCompany, "Concurrency SWE Intern", LocationType.REMOTE, SourceProvider.USER_MANUAL);
        testOpportunity.setCreatedByUser(testUser);
        testOpportunity = opportunityRepository.save(testOpportunity);

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
            List<Application> apps = applicationRepository.findByUserId(u.getId());
            applicationRepository.deleteAll(apps);
            applicationRepository.flush();

            opportunityRepository.findByCreatedByUserId(u.getId()).forEach(opp -> {
                opportunityRepository.delete(opp);
            });
            opportunityRepository.flush();

            userRepository.delete(u);
            userRepository.flush();
        });

        companyRepository.findByCanonicalName(TEST_COMPANY_CANONICAL).ifPresent(c -> {
            companyRepository.delete(c);
            companyRepository.flush();
        });
    }

    @Test
    @DisplayName("Two concurrent identical application creations for same (userId, oppId): exactly 1 succeeds (201), 1 gets 409 (not 500), 1 row in DB")
    void concurrentApplicationCreation_exactlyOneSucceeds_losingGets409_noDuplicates() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        CreateApplicationRequest request = new CreateApplicationRequest(
                testOpportunity.getId(),
                ApplicationStatus.SAVED,
                null,
                null,
                "First apply notes",
                4
        );
        String payload = objectMapper.writeValueAsString(request);

        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        List<String> responseBodies = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/applications")
                                    .header("Authorization", validToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                            .andReturn();

                    statusCodes.add(result.getResponse().getStatus());
                    responseBodies.add(result.getResponse().getContentAsString());
                } catch (Exception e) {
                    statusCodes.add(500);
                    responseBodies.add(e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(statusCodes).hasSize(2);

        long count201 = statusCodes.stream().filter(s -> s == 201).count();
        long count409 = statusCodes.stream().filter(s -> s == 409).count();
        long count500 = statusCodes.stream().filter(s -> s == 500).count();

        assertThat(count201).as("Exactly one request must succeed with 201 Created").isEqualTo(1);
        assertThat(count409).as("Losing concurrent request must receive 409 Conflict").isEqualTo(1);
        assertThat(count500).as("Zero internal server errors (500) permitted").isEqualTo(0);

        List<Application> dbRecords = applicationRepository.findByUserId(testUser.getId());
        assertThat(dbRecords).as("Database must contain exactly one application for candidate opportunity").hasSize(1);
        assertThat(dbRecords.get(0).getOpportunity().getId()).isEqualTo(testOpportunity.getId());
    }
}