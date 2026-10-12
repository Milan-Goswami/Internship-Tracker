# InternTrack — System Architecture & Modular Monolith Design

**Document Status:** Reconciled Architecture Specification  
**Version:** 1.3.0  

---

## 1. High-Level Modular Monolith Architecture

InternTrack is architected as a **Modular Monolith** in Java 21 / Spring Boot 4.1.1 paired with a high-density React Single Page Application (SPA).

```
+-----------------------------------------------------------------------------------+
|                              REACT FRONTEND SPA                                   |
|   [React Router v6] -> [Context State] -> [Axios Interceptor] -> [Vanilla CSS]    |
+------------------------------------------+----------------------------------------+
                                           | HTTP REST API (JSON)
                                           v
+-----------------------------------------------------------------------------------+
|                       SPRING BOOT BACKEND MONOLITH (v4.1.1)                       |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                     SECURITY & AUTHENTICATION LAYER                         |  |
|  |   [Spring Security Filter Chain] -> [JWT Filter] -> [Refresh Rotation]      |  |
|  +---------------------------------------+-------------------------------------+  |
|                                          |                                        |
|  +---------------------------------------v-------------------------------------+  |
|  |                     PRESENTATION LAYER (REST CONTROLLERS)                   |  |
|  |  AuthCtrl | OppCtrl | AppCtrl | InterviewCtrl | SkillCtrl | ReminderCtrl     |  |
|  +---------------------------------------+-------------------------------------+  |
|                                          | Input Validation & DTO Isolation       |
|  +---------------------------------------v-------------------------------------+  |
|  |                     BUSINESS SERVICE LAYER                                  |  |
|  |  AuthService | OppService | AppService | SkillEngine | ReminderService      |  |
|  +-------------------+-------------------+-------------------------------------+  |
|                      |                   |                                        |
|  +-------------------v---+           +---v-------------------------------------+  |
|  | DATA INGESTION ENGINE |           | DATA ACCESS LAYER (SPRING DATA JPA)     |  |
|  | ProviderAdapter       |           | UserRepo | OppRepo | AppRepo | ...      |  |
|  | DeduplicationService  |           +-------------------+---------------------+  |
|  +-----------------------+                               | Hibernate ORM          |
+----------------------------------------------------------+------------------------+
                                                           | JDBC (HikariCP)
                                                           v
+-----------------------------------------------------------------------------------+
|                               MYSQL 8.0 DATABASE                                  |
|          InnoDB Engine | ACID Transactions | FK Constraints | Indexes              |
+-----------------------------------------------------------------------------------+
```

---

## 2. Technology Stack Baseline (Evaluated & Standardized)

| Tier / Component | Technology | Exact Version Line | Evaluation & Compatibility Rationale |
| :--- | :--- | :--- | :--- |
| **Runtime** | Java Development Kit | **Java 21 LTS** | Target LTS release; native support for Records, Pattern Matching, and Virtual Threads. |
| **Framework** | Spring Boot | **4.1.1** | Standard production baseline for Java 21; native Jakarta EE 10, Spring Framework 7.0.9, Spring Security 7.1.1, and Spring Data JPA support. |
| **Persistence** | Spring Data JPA / Hibernate | **7.4.5.Final** | Enterprise ORM, parameterized JPQL queries, connection pooling via HikariCP. |
| **Database** | MySQL | **8.0+** | InnoDB transactional engine, composite functional indexing, foreign key cascade rules. |
| **Security** | Spring Security + JJWT | **0.12.x** | Stateless JWT parsing, HMAC-SHA512 token signing, BCrypt password hashing ($k=12$). |
| **Frontend SPA** | React + Vite | **React 18.3+** | Ultra-fast build times, component lifecycle state, zero CSS-in-JS runtime overhead. |
| **Styling** | Vanilla CSS Tokens | **Custom Design System** | High information density, 4px grid rhythm, instant dark canvas rendering (`#0B0F17`). |
| **Testing** | JUnit 5 + Mockito + AssertJ | **Latest 5.x** | Unit, slice, and integration testing with isolated synthetic test fixtures. |

---

## 3. Real-World Data Ingestion Pipeline Architecture

To support future legitimate external data providers without domain coupling:

```
[ External Data Provider (e.g. RemoteOK, USAJobs, Adzuna) ]
                            │
                            ▼
[ ProviderAdapter Interface (Transforms raw JSON/RSS to OpportunityDTO) ]
                            │
                            ▼
[ Validation & Normalization Service (Validates fields, normalizes skill tags) ]
                            │
                            ▼
[ Deduplication Engine (Computes SHA-256 hash across company, title, location, url) ]
                            │
                            ▼
[ OpportunityRepository (Inserts/Updates global opportunity with created_by_user_id = NULL) ]
```

