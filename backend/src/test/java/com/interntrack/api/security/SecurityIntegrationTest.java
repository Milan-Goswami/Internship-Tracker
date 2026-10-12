package com.interntrack.api.security;

import com.interntrack.api.entity.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=TEST_SECURITY_INTEGRATION_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
})
class SecurityIntegrationTest {

    private static final String TEST_SECRET = "TEST_SECURITY_INTEGRATION_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private AuthenticatedUser regularCandidate;
    private AuthenticatedUser adminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        regularCandidate = new AuthenticatedUser(42L, "candidate@example.com", "Alice Candidate", UserRole.ROLE_USER, true);
        adminUser = new AuthenticatedUser(1L, "admin@example.com", "Admin Boss", UserRole.ROLE_ADMIN, true);
    }

    @Test
    @DisplayName("Health endpoint /api/v1/health is accessible without authentication (200 OK)")
    void healthEndpoint_withoutToken_accessible() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Protected endpoint without Authorization header returns 401 Unauthorized")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required or token invalid"));
    }

    @Test
    @DisplayName("Protected endpoint with valid user JWT authenticates and populates principal (200 OK)")
    void protectedEndpoint_withValidUserToken_returns200AndUserPrincipal() throws Exception {
        String token = jwtService.generateAccessToken(regularCandidate);

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.email").value("candidate@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Protected endpoint with lowercase 'bearer <token>' is supported and authenticates (200 OK)")
    void protectedEndpoint_withLowercaseBearerPrefix_authenticates() throws Exception {
        String token = jwtService.generateAccessToken(regularCandidate);

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    @DisplayName("Protected endpoint with expired JWT returns 401 Unauthorized")
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("42")
                .claim("email", "candidate@example.com")
                .claim("fullName", "Alice")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 200_000))
                .expiration(new Date(System.currentTimeMillis() - 100_000))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint with invalid cryptographic signature returns 401 Unauthorized")
    void protectedEndpoint_withInvalidSignature_returns401() throws Exception {
        String otherSecret = "OTHER_SECRET_0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
        SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
        String forgedToken = Jwts.builder()
                .subject("42")
                .claim("email", "candidate@example.com")
                .claim("fullName", "Alice")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(otherKey, Jwts.SIG.HS512)
                .compact();

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint with malformed JWT returns 401 Unauthorized")
    void protectedEndpoint_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer invalid.jwt.structure"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "Bearer ", "Bearer    ", "bearer", "Basic dXNlcjpwYXNz", "Token abc"})
    @DisplayName("Protected endpoint with malformed or empty Authorization headers returns 401 Unauthorized")
    void protectedEndpoint_withMalformedHeader_returns401(String malformedHeader) throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", malformedHeader))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Admin-only endpoint with ROLE_USER token returns 403 Forbidden")
    void adminEndpoint_withUserRole_returns403Forbidden() throws Exception {
        String userToken = jwtService.generateAccessToken(regularCandidate);

        mockMvc.perform(get("/api/v1/test/admin-only")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Admin-only endpoint with ROLE_ADMIN token returns 200 OK")
    void adminEndpoint_withAdminRole_returns200Ok() throws Exception {
        String adminToken = jwtService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/test/admin-only")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("admin-access-granted"));
    }

    @Test
    @DisplayName("Stateless verification: No session cookie is emitted and subsequent request without token is 401")
    void applicationRemainsStateless_noSessionCreated() throws Exception {
        String token = jwtService.generateAccessToken(regularCandidate);

        MvcResult result = mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getHeader("Set-Cookie")).isNull();
        assertThat(result.getRequest().getSession(false)).isNull();

        // Subsequent request in same client context without token must be rejected with 401
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized());
    }
}