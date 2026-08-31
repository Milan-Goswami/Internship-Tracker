# InternTrack — Security Plan & Threat Mitigation Architecture

**Document Status:** Reconciled Security Specification  
**Version:** 1.1.0  

---

## 1. Authentication & Session Management

### 1.1 Password Hashing Strategy
- **Algorithm:** BCrypt password hashing algorithm with a cost factor of 12 ($k=12$).
- **Salt Generation:** Unique, cryptographically secure 128-bit random salt automatically generated per password hash.
- **Rule:** Plaintext passwords are NEVER logged, printed, or stored in any persistent or transient buffer.

---

### 1.2 JWT & Refresh Token Lifecycle with Reuse Detection

```
+----------------+                +-------------------------+                +---------------------+
|  Client (SPA)  |                |  Spring Security Filter |                |  AuthService / DB   |
+-------+--------+                +------------+------------+                +----------+----------+
        |                                      |                                        |
        | 1. POST /api/v1/auth/login           |                                        |
        |------------------------------------->|                                        |
        |                                      | Verify credentials                     |
        | 2. Returns Access Token (JSON)       | Issue Access JWT + Refresh Token       |
        |    + Refresh Token (HttpOnly Cookie) | Store SHA-256(RefreshToken) in DB      |
        |<-------------------------------------|                                        |
        |                                      |                                        |
        | 3. GET /api/v1/applications          |                                        |
        |    Header: Bearer <Access_Token>     |                                        |
        |------------------------------------->| Verify signature & claims              |
        | 4. Protected Resource Data           | Populate SecurityContextHolder         |
        |<-------------------------------------|                                        |
        |                                      |                                        |
        | 5. POST /api/v1/auth/refresh         |                                        |
        |    Cookie: refreshToken=<token>      |                                        |
        |    Header: X-InternTrack-Client: web |                                        |
        |------------------------------------->| Verify SHA-256(token) in DB            |
        |                                      |                                        |
        |                                      | --- IF TOKEN REVOKED (REUSE DETECTED) -|
        |                                      | -> Invalidate ALL user refresh tokens  |
        |                                      | -> Return 401 Unauthorized             |
        |                                      |                                        |
        |                                      | --- IF TOKEN VALID (ROTATION) -------- |
        |                                      | -> Mark old token revoked              |
        | 6. Returns New Access Token (JSON)   | -> Link replaced_by_token_hash         |
        |    + New Refresh Token (Cookie)      | -> Save new SHA-256(token) to DB       |
        |<-------------------------------------|                                        |
```

#### Token Specifications:
- **Access Token:**
  - *Format:* Compact JWT signed with HMAC-SHA512 (`HS512`).
  - *Lifespan:* 15 Minutes.
  - *Claims:* `sub` (User ID), `email`, `role`, `iat`, `exp`.
  - *Transmission:* `Authorization: Bearer <token>` in HTTP header.
- **Refresh Token Entity:**
  - *Format:* Cryptographically secure 256-bit random string.
  - *Storage:* Stored in database strictly as a **SHA-256 hash** (`token_hash`), never in plaintext.
  - *Lifespan:* 7 Days.
  - *Cookie Attributes:* `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh`.

#### Token Rotation & Reuse Detection Rules:
1. **Single-Use Rotation:** Every successful call to `POST /api/v1/auth/refresh` immediately invalidates the presenting refresh token and issues a new refresh token.
2. **Replaced By Linkage:** The invalidated token record sets `revoked = true`, `revoked_at = NOW()`, and records `replaced_by_token_hash`.
3. **Automated Breach Invalidation:** If a refresh token that has *already been revoked* is presented, the system flags a session hijacking attempt and **immediately revokes all active refresh tokens for that user ID**, forcing re-authentication across all devices.

---

## 2. Authorization & Insecure Direct Object Reference (IDOR) Defense

### 2.1 Role-Based Access Control (RBAC)
- **`ROLE_USER`:** Default candidate role. Strictly isolated to accessing self-owned records.
- **`ROLE_ADMIN`:** Maintenance role for master skill taxonomy, company catalog verification, and system job monitors.

### 2.2 Strict Multi-Tenant Ownership Verification Rule
To prevent IDOR vulnerabilities, **all service-layer methods verify resource ownership against the authenticated principal ID before reading or mutating records**:

```java
@Transactional(readOnly = true)
public ApplicationResponseDTO getApplicationById(Long applicationId, Long authenticatedUserId) {
    Application application = applicationRepository.findById(applicationId)
        .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
    // Strict multi-tenant boundary check
    if (!application.getUser().getId().equals(authenticatedUserId)) {
        throw new AccessDeniedException("Unauthorized access to requested application");
    }
    
    return applicationMapper.toDto(application);
}
```

---

## 3. Defense-in-Depth & Boundary Protection

### 3.1 Input Validation Boundary
All request DTOs enforce strict declarative validation (`jakarta.validation`):
- `@NotBlank`, `@NotNull` on mandatory identifiers and titles.
- `@Email` format verification.
- `@Size(min=8, max=100)` on passwords.
- `@Pattern` on enumerations and URLs to reject malicious injection payloads.

### 3.2 SQL Injection & Cross-Site Scripting (XSS)
- **SQL Injection:** 100% mitigated through Spring Data JPA parameterized queries and Hibernate ORM criteria. Direct string concatenation in SQL/JPQL is strictly prohibited.
- **XSS:** React automatically escapes untrusted dynamic content in JSX. All REST responses emit `Content-Type: application/json;charset=UTF-8`.

### 3.3 CSRF Protection on Cookie Endpoints
- Since access tokens are passed via headers (`Authorization: Bearer`), standard API endpoints are immune to CSRF.
- For the cookie-based `/api/v1/auth/refresh` endpoint, CSRF is mitigated via:
  1. `SameSite=Strict` cookie attribute.
  2. Custom request header validation: The server rejects refresh requests lacking the `X-InternTrack-Client: web` custom header (which cannot be forged in cross-origin simple requests).

### 3.4 Rate Limiting on Authentication Endpoints
- **Bucket4j / Spring Filter:** Token-bucket rate limiter applied to `/api/v1/auth/login` and `/api/v1/auth/register`.
- **Policy:** Maximum 5 failed authentication attempts per IP/username per minute before triggering a 15-minute temporary lockout (`HTTP 429 Too Many Requests`).

---

## 4. Secrets Management & Configuration Security

- **Zero Hardcoded Secrets Policy:** Database passwords, JWT signing keys, and external API keys are NEVER committed to version control.
- **Environment Injection:** Credentials are provided at runtime via environment variables:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - `JWT_SECRET_KEY` (minimum 512-bit secure hex key)
- **Version Control Exclusions:** `.gitignore` enforces exclusion of `.env`, `application-local.yml`, IDE configs, and target build directories.