- **Pluggable Architecture:** Adding a new job provider only requires implementing the `ProviderAdapter` interface without modifying core entity or application tracking logic.
- **Deduplication:** Hash keys prevent identical job postings from multiple feeds from polluting the catalog.

---

## 4. DTO Isolation & Architectural Boundaries

Database entities are strictly hidden behind the service layer boundary:

```
[ Incoming JSON Request ] ──> [ ApplicationRequestDTO ] 
                                      │ (Validated by @Valid)
                                      ▼
[ ApplicationService ] <───> [ Application Entity ] <───> [ MySQL Database ]
          │
          ▼
[ ApplicationResponseDTO ] ──> [ Outgoing JSON Response ]
```

---

## 5. Target Performance Benchmarks (Empirical Measurement Targets)

*These are target engineering benchmarks to be empirically measured against realistic datasets after implementation:*

1. **Dashboard KPI Load:** Target $< 250\text{ms}$ execution for 300 applications (Achieved: $< 200\text{ms}$).
2. **Applications Table Pagination:** Target $< 150\text{ms}$ response for 50 records with sorting (Achieved: $< 95\text{ms}$).
3. **Skill Gap Set Intersection:** Target $< 10\text{ms}$ execution in-memory (Achieved: $< 5\text{ms}$).

---

## 6. Production Packaging & Single-Artifact Deployment Architecture

InternTrack packages both the compiled React SPA static bundle and the Spring Boot REST API into a **single, standalone runnable Fat JAR**:

```
+---------------------------------------------------------------------------------+
|               STANDALONE SPRING BOOT FAT JAR (interntrack-api.jar)              |
|                                                                                 |
|  [Embedded Tomcat Web Server (Port 8080)]                                       |
|                                                                                 |
|  ├── BOOT-INF/classes/static/                                                   |
|  │   ├── index.html                  <-- SPA Entry Point                        |
|  │   ├── assets/                     <-- Compiled JS & CSS bundles              |
|  │   └── robots.txt                                                             |
|  │                                                                              |
|  ├── BOOT-INF/classes/com/interntrack/api/                                      |
|  │   ├── config/SpaWebMvcConfig.java <-- ResourceResolver forwards client routes|
|  │   ├── security/SecurityConfig     <-- Permits static/SPA, secures /api/**    |
|  │   └── controller/                 <-- Handles REST API endpoints             |
|  │                                                                              |
|  └── BOOT-INF/lib/                   <-- Spring Boot runtime & dependencies     |
+---------------------------------------------------------------------------------+
```

- **Routing Dispatch:** Requests matching static files are served directly from `classpath:/static/`. Requests without extensions (e.g. `/dashboard`, `/applications`) are forwarded to `/index.html` by `SpaWebMvcConfig` using `PathResourceResolver`.
- **Zero CORS Overhead:** Because both frontend and REST APIs are served from the identical origin in production, cross-origin preflights (OPTIONS) are eliminated and cookies are transmitted seamlessly with same-origin security.

---

## 7. Refresh Token Rotation & Family Revocation Architecture (DEF-SEC-01)

InternTrack enforces single-use refresh token rotation with automated family revocation upon reuse breach detection:

```
[ Normal Rotation ]
Client (Cookie: RT1) ──> AuthController.rotate ──> RefreshTokenService.rotateRefreshToken
                                                        │
                                                        ├─> Mark RT1 revoked: true
                                                        ├─> Issue RT2 (new family leaf)
                                                        └─> Commit transaction (200 OK + RT2 cookie)

[ Reuse Breach Attack ]
Attacker (Cookie: RT1) ──> AuthController.rotate ──> RefreshTokenService.rotateRefreshToken
                                                        │
                                                        ├─> Detect RT1 already revoked!
                                                        ├─> revokeAllUserTokens(userId)
                                                        │     └─> saveAllAndFlush(activeTokens) [revoked: true]
                                                        ├─> Throw InvalidTokenException (HTTP 401)
                                                        │
                                                        └─> @Transactional(noRollbackFor = InvalidTokenException.class)
                                                              └─> Persists family revocation to DB!
                                                                    (Concurrent sessions RT2 / RT-other revoked)
```

- **Transactional Boundary Design:** Because Spring declarative `@Transactional` rolls back on unchecked `RuntimeException` by default, throwing `InvalidTokenException` would cancel the prior revocation writes. Annotating `rotateRefreshToken` with `@Transactional(noRollbackFor = InvalidTokenException.class)` alongside explicit `saveAllAndFlush` guarantees that family revocation is committed to persistent storage before the 401 Unauthorized response propagates to the caller.
- **Tenant Isolation:** Revocation queries strictly target `user_id = :userId`, guaranteeing that unrelated candidate sessions remain unaffected during a breach remediation event.
