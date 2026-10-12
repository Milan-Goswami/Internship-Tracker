package com.interntrack.api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_SPA_ROUTING_KEY_EXACTLY_64_BYTES_LONG_0123456789ABCDEF0123456789ABCDEF"
})
class SpaRoutingIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Static index.html is permitted without auth and contains InternTrack SPA title")
    void indexHtml_servesContent() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("InternTrack")));
    }

    @Test
    @DisplayName("Client-side route /dashboard is permitted without auth and falls back to index.html")
    void clientRoute_dashboard_servesIndexHtml() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("InternTrack")));
    }

    @Test
    @DisplayName("Client-side route /applications is permitted without auth and falls back to index.html")
    void clientRoute_applications_servesIndexHtml() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("InternTrack")));
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/applications requires JWT and returns 401 Unauthorized")
    void apiRoute_requiresAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Public API endpoint /api/v1/health is permitted without auth")
    void publicApi_health_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
