package com.interntrack.api.controller;

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
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.security.AuthenticatedUser;
import com.interntrack.api.security.JwtService;
import com.interntrack.api.service.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_REMINDER_CONTROLLER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class ReminderControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ReminderService reminderService;

    private MockMvc mockMvc;
    private String userToken;
    private final Long USER_ID = 100L;

    private Reminder sampleReminder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        AuthenticatedUser authUser = new AuthenticatedUser(USER_ID, "candidate@example.com", "Candidate One", UserRole.ROLE_USER, true);
        userToken = "Bearer " + jwtService.generateAccessToken(authUser);

        User user = new User("candidate@example.com", "hash", "Candidate One", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", USER_ID);

        Company company = new Company("Google", "google.com", "Tech");
        Opportunity opp = new Opportunity(company, "SWE Intern", LocationType.REMOTE, SourceProvider.SYSTEM);
        Application app = new Application(user, opp, ApplicationStatus.APPLIED);
        ReflectionTestUtils.setField(app, "id", 10L);

        sampleReminder = new Reminder(
                user,
                app,
                "Submit OA Part 1",
                Instant.now().plusSeconds(86400),
                ReminderType.OA_DEADLINE
        );
        ReflectionTestUtils.setField(sampleReminder, "id", 1L);
    }

    @Test
    @DisplayName("GET /api/v1/reminders returns 401 when unauthenticated")
    void listReminders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reminders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/reminders returns 200 with user reminder list")
    void listReminders_returns200() throws Exception {
        when(reminderService.findByUserIdFiltered(eq(USER_ID), isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleReminder));

        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Submit OA Part 1"))
                .andExpect(jsonPath("$.data[0].reminderType").value("OA_DEADLINE"))
                .andExpect(jsonPath("$.data[0].completed").value(false));

        verify(reminderService).findByUserIdFiltered(USER_ID, null, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/reminders with completed filter returns 200")
    void listReminders_withFilter_returns200() throws Exception {
        when(reminderService.findByUserIdFiltered(eq(USER_ID), eq(false), isNull(), isNull()))
                .thenReturn(List.of(sampleReminder));

        mockMvc.perform(get("/api/v1/reminders?completed=false")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(reminderService).findByUserIdFiltered(USER_ID, false, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/reminders with dueBefore filter returns 200")
    void listReminders_withDueBefore_returns200() throws Exception {
        Instant cutoff = Instant.parse("2026-10-15T12:00:00Z");
        when(reminderService.findByUserIdFiltered(eq(USER_ID), isNull(), isNull(), eq(cutoff)))
                .thenReturn(List.of(sampleReminder));

        mockMvc.perform(get("/api/v1/reminders?dueBefore=2026-10-15T12:00:00Z")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Submit OA Part 1"));

        verify(reminderService).findByUserIdFiltered(USER_ID, null, null, cutoff);
    }

    @Test
    @DisplayName("GET /api/v1/reminders with dueBefore and completed filters returns 200")
    void listReminders_withDueBeforeAndCompleted_returns200() throws Exception {
        Instant cutoff = Instant.parse("2026-10-15T12:00:00Z");
        when(reminderService.findByUserIdFiltered(eq(USER_ID), eq(false), isNull(), eq(cutoff)))
                .thenReturn(List.of(sampleReminder));

        mockMvc.perform(get("/api/v1/reminders?dueBefore=2026-10-15T12:00:00Z&completed=false")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(reminderService).findByUserIdFiltered(USER_ID, false, null, cutoff);
    }

    @Test
    @DisplayName("GET /api/v1/reminders with dueBefore, completed, and applicationId returns 200")
    void listReminders_withDueBeforeCompletedAndApplicationId_returns200() throws Exception {
        Instant cutoff = Instant.parse("2026-10-15T12:00:00Z");
        when(reminderService.findByUserIdFiltered(eq(USER_ID), eq(false), eq(10L), eq(cutoff)))
                .thenReturn(List.of(sampleReminder));

        mockMvc.perform(get("/api/v1/reminders?dueBefore=2026-10-15T12:00:00Z&completed=false&applicationId=10")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(reminderService).findByUserIdFiltered(USER_ID, false, 10L, cutoff);
    }

    @Test
    @DisplayName("POST /api/v1/reminders returns 201 with Location header on valid payload")
    void createReminder_returns201() throws Exception {
        String req = """
                {
                    "title": "Prepare for Technical Round",
                    "dueAt": "2026-10-15T10:00:00Z",
                    "reminderType": "INTERVIEW_PREP"
                }
                """;

        when(reminderService.createReminder(eq(USER_ID), isNull(), eq("Prepare for Technical Round"), any(Instant.class), eq(ReminderType.INTERVIEW_PREP)))
                .thenReturn(sampleReminder);

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reminders/" + sampleReminder.getId()))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Submit OA Part 1"));
    }

    @Test
    @DisplayName("POST /api/v1/reminders returns 400 when title is blank")
    void createReminder_blankTitle_returns400() throws Exception {
        String req = """
                {
                    "title": "   ",
                    "dueAt": "2026-10-15T10:00:00Z",
                    "reminderType": "CUSTOM"
                }
                """;

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(reminderService);
    }

    @Test
    @DisplayName("POST /api/v1/reminders returns 400 when dueAt is missing")
    void createReminder_missingDueAt_returns400() throws Exception {
        String req = """
                {
                    "title": "Follow up email",
                    "reminderType": "FOLLOW_UP"
                }
                """;

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(reminderService);
    }

    @Test
    @DisplayName("POST /api/v1/reminders returns 403 when linking to another user's application")
    void createReminder_otherUserApplication_returns403() throws Exception {
        String req = """
                {
                    "title": "Follow up",
                    "dueAt": "2026-10-15T10:00:00Z",
                    "reminderType": "FOLLOW_UP",
                    "applicationId": 99
                }
                """;

        when(reminderService.createReminder(eq(USER_ID), eq(99L), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Access denied: Application belongs to another user"));

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/reminders/{id} returns 200 for owned reminder")
    void getReminderById_returns200() throws Exception {
        when(reminderService.findAccessibleById(1L, USER_ID)).thenReturn(sampleReminder);

        mockMvc.perform(get("/api/v1/reminders/1")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Submit OA Part 1"));
    }

    @Test
    @DisplayName("GET /api/v1/reminders/{id} returns 404 when reminder does not exist")
    void getReminderById_notFound_returns404() throws Exception {
        when(reminderService.findAccessibleById(99L, USER_ID))
                .thenThrow(new ResourceNotFoundException("Reminder not found with id: 99"));

        mockMvc.perform(get("/api/v1/reminders/99")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/reminders/{id} returns 403 when reminder belongs to another user")
    void getReminderById_otherUser_returns403() throws Exception {
        when(reminderService.findAccessibleById(50L, USER_ID))
                .thenThrow(new AccessDeniedException("Access denied: You do not own this reminder"));

        mockMvc.perform(get("/api/v1/reminders/50")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/reminders/{id} returns 200 on successful update")
    void updateReminder_returns200() throws Exception {
        String req = """
                {
                    "title": "Updated OA Reminder",
                    "dueAt": "2026-10-16T12:00:00Z",
                    "completed": true,
                    "reminderType": "OA_DEADLINE"
                }
                """;

        when(reminderService.updateReminder(eq(1L), eq(USER_ID), isNull(), eq("Updated OA Reminder"), any(Instant.class), eq(true), eq(ReminderType.OA_DEADLINE)))
                .thenReturn(sampleReminder);

        mockMvc.perform(put("/api/v1/reminders/1")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Submit OA Part 1"));
    }

    @Test
    @DisplayName("PUT /api/v1/reminders/{id} returns 403 when updating another user's reminder")
    void updateReminder_otherUser_returns403() throws Exception {
        String req = """
                {
                    "title": "Hijack title"
                }
                """;

        when(reminderService.updateReminder(eq(99L), eq(USER_ID), any(), any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Access denied: You do not own this reminder"));

        mockMvc.perform(put("/api/v1/reminders/99")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/reminders/{id}/complete marks completed and returns 200")
    void completeReminder_returns200() throws Exception {
        sampleReminder.setCompleted(true);
        when(reminderService.markCompleted(1L, USER_ID)).thenReturn(sampleReminder);

        mockMvc.perform(patch("/api/v1/reminders/1/complete")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/reminders/{id}/complete returns 403 when not owner")
    void completeReminder_otherUser_returns403() throws Exception {
        when(reminderService.markCompleted(55L, USER_ID))
                .thenThrow(new AccessDeniedException("Access denied: You do not own this reminder"));

        mockMvc.perform(patch("/api/v1/reminders/55/complete")
                        .header("Authorization", userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/v1/reminders/{id} returns 204 for owned reminder")
    void deleteReminder_returns204() throws Exception {
        doNothing().when(reminderService).deleteReminder(1L, USER_ID);

        mockMvc.perform(delete("/api/v1/reminders/1")
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        verify(reminderService).deleteReminder(1L, USER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/reminders/{id} returns 403 when deleting another user's reminder")
    void deleteReminder_otherUser_returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied: You do not own this reminder"))
                .when(reminderService).deleteReminder(99L, USER_ID);

        mockMvc.perform(delete("/api/v1/reminders/99")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}