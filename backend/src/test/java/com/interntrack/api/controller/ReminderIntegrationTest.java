package com.interntrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.Company;
import com.interntrack.api.entity.Opportunity;
import com.interntrack.api.entity.Reminder;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.ApplicationStatus;
import com.interntrack.api.entity.enums.LocationType;
import com.interntrack.api.entity.enums.ReminderType;
import com.interntrack.api.entity.enums.SourceProvider;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.CompanyRepository;
import com.interntrack.api.repository.OpportunityRepository;
import com.interntrack.api.repository.ReminderRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=REMINDER_INTEGRATION_TEST_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class ReminderIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Application applicationA;
    private Application applicationB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long now = System.currentTimeMillis();

        // Create Users
        userA = userRepository.save(new User("rem_a_" + now + "@test.com", "hashA", "Candidate A", UserRole.ROLE_USER));
        userB = userRepository.save(new User("rem_b_" + now + "@test.com", "hashB", "Candidate B", UserRole.ROLE_USER));

        AuthenticatedUser authA = new AuthenticatedUser(userA.getId(), userA.getEmail(), userA.getFullName(), userA.getRole(), true);
        AuthenticatedUser authB = new AuthenticatedUser(userB.getId(), userB.getEmail(), userB.getFullName(), userB.getRole(), true);

        tokenA = "Bearer " + jwtService.generateAccessToken(authA);
        tokenB = "Bearer " + jwtService.generateAccessToken(authB);

        // Create shared/sample companies and opportunities
        Company comp = companyRepository.save(new Company("Comp_" + now, "comp.com", "Tech"));
        Opportunity opp = opportunityRepository.save(new Opportunity(comp, "SWE Intern", LocationType.REMOTE, SourceProvider.SYSTEM));

        // Create Applications
        applicationA = applicationRepository.save(new Application(userA, opp, ApplicationStatus.APPLIED));
        applicationB = applicationRepository.save(new Application(userB, opp, ApplicationStatus.APPLIED));
    }

    @AfterEach
    void tearDown() {
        reminderRepository.deleteAll();
        applicationRepository.deleteAll();
        opportunityRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-end multi-tenant lifecycle and IDOR verification")
    void fullReminderLifecycle_andIdorProtection() throws Exception {
        // 1. User A creates reminder linked to applicationA
        String createReq = """
                {
                    "title": "Submit OA Part 1",
                    "dueAt": "2026-10-15T10:00:00Z",
                    "reminderType": "OA_DEADLINE",
                    "applicationId": %d
                }
                """.formatted(applicationA.getId());

        MvcResult createResult = mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print()).andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/reminders/")))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Submit OA Part 1")))
                .andExpect(jsonPath("$.data.reminderType", is("OA_DEADLINE")))
                .andExpect(jsonPath("$.data.applicationId", is(applicationA.getId().intValue())))
                .andExpect(jsonPath("$.data.completed", is(false)))
                .andReturn();

        Long reminderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        assertThat(reminderId).isNotNull();

        // 2. User A retrieves their reminder
        mockMvc.perform(get("/api/v1/reminders/" + reminderId)
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(reminderId.intValue())))
                .andExpect(jsonPath("$.data.title", is("Submit OA Part 1")));

        // 3. User A lists reminders -> has 1 item
        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 4. User B lists reminders -> receives empty list (multi-tenant isolation)
        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 5. Cross-tenant linkage: User B tries to create reminder linking User A's application -> 403 Forbidden
        String illegalLinkReq = """
                {
                    "title": "Sneaky Link",
                    "dueAt": "2026-10-15T10:00:00Z",
                    "reminderType": "CUSTOM",
                    "applicationId": %d
                }
                """.formatted(applicationA.getId());

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(illegalLinkReq))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        // 6. IDOR: User B attempts GET on User A's reminder -> 403 Forbidden
        mockMvc.perform(get("/api/v1/reminders/" + reminderId)
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        // 7. IDOR: User B attempts PUT on User A's reminder -> 403 Forbidden
        String updateReqB = """
                {
                    "title": "Hijack"
                }
                """;
        mockMvc.perform(put("/api/v1/reminders/" + reminderId)
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReqB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        // 8. IDOR: User B attempts PATCH complete on User A's reminder -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/reminders/" + reminderId + "/complete")
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        // 9. IDOR: User B attempts DELETE on User A's reminder -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/reminders/" + reminderId)
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        // 10. User A completes reminder -> 200 OK, completed=true
        mockMvc.perform(patch("/api/v1/reminders/" + reminderId + "/complete")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.completed", is(true)));

        // Verify in DB
        Reminder inDb = reminderRepository.findById(reminderId).orElseThrow();
        assertThat(inDb.getCompleted()).isTrue();

        // 11. User A updates reminder details
        String updateReqA = """
                {
                    "title": "Submit OA Part 1 - Completed",
                    "dueAt": "2026-10-15T12:00:00Z",
                    "completed": true,
                    "reminderType": "OA_DEADLINE"
                }
                """;
        mockMvc.perform(put("/api/v1/reminders/" + reminderId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReqA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Submit OA Part 1 - Completed")));

        // 12. User A deletes their reminder -> 204 No Content
        mockMvc.perform(delete("/api/v1/reminders/" + reminderId)
                        .header("Authorization", tokenA))
                .andExpect(status().isNoContent());

        // Verify deleted from DB
        assertThat(reminderRepository.findById(reminderId)).isEmpty();
    }

    @Test
    @DisplayName("dueBefore filter: verifies <= cutoff, > cutoff excluded, combinations, and multi-tenant isolation")
    void dueBeforeFilteringAndCombinations_multiTenantVerification() throws Exception {
        Instant cutoff = Instant.parse("2026-10-15T12:00:00Z");

        // User A reminders
        Reminder remBefore = new Reminder(userA, applicationA, "Before cutoff", cutoff.minusSeconds(3600), ReminderType.CUSTOM);
        remBefore.setCompleted(false);

        Reminder remExact = new Reminder(userA, applicationA, "Exact cutoff", cutoff, ReminderType.OA_DEADLINE);
        remExact.setCompleted(false);

        Reminder remAfter = new Reminder(userA, applicationA, "After cutoff", cutoff.plusSeconds(3600), ReminderType.CUSTOM);
        remAfter.setCompleted(false);

        Reminder remBeforeCompleted = new Reminder(userA, applicationA, "Before cutoff completed", cutoff.minusSeconds(1800), ReminderType.CUSTOM);
        remBeforeCompleted.setCompleted(true);

        Reminder remBeforeNoApp = new Reminder(userA, null, "Before cutoff no app", cutoff.minusSeconds(1800), ReminderType.CUSTOM);
        remBeforeNoApp.setCompleted(false);

        reminderRepository.saveAll(List.of(remBefore, remExact, remAfter, remBeforeCompleted, remBeforeNoApp));

        // User B reminder
        Reminder remUserB = new Reminder(userB, applicationB, "User B reminder", cutoff.minusSeconds(1800), ReminderType.CUSTOM);
        remUserB.setCompleted(false);
        reminderRepository.save(remUserB);

        // 1. Omitted dueBefore: returns all 5 of User A's reminders
        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(5)));

        // 2. dueBefore cutoff: returns <= cutoff (before, exact, before completed, before no app); excludes after
        mockMvc.perform(get("/api/v1/reminders?dueBefore=2026-10-15T12:00:00Z")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[*].title", hasItems("Before cutoff", "Exact cutoff", "Before cutoff completed", "Before cutoff no app")))
                .andExpect(jsonPath("$.data[*].title", not(hasItem("After cutoff"))))
                .andExpect(jsonPath("$.data[*].title", not(hasItem("User B reminder"))));

        // 3. dueBefore + completed=false: returns incomplete <= cutoff; excludes completed and after
        mockMvc.perform(get("/api/v1/reminders?dueBefore=2026-10-15T12:00:00Z&completed=false")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].title", hasItems("Before cutoff", "Exact cutoff", "Before cutoff no app")))
                .andExpect(jsonPath("$.data[*].title", not(hasItem("Before cutoff completed"))))
                .andExpect(jsonPath("$.data[*].title", not(hasItem("After cutoff"))));

        // 4. dueBefore + completed=false + applicationId: returns incomplete <= cutoff for applicationA only
        mockMvc.perform(get("/api/v1/reminders?dueBefore=2026-10-15T12:00:00Z&completed=false&applicationId=" + applicationA.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].title", hasItems("Before cutoff", "Exact cutoff")))
                .andExpect(jsonPath("$.data[*].title", not(hasItem("Before cutoff no app"))))
                .andExpect(jsonPath("$.data[*].title", not(hasItem("Before cutoff completed"))))
                .andExpect(jsonPath("$.data[*].title", not(hasItem("After cutoff"))));

        // 5. Multi-tenant isolation: User B querying dueBefore only sees User B's reminder
        mockMvc.perform(get("/api/v1/reminders?dueBefore=2026-10-15T12:00:00Z&completed=false")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("User B reminder")));
    }
}