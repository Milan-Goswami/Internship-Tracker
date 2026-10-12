package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.company.CreateCompanyRequest;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.CompanyRepository;
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
        "security.jwt.secret=COMPANY_CONCURRENCY_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class CompanyConcurrencyIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CompanyRepository companyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String validToken;
    private final String targetCanonical = "concurrentalphacorp";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        AuthenticatedUser samplePrincipal = new AuthenticatedUser(101L, "concurrency@example.com", "Concurrent User", UserRole.ROLE_USER, true);
        validToken = jwtService.generateAccessToken(samplePrincipal);

        // Ensure clean state before running
        companyRepository.findByCanonicalName(targetCanonical).ifPresent(companyRepository::delete);
    }

    @AfterEach
    void tearDown() {
        // Clean up persisted test entity to keep interntrack_db at 0 rows
        companyRepository.findByCanonicalName(targetCanonical).ifPresent(companyRepository::delete);
    }

    @Test
    @DisplayName("Two concurrent identical company creation requests: exactly 1 succeeds (201), 1 fails with 409 (not 500), exactly 1 row in database")
    void concurrentCompanyCreation_exactlyOneSucceeds_losingGets409_noDuplicates() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Concurrent Alpha, Corp!",
                "concurrentalpha.com",
                "Technology",
                "https://logo.example.com/logo.png",
                "Austin, TX"
        );
        String payload = objectMapper.writeValueAsString(request);

        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        List<String> responseBodies = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/companies")
                                    .header("Authorization", "Bearer " + validToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                            .andReturn();
                    statusCodes.add(result.getResponse().getStatus());
                    responseBodies.add(result.getResponse().getContentAsString());
                } catch (Exception e) {
                    statusCodes.add(500);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(statusCodes).hasSize(2);

        // 1. Neither response is HTTP 500
        assertThat(statusCodes).doesNotContain(500);

        // 2. Exactly one request succeeds with HTTP 201 Created
        long count201 = statusCodes.stream().filter(s -> s == 201).count();
        assertThat(count201).isEqualTo(1);

        // 3. The losing request receives HTTP 409 Conflict
        long count409 = statusCodes.stream().filter(s -> s == 409).count();
        assertThat(count409).isEqualTo(1);

        // 4. Exactly one company is persisted in MySQL
        Optional<Company> persisted = companyRepository.findByCanonicalName(targetCanonical);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getName()).isEqualTo("Concurrent Alpha, Corp!");

        // 5. No duplicate rows exist in database
        List<Company> duplicates = companyRepository.findAll().stream()
                .filter(c -> targetCanonical.equals(c.getCanonicalName()))
                .toList();
        assertThat(duplicates).hasSize(1);
    }
}