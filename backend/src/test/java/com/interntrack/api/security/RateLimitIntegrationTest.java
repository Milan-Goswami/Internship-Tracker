package com.interntrack.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interntrack.api.dto.auth.LoginRequest;
import com.interntrack.api.dto.auth.RefreshTokenRequest;
import com.interntrack.api.dto.auth.RegisterRequest;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.repository.RefreshTokenRepository;
import com.interntrack.api.repository.UserRepository;
import com.interntrack.api.security.PasswordService;
import com.interntrack.api.service.RateLimitService;
import com.interntrack.api.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_SECURITY_INTEGRATION_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF",
        "ratelimit.login.ip.capacity=5",
        "ratelimit.login.account.capacity=3",
        "ratelimit.register.ip.capacity=3",
        "ratelimit.refresh.ip.capacity=4"
})
@ActiveProfiles("test")
public class RateLimitIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordService passwordService;

    private User primaryUser;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        rateLimitService.reset();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        primaryUser = new User();
        primaryUser.setEmail("victim@example.com");
        primaryUser.setPasswordHash(passwordService.hash("CorrectPassword123!"));
        primaryUser.setFullName("Victim Account");
        primaryUser.setRole(UserRole.ROLE_USER);
        primaryUser.setIsActive(true);
        primaryUser = userRepository.save(primaryUser);
    }

    @AfterEach
    public void teardown() {
        rateLimitService.reset();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==========================================
    // 1-10: LOGIN RATE LIMITING TESTS
    // ==========================================

    @Test
    @DisplayName("1. Requests below per-account threshold reach authentication processing")
    void testLoginBelowAccountThreshold_reachesAuth() throws Exception {
        LoginRequest req = new LoginRequest("victim@example.com", "WrongPassword1!");

        // 1st and 2nd attempt with bad credentials should reach auth and return 401 Unauthorized (capacity = 3)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(request -> { request.setRemoteAddr("192.168.1.1"); return request; }))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(request -> { request.setRemoteAddr("192.168.1.1"); return request; }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Exceeding per-account threshold returns 429")
    void testLoginExceedingAccountThreshold_returns429() throws Exception {
        LoginRequest req = new LoginRequest("victim@example.com", "WrongPassword1!");

        // Use distinct IPs so IP limiter (capacity=5) is not triggered
        for (int i = 1; i <= 3; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr("192.168.10." + ipSuffix); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // 4th attempt exceeds account capacity (3) -> 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(request -> { request.setRemoteAddr("192.168.10.99"); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    @DisplayName("3. Changing IP does NOT bypass per-account limiter")
    void testLoginChangingIp_doesNotBypassAccountLimiter() throws Exception {
        LoginRequest req = new LoginRequest("victim@example.com", "WrongPassword1!");

        for (int i = 1; i <= 3; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr("10.1.1." + ipSuffix); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // Even with completely new IP 10.2.2.2, account limit blocks it
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(request -> { request.setRemoteAddr("10.2.2.2"); return request; }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("4. One throttled account does not throttle another account unnecessarily")
    void testLoginThrottledAccount_doesNotThrottleOtherAccount() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("innocent@example.com");
        otherUser.setPasswordHash(passwordService.hash("CorrectPassword123!"));
        otherUser.setFullName("Innocent User");
        otherUser.setRole(UserRole.ROLE_USER);
        otherUser.setIsActive(true);
        userRepository.save(otherUser);

        LoginRequest victimReq = new LoginRequest("victim@example.com", "WrongPassword1!");
        // Exhaust victim account limit across different IPs
        for (int i = 1; i <= 3; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(victimReq))
                            .with(request -> { request.setRemoteAddr("10.3.1." + ipSuffix); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // Victim is throttled
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(victimReq))
                        .with(request -> { request.setRemoteAddr("10.3.1.50"); return request; }))
                .andExpect(status().isTooManyRequests());

        // Innocent user can still login successfully from independent IP
        LoginRequest innocentReq = new LoginRequest("innocent@example.com", "CorrectPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(innocentReq))
                        .with(request -> { request.setRemoteAddr("10.3.1.51"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("5. Exceeding per-IP threshold returns 429")
    void testLoginExceedingIpThreshold_returns429() throws Exception {
        String testIp = "172.16.0.5";

        // IP capacity is 5. We test with 5 distinct non-existent accounts so account limit (3) isn't hit
        for (int i = 1; i <= 5; i++) {
            LoginRequest req = new LoginRequest("unique_user_" + i + "@example.com", "WrongPass123!");
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr(testIp); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt from the same IP -> 429
        LoginRequest req6 = new LoginRequest("unique_user_6@example.com", "WrongPass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req6))
                        .with(request -> { request.setRemoteAddr(testIp); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    @DisplayName("6. Changing account/email does NOT bypass per-IP limiter")
    void testLoginChangingEmail_doesNotBypassIpLimiter() throws Exception {
        String testIp = "172.16.0.10";

        for (int i = 1; i <= 5; i++) {
            LoginRequest req = new LoginRequest("stuffing_" + i + "@example.com", "WrongPass123!");
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr(testIp); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // Even with brand new victim@example.com, IP limiter blocks it
        LoginRequest victimReq = new LoginRequest("victim@example.com", "CorrectPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(victimReq))
                        .with(request -> { request.setRemoteAddr(testIp); return request; }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("7. The two limiters operate independently")
    void testLoginLimitersOperateIndependently() throws Exception {
        // Account limiter triggered from multiple IPs
        for (int i = 1; i <= 3; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("victim@example.com", "BadPass")))
                            .with(request -> { request.setRemoteAddr("10.5.0." + ipSuffix); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // Account is now throttled on new IP for wrong passwords
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("victim@example.com", "WrongPassword99!")))
                        .with(request -> { request.setRemoteAddr("10.5.0.99"); return request; }))
                .andExpect(status().isTooManyRequests());

        // But 10.5.0.1 has only used 1 of its 5 IP tokens, so another account from 10.5.0.1 still works
        User anotherUser = new User("other@example.com", passwordService.hash("Pass123!"), "Other", UserRole.ROLE_USER);
        userRepository.save(anotherUser);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("other@example.com", "Pass123!")))
                        .with(request -> { request.setRemoteAddr("10.5.0.1"); return request; }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("8. Successful authentication behaves correctly with respect to limiter state")
    void testLoginSuccess_worksProperlyUnderLimits() throws Exception {
        LoginRequest validReq = new LoginRequest("victim@example.com", "CorrectPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq))
                        .with(request -> { request.setRemoteAddr("192.168.20.1"); return request; }))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("9. 429 response uses expected API error envelope and Retry-After header")
    void testLogin429Response_envelopeAndRetryAfterHeader() throws Exception {
        LoginRequest req = new LoginRequest("victim@example.com", "WrongPassword1!");

        for (int i = 1; i <= 3; i++) {
            final int idx = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr("10.6.0." + idx); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(request -> { request.setRemoteAddr("10.6.0.100"); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many login attempts. Please try again later."));
    }

    @Test
    @DisplayName("10. No sensitive limiter state or bucket info is leaked in 429 response")
    void testLogin429Response_doesNotLeakInternalLimiterState() throws Exception {
        LoginRequest req = new LoginRequest("victim@example.com", "WrongPassword1!");

        for (int i = 1; i <= 3; i++) {
            final int idx = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr("10.7.0." + idx); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(request -> { request.setRemoteAddr("10.7.0.100"); return request; }))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Ensure no internal bucket names, remaining token counts, or account details leaked
        assertThat(body).doesNotContain("bucket");
        assertThat(body).doesNotContain("capacity");
        assertThat(body).doesNotContain("tokensRemaining");
        assertThat(body).doesNotContain("victim@example.com");
    }

    // ==========================================
    // 11-14: REGISTRATION RATE LIMITING TESTS
    // ==========================================

    @Test
    @DisplayName("11. Requests below registration IP threshold behave normally")
    void testRegisterBelowIpThreshold_succeeds() throws Exception {
        String regIp = "172.20.0.1";
        RegisterRequest req1 = new RegisterRequest("newbie1@example.com", "ValidPass123!", "Newbie 1");
        RegisterRequest req2 = new RegisterRequest("newbie2@example.com", "ValidPass123!", "Newbie 2");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1))
                        .with(request -> { request.setRemoteAddr(regIp); return request; }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2))
                        .with(request -> { request.setRemoteAddr(regIp); return request; }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("12. Exceeding registration IP threshold returns 429")
    void testRegisterExceedingIpThreshold_returns429() throws Exception {
        String regIp = "172.20.0.2";
        // Capacity is 3
        for (int i = 1; i <= 3; i++) {
            RegisterRequest req = new RegisterRequest("reg" + i + "@example.com", "ValidPass123!", "User " + i);
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr(regIp); return request; }))
                    .andExpect(status().isCreated());
        }

        // 4th registration from same IP returns 429
        RegisterRequest req4 = new RegisterRequest("reg4@example.com", "ValidPass123!", "User 4");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req4))
                        .with(request -> { request.setRemoteAddr(regIp); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many registration attempts. Please try again later."));
    }

    @Test
    @DisplayName("13. Changing email does NOT bypass registration IP limiter")
    void testRegisterChangingEmail_doesNotBypassIpLimiter() throws Exception {
        String regIp = "172.20.0.3";
        for (int i = 1; i <= 3; i++) {
            RegisterRequest req = new RegisterRequest("mass" + i + "@example.com", "ValidPass123!", "Mass " + i);
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr(regIp); return request; }))
                    .andExpect(status().isCreated());
        }

        RegisterRequest freshReq = new RegisterRequest("completely_different@example.com", "ValidPass123!", "Diff");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(freshReq))
                        .with(request -> { request.setRemoteAddr(regIp); return request; }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("14. Another IP is treated independently for registration")
    void testRegisterAnotherIp_treatedIndependently() throws Exception {
        String blockedIp = "172.20.0.4";
        for (int i = 1; i <= 3; i++) {
            RegisterRequest req = new RegisterRequest("blocked" + i + "@example.com", "ValidPass123!", "Blocked " + i);
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr(blockedIp); return request; }))
                    .andExpect(status().isCreated());
        }

        // Blocked on 172.20.0.4
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("blocked4@example.com", "ValidPass123!", "Blocked 4")))
                        .with(request -> { request.setRemoteAddr(blockedIp); return request; }))
                .andExpect(status().isTooManyRequests());

        // Independent IP 172.20.0.5 succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("independent@example.com", "ValidPass123!", "Independent")))
                        .with(request -> { request.setRemoteAddr("172.20.0.5"); return request; }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==========================================
    // 15-18: REFRESH RATE LIMITING & SECURITY TESTS
    // ==========================================

    @Test
    @DisplayName("15. Repeated refresh abuse eventually returns 429")
    void testRefreshAbuse_returns429() throws Exception {
        String refreshIp = "192.168.30.1";
        // Refresh capacity is 4. Make 4 attempts (which fail with 401 due to invalid token or 403 due to header, but consume tokens)
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .header("X-InternTrack-Client", "web")
                            .cookie(new Cookie("refreshToken", "dummy_token_" + i))
                            .with(request -> { request.setRemoteAddr(refreshIp); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // 5th attempt returns 429
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", "dummy_token_5"))
                        .with(request -> { request.setRemoteAddr(refreshIp); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many refresh attempts. Please try again later."));
    }

    @Test
    @DisplayName("16. Normal refresh token rotation remains functional under rate limiting")
    void testRefreshNormalRotation_remainsFunctional() throws Exception {
        String rawToken = refreshTokenService.createRefreshToken(primaryUser);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", rawToken))
                        .with(request -> { request.setRemoteAddr("192.168.30.2"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("17. Existing refresh token reuse detection remains functional")
    void testRefreshReuseDetection_remainsFunctional() throws Exception {
        String rawToken = refreshTokenService.createRefreshToken(primaryUser);

        // 1st valid use -> rotates token
        MvcResult firstUseResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", rawToken))
                        .with(request -> { request.setRemoteAddr("192.168.30.3"); return request; }))
                .andExpect(status().isOk())
                .andReturn();

        // 2nd reuse of old rawToken -> triggers reuse detection -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", rawToken))
                        .with(request -> { request.setRemoteAddr("192.168.30.3"); return request; }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("18. Existing logout/revocation behavior remains functional")
    void testLogoutRevocation_remainsFunctional() throws Exception {
        String rawToken = refreshTokenService.createRefreshToken(primaryUser);

        // Logout revokes token
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", rawToken))
                        .with(request -> { request.setRemoteAddr("192.168.30.4"); return request; }))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refreshToken", 0));

        // Attempt to use revoked token returns 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", rawToken))
                        .with(request -> { request.setRemoteAddr("192.168.30.4"); return request; }))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 19: SPOOFED FORWARDING HEADERS TEST
    // ==========================================

    @Test
    @DisplayName("19. Spoofed X-Forwarded-For cannot bypass IP throttling")
    void testSpoofedXForwardedFor_cannotBypassIpLimiting() throws Exception {
        String socketIp = "192.168.40.10";

        // IP capacity is 5. Attacker tries to spoof X-Forwarded-For with a unique fake IP on each request
        for (int i = 1; i <= 5; i++) {
            LoginRequest req = new LoginRequest("user" + i + "@example.com", "BadPass");
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "203.0.113." + i)
                            .header("X-Real-IP", "203.0.113." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr(socketIp); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt with yet another spoofed IP fails with 429 because actual remoteAddr is tracked
        LoginRequest req6 = new LoginRequest("user6@example.com", "BadPass");
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.99")
                        .header("X-Real-IP", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req6))
                        .with(request -> { request.setRemoteAddr(socketIp); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    // ==========================================
    // 20: CONCURRENCY / THREAD SAFETY TEST
    // ==========================================

    @Test
    @DisplayName("20. Concurrent requests are safely throttled without race conditions")
    void testConcurrency_threadSafeLimiting() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger tooManyRequestsCount = new AtomicInteger(0);
        AtomicInteger unauthorizedCount = new AtomicInteger(0);

        String testIp = "192.168.50.1"; // Capacity is 5
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int id = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    LoginRequest req = new LoginRequest("concurrent" + id + "@example.com", "BadPass");
                    MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req))
                                    .with(request -> { request.setRemoteAddr(testIp); return request; }))
                            .andReturn();

                    int status = res.getResponse().getStatus();
                    if (status == 429) {
                        tooManyRequestsCount.incrementAndGet();
                    } else if (status == 401) {
                        unauthorizedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // fire all requests simultaneously

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Total requests = 10, IP capacity = 5
        // Exactly 5 requests should get 401 and exactly 5 requests should get 429
        assertThat(unauthorizedCount.get()).isEqualTo(5);
        assertThat(tooManyRequestsCount.get()).isEqualTo(5);
    }

    // ==========================================
    // 21-24: ADVERSARIAL EDGE CASE REVIEWS
    // ==========================================

    @Test
    @DisplayName("21. Account DoS Protection: Legitimate user can still log in and reset counter even after attacker exhausts account bucket")
    void testAccountDosPrevention_legitimateUserCanLoginEvenIfAttackerExhaustsAccountBucket() throws Exception {
        LoginRequest badPassReq = new LoginRequest("victim@example.com", "WrongPassword1!");

        // Attacker exhausts victim account limit (capacity = 3) from 3 different IPs
        for (int i = 1; i <= 3; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badPassReq))
                            .with(request -> { request.setRemoteAddr("10.8.0." + ipSuffix); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // Attacker's 4th attempt with wrong password gets throttled with 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPassReq))
                        .with(request -> { request.setRemoteAddr("10.8.0.50"); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"));

        // Legitimate user arrives with CORRECT password from their own IP -> SUCCESS (200 OK)
        LoginRequest validReq = new LoginRequest("victim@example.com", "CorrectPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq))
                        .with(request -> { request.setRemoteAddr("10.8.0.100"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));

        // The successful login reset the account limiter, so subsequent mistype starts fresh
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPassReq))
                        .with(request -> { request.setRemoteAddr("10.8.0.101"); return request; }))
                .andExpect(status().isUnauthorized()); // 401, not 429!
    }

    @Test
    @DisplayName("22. Dual-Bucket Fairness: An IP throttled by IP limiter does NOT consume account tokens")
    void testDualBucketFairness_throttledIpDoesNotConsumeAccountTokens() throws Exception {
        String attackerIp = "172.16.50.1"; // IP capacity = 5

        // Exhaust attacker's IP bucket using different random accounts
        for (int i = 1; i <= 5; i++) {
            LoginRequest req = new LoginRequest("random" + i + "@example.com", "BadPass");
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(request -> { request.setRemoteAddr(attackerIp); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // Attacker's IP is now completely throttled.
        // Attacker tries to target victim@example.com from the throttled IP
        LoginRequest victimReq = new LoginRequest("victim@example.com", "BadPass");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(victimReq))
                        .with(request -> { request.setRemoteAddr(attackerIp); return request; }))
                .andExpect(status().isTooManyRequests());

        // Verify victim account tokens were NOT consumed by the throttled attacker IP!
        // A request from a clean IP for victim should get 401, showing account still has capacity (3 tokens)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(victimReq))
                        .with(request -> { request.setRemoteAddr("172.16.50.99"); return request; }))
                .andExpect(status().isUnauthorized()); // 401 proves account bucket was not depleted by the throttled IP!
    }

    @Test
    @DisplayName("23. Bucket Storage & Memory Boundedness: Cleanup prunes idle buckets while preserving active limiters")
    void testBucketCleanup_idleBucketsEvictedAndActiveThrottlingPreserved() {
        // Create 20 idle buckets
        for (int i = 0; i < 20; i++) {
            rateLimitService.resolveBucket("test:idle:ip:" + i);
        }
        assertThat(rateLimitService.getCacheSize()).isGreaterThanOrEqualTo(20);

        // Consume tokens on an active throttled bucket
        rateLimitService.resolveBucket("login:ip:active-throttled").tryConsume(5);

        // Run cleanup
        int pruned = rateLimitService.cleanup();
        assertThat(pruned).isGreaterThan(0);

        // Verify the active bucket is still present and still throttled (has 0 tokens left)
        assertThat(rateLimitService.resolveBucket("login:ip:active-throttled").tryConsume(1)).isFalse();
    }

    @Test
    @DisplayName("24. Refresh Throttling Safety: 429 response does NOT consume or revoke a valid refresh token")
    void testRefreshThrottled_doesNotRevokeOrConsumeValidRefreshToken() throws Exception {
        String rawToken = refreshTokenService.createRefreshToken(primaryUser);
        String throttledIp = "192.168.99.1"; // Capacity = 4

        // Exhaust IP refresh quota (4 attempts)
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .header("X-InternTrack-Client", "web")
                            .cookie(new Cookie("refreshToken", "dummy_token_" + i))
                            .with(request -> { request.setRemoteAddr(throttledIp); return request; }))
                    .andExpect(status().isUnauthorized());
        }

        // 5th attempt with the VALID refresh token from the throttled IP returns 429
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", rawToken))
                        .with(request -> { request.setRemoteAddr(throttledIp); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"));

        // Prove the valid refresh token was NOT consumed or revoked by the 429 rejection!
        // From an unthrottled IP, the token still successfully rotates!
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-InternTrack-Client", "web")
                        .cookie(new Cookie("refreshToken", rawToken))
                        .with(request -> { request.setRemoteAddr("192.168.99.2"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }
}

