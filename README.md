# InternTrack — Internship Application Intelligence Platform

[![Java](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![React](https://img.shields.io/badge/React-18.3.1-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5.4.21-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![WCAG 2.1 AA](https://img.shields.io/badge/Accessibility-WCAG%202.1%20AA%20(100%2F100)-success)](https://www.w3.org/WAI/standards-guidelines/wcag/)
[![Lighthouse Performance](https://img.shields.io/badge/Lighthouse%20Performance-95+-success)](https://developers.google.com/web/tools/lighthouse)

**InternTrack** is a high-performance, full-stack enterprise web application designed for university students, career switchers, and software engineering candidates to manage the internship application lifecycle. It couples a **Java 21 / Spring Boot 4.1.1 Modular Monolith** with a high-density, accessible **React 18 Single Page Application (SPA)**, featuring an in-memory deterministic skill gap analysis engine, dual-tier rate limiting, and single-artifact production packaging.

---

## Table of Contents

1. [Key Features & Capabilities](#key-features--capabilities)
2. [Technology Stack & Exact Versions](#technology-stack--exact-versions)
3. [System Architecture](#system-architecture)
4. [Deterministic Skill Gap Engine](#deterministic-skill-gap-engine)
5. [Security & Authentication Architecture](#security--authentication-architecture)
6. [Database Schema & Query Optimization](#database-schema--query-optimization)
7. [REST API Reference](#rest-api-reference)
8. [Configuration & Environment Variables](#configuration--environment-variables)
9. [Local Development Setup](#local-development-setup)
10. [Production Build & Standalone Fat JAR](#production-build--standalone-fat-jar)
11. [Testing & Verification Suite](#testing--verification-suite)
12. [Architectural Characteristics & Production Considerations](#architectural-characteristics--production-considerations)

---

## Key Features & Capabilities

- **Interactive Kanban & Dense Table Workspace:** Track applications through dynamic recruitment stages (`SAVED`, `APPLIED`, `SCREENING`, `INTERVIEWING`, `OFFER`, `REJECTED`, `WITHDRAWN`) with real-time drag-and-drop / stage dropdowns, multi-column sorting, batch status mutations, and pagination.
- **Deterministic Skill Gap Matching:** Instantly computes a candidate's qualification percentage against posting requirements using normalized set intersection with weighted scoring (75% required / 25% preferred) and identifies missing skills in $<10\text{ms}$.
- **Command Center Dashboard & Funnel Intelligence:** Real-time analytics displaying application counts by stage, conversion rates across recruitment pipeline funnels, and upcoming deadlines.
- **Interview Hub & Prep Notes:** Centralized preparation workspace tracking interview rounds, scheduled dates, interviewers, preparation topics, and post-interview reflections.
- **Reminders & Deadline Tracker:** Track application deadlines and follow-up tasks with optimistic UI updates and filterable task lists (`ALL`, `UPCOMING`, `OVERDUE`, `COMPLETED`).
- **Document Registry:** Associate tailored resume and cover letter versions with individual applications while enforcing protocol sanitization.
- **Accessibility & Keyboard Ergonomics (WCAG 2.1 AA):** Roving `tabindex` on dense tables, `j`/`k` keyboard navigation, `/` or `Ctrl+K` global command search, Escape key modal trapping with focus restoration, and 100/100 Lighthouse Accessibility scores.

---

## Technology Stack & Exact Versions

| Component / Layer | Technology | Exact Version | Rationale & Characteristics |
| :--- | :--- | :--- | :--- |
| **Backend Runtime** | OpenJDK | `Java 21 LTS` | Modern Java features (Records, Pattern Matching, Sequenced Collections). |
| **Backend Framework** | Spring Boot | `4.1.1` | Native Jakarta EE 10 baseline, Spring Framework 7, Spring Security 7. |
| **Persistence / ORM** | Spring Data JPA / Hibernate | `7.4.5.Final` | Type-safe repositories, parameterized JPQL queries, HikariCP connection pooling. |
| **Database** | MySQL | `8.0+` | InnoDB transactional engine, composite functional indexing, foreign key cascade constraints. |
| **Security & Tokens** | JJWT | `0.12.6` | Stateless HMAC-SHA512 access tokens with cryptographic signing. |
| **Rate Limiting** | Bucket4j Core | `8.10.1` | Token-bucket algorithm for per-IP and per-account brute-force defense. |
| **Frontend Framework** | React | `18.3.1` | Functional components, hooks, Context API state management. |
| **Frontend Router** | React Router DOM | `6.26.2` | Declarative client-side routing, protected and public route guards. |
| **Build Tooling** | Vite | `5.4.21` | Sub-second HMR and optimized Rollup production builds. |
| **HTTP Client** | Axios | `1.7.7` | Configured with automatic base URL routing and cookie credentials. |
| **Design System** | Custom Vanilla CSS | Design Tokens | High information density, 4px grid rhythm, `#0B0F17` dark slate canvas. |
| **Testing Tools** | JUnit 5 / Vitest / Playwright | Latest Stable | 508 backend tests, 68 Vitest unit tests, 13 Playwright Chromium E2E tests. |

---

## System Architecture

InternTrack is architected as a **Modular Monolith** with clear architectural boundaries between security, presentation, business services, and persistence layers:

```
+-------------------------------------------------------------------------------+
|                            REACT SINGLE PAGE APP (SPA)                        |
|  [React Router v6] -> [AuthContext] -> [Axios Interceptor] -> [Vanilla CSS]   |
+---------------------------------------+---------------------------------------+
                                        | HTTP REST (JSON) / Same-Origin
                                        v
+-------------------------------------------------------------------------------+
|                      SPRING BOOT BACKEND RUNTIME (v4.1.1)                      |
|                                                                               |
|  +-------------------------------------------------------------------------+  |
|  |                     SECURITY & RATE LIMITING FILTER                     |  |
|  |   Bucket4j Rate Limiter -> JwtAuthenticationFilter -> SecurityContext   |  |
|  +------------------------------------+------------------------------------+  |
|                                       |                                       |
|  +------------------------------------v------------------------------------+  |
|  |                     PRESENTATION LAYER (REST CONTROLLERS)               |  |
|  |  AuthController | ApplicationController | OpportunityController         |  |
|  |  InterviewController | SkillController | ReminderController             |  |
|  |  AnalyticsController | DocumentController | SpaWebMvcConfig (SPA Fallback)|
|  +------------------------------------+------------------------------------+  |
|                                       | Input Validation & DTO Isolation      |
|  +------------------------------------v------------------------------------+  |
|  |                     BUSINESS SERVICE & DOMAIN LAYER                     |  |
|  |  AuthenticationService | ApplicationService | DeterministicSkillEngine  |  |
|  |  InterviewService | ReminderService | AnalyticsService                  |  |
|  +------------------------------------+------------------------------------+  |
|                                       | Spring Data JPA Repositories          |
|  +------------------------------------v------------------------------------+  |
|  |                     DATA ACCESS LAYER (HIBERNATE ORM)                   |  |
|  |  HikariCP Connection Pool -> MySQL Dialect -> Prepared Statements       |  |
|  +------------------------------------+------------------------------------+  |
+---------------------------------------+---------------------------------------+
                                        | TCP/IP (Port 3306)
                                        v
+-------------------------------------------------------------------------------+
|                              MYSQL 8.0 DATABASE                               |
|        12 Relational Tables | Composite B-Tree Indexes | Foreign Keys         |
+-------------------------------------------------------------------------------+
```

---

## Deterministic Skill Gap Engine

Rather than relying on non-deterministic external LLMs or heuristic guesses, the skill gap calculator evaluates candidate profiles against opportunity job postings using a **mathematical set intersection algorithm**:

1. **Skill Canonicalization:** Skills are normalized into canonical lowercase strings (e.g., `"react.js"` and `"ReactJS"` normalize to `react`).
2. **Set Partitioning:** Posting skills are split into disjoint sets:
   $$\text{Required} \quad (R) \quad \text{and} \quad \text{Preferred} \quad (P)$$
3. **Weight Allocation:**
   - **Case A (Both Required & Preferred present):**
     $$\text{Score} = 0.75 \times \frac{|U \cap R|}{|R|} + 0.25 \times \frac{|U \cap P|}{|P|}$$
   - **Case B (Only Required present):**
     $$\text{Score} = \frac{|U \cap R|}{|R|}$$
   - **Case C (Only Preferred present):**
     $$\text{Score} = \frac{|U \cap P|}{|P|}$$
   - **Case D (Zero skills listed on posting):** Score defaults to $100\%$ with empty missing list.
4. **Missing Skills Extraction:** All required skills not found in candidate skills ($R \setminus U$) are returned in order for immediate resume targeting.

---

## Security & Authentication Architecture

- **Stateless Bearer JWTs:** Authenticated requests carry an HMAC-SHA512 signed JWT in the `Authorization: Bearer <token>` header, expiring in 15 minutes (`900,000ms`).
- **HttpOnly Refresh Token Rotation:** Long-lived refresh tokens (7 days) are stored in secure, HttpOnly cookies with database-persisted token hashes (`uk_refresh_token_hash`). Automatic reuse detection revokes all active family tokens if a compromised token is replayed.
- **Dual-Tier Bucket4j Rate Limiting:**
  - `POST /api/v1/auth/login`: Guarded by both IP capacity (10 tokens, 10 min refill) and account email capacity (5 tokens, 15 min refill).
  - `POST /api/v1/auth/register`: Guarded by IP capacity (3 tokens, 60 min refill).
- **Password Hashing:** Passwords hashed with BCrypt ($k=12$).
- **SPA Client Route Fallback & API Isolation:** Spring Security permits public static assets (`/assets/**`, `/index.html`, `/robots.txt`) and client SPA routes (`/dashboard`, `/applications`, etc.) while strictly protecting `/api/**` endpoints.

---

## Database Schema & Query Optimization

The database consists of **12 normalized tables** created via reproducible DDL ([schema.sql](backend/src/main/resources/schema.sql)):

1. `users`: Credentials, full name, role, timestamps.
2. `user_profiles`: Target job title, target location, target salary ranges, preferred work mode.
3. `companies`: Enterprise catalog with canonical names, industries, and headquarters.
4. `skills`: Standardized taxonomy of technical and soft skills.
5. `opportunities`: Job postings with compensation, deadlines, location type, and SHA-256 deduplication hashes (`raw_hash`).
6. `opportunity_skills`: Many-to-many junction table tagging required vs. preferred skills.
7. `user_skills`: Candidate skill matrix with proficiency levels and experience years.
8. `applications`: Application status state machine with document links and ratings.
9. `interviews`: Interview rounds, scheduling, interviewers, prep notes, and outcomes.
10. `documents`: Resumes and cover letters with protocol-sanitized storage paths.
11. `reminders`: Follow-up deadlines, completion flags, and application associations.
12. `refresh_tokens`: Refresh token hash storage with revocation flags.

### Query Performance Benchmarks
- Composite index `idx_app_user_updated (user_id, updated_at)` ensures $<150\text{ms}$ paginated retrieval across large application datasets.
- Composite index `idx_reminder_user_type_comp (user_id, completed, reminder_type)` powers instant dashboard deadline queries.
- Dashboard analytics executes in $<250\text{ms}$ via consolidated JPQL aggregation queries.

---

## REST API Reference

All REST endpoints are prefixed with `/api/v1`.

### 1. Authentication (`/api/v1/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public (Rate-Limited) | Register candidate account (`fullName`, `email`, `password`). |
| `POST` | `/api/v1/auth/login` | Public (Rate-Limited) | Authenticate user; returns JWT and sets refresh cookie. |
| `POST` | `/api/v1/auth/refresh` | Public | Rotates refresh token and issues new access token. |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revokes current refresh token family. |
| `GET` | `/api/v1/auth/me` | Authenticated | Fetch authenticated user profile details. |

### 2. Applications (`/api/v1/applications`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/applications` | Authenticated | List user applications with optional `status` filter. |
| `GET` | `/api/v1/applications/{id}` | Authenticated | Get application details by ID. |
| `POST` | `/api/v1/applications` | Authenticated | Create application for an opportunity. |
| `PUT` | `/api/v1/applications/{id}` | Authenticated | Update application details (notes, ratings, documents). |
| `PATCH` | `/api/v1/applications/{id}/status` | Authenticated | Mutate application status (`SAVED` $\to$ `OFFER`). |
| `DELETE` | `/api/v1/applications/{id}` | Authenticated | Delete application record. |

### 3. Opportunities (`/api/v1/opportunities`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/opportunities` | Authenticated | Search opportunities with keyword, location, and type filters. |
| `GET` | `/api/v1/opportunities/{id}` | Authenticated | Get opportunity details and required skill breakdown. |
| `POST` | `/api/v1/opportunities` | Authenticated | Create a custom opportunity posting. |

### 4. Skill Gap & Taxonomy (`/api/v1/skills`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/skills` | Authenticated | List all standardized skills in catalog. |
| `GET` | `/api/v1/skills/user` | Authenticated | List candidate's personal skills. |
| `POST` | `/api/v1/skills/user` | Authenticated | Add skill to candidate profile. |
| `DELETE` | `/api/v1/skills/user/{id}` | Authenticated | Remove skill from profile. |
| `GET` | `/api/v1/skills/gap/{opportunityId}` | Authenticated | Calculate match percentage and missing skills. |

### 5. Interviews (`/api/v1/interviews`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/interviews` | Authenticated | List interviews for user's applications. |
| `GET` | `/api/v1/interviews/{id}` | Authenticated | Get interview round details. |
| `POST` | `/api/v1/interviews` | Authenticated | Schedule interview round for an application. |
| `PUT` | `/api/v1/interviews/{id}` | Authenticated | Update interview round details, notes, or outcome. |
| `DELETE` | `/api/v1/interviews/{id}` | Authenticated | Delete interview round. |

### 6. Reminders (`/api/v1/reminders`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/reminders` | Authenticated | List reminders with status/date filters. |
| `POST` | `/api/v1/reminders` | Authenticated | Create reminder with due date and type. |
| `PATCH` | `/api/v1/reminders/{id}/complete` | Authenticated | Toggle reminder completion status. |
| `DELETE` | `/api/v1/reminders/{id}` | Authenticated | Delete reminder. |

### 7. Analytics & Intelligence (`/api/v1/analytics`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/summary` | Authenticated | Aggregate dashboard metrics and counts by status. |
| `GET` | `/api/v1/analytics/funnel` | Authenticated | Pipeline conversion funnel metrics. |

### 8. Documents & Settings (`/api/v1/documents`, `/api/v1/users`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/documents` | Authenticated | List uploaded resumes and cover letters. |
| `POST` | `/api/v1/documents` | Authenticated | Register document metadata and storage path. |
| `DELETE` | `/api/v1/documents/{id}` | Authenticated | Remove document reference. |
| `GET` | `/api/v1/users/profile` | Authenticated | Get candidate career preferences profile. |
| `PUT` | `/api/v1/users/profile` | Authenticated | Update career preferences and target salary. |
| `GET` | `/api/v1/health` | Public | System health status (`{"status":"UP"}`). |

---

## Configuration & Environment Variables

The backend application is configured via `application.yml` and accepts the following environment variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP port on which the Spring Boot server listens. |
| `DB_URL` | `jdbc:mysql://localhost:3306/interntrack_db...` | JDBC URL for MySQL database connection. |
| `DB_USERNAME` | `root` | Database username. |
| `DB_PASSWORD` | *(empty)* | Database password. |
| `JWT_SECRET_KEY` | *(auto-generated dev key)* | HMAC-SHA512 secret string (must be at least 64 bytes). |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | `900000` (15 min) | Access token expiration duration in milliseconds. |
| `COOKIE_SECURE` | `false` (set `true` in prod HTTPS) | Sets `Secure` flag on refresh token cookie. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed origins for development dev-server proxies. |
| `RATELIMIT_LOGIN_IP_CAPACITY` | `10` | Allowed login attempts per IP per 10-minute window. |
| `RATELIMIT_LOGIN_ACCOUNT_CAPACITY` | `5` | Allowed login attempts per email per 15-minute window. |
| `RATELIMIT_REGISTER_IP_CAPACITY` | `3` | Allowed registrations per IP per 60-minute window. |

---

## Local Development Setup

### Prerequisites
- **Java 21 LTS** (`java -version`)
- **Node.js 18+ & npm** (`node -v`)
- **MySQL 8.0+** running on `localhost:3306`

### 1. Database Initialization
Create a MySQL database and execute the schema DDL:
```sql
CREATE DATABASE interntrack_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Tables and initial seed records are automatically applied by Spring Boot from `schema.sql` and `data.sql` when `spring.sql.init.mode=always`.

### 2. Start Backend Development Server
```powershell
cd backend
$env:PORT = "8080"
$env:DB_URL = "jdbc:mysql://localhost:3306/interntrack_dev?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
$env:JWT_SECRET_KEY = "THIS_IS_A_DEV_SECRET_KEY_REPLACE_WITH_A_LONG_SECURE_KEY_IN_PRODUCTION"
.\mvnw.cmd spring-boot:run
```
Backend API will be live at `http://localhost:8080/api/v1/health`.

### 3. Start Frontend Development Server
In a separate terminal:
```powershell
cd frontend
npm install
npm run dev
```
Vite dev server will start at `http://localhost:5173`. Proxied API calls to `/api` automatically route to `http://localhost:8080`.

---

## Production Build & Standalone Fat JAR

InternTrack packages both the compiled React SPA static assets and the Spring Boot API backend into a **single, standalone runnable Fat JAR**.

### 1. Build & Package Sequence
```powershell
# Step 1: Compile the React frontend production bundle
cd frontend
npm run build
cd ..

# Step 2: Package the Spring Boot Fat JAR with embedded static assets
cd backend
.\mvnw.cmd clean package -DskipTests
cd ..
```
This produces `backend/target/interntrack-api-0.0.1-SNAPSHOT.jar`, which bundles:
- `BOOT-INF/classes/static/index.html` (SPA entry point)
- `BOOT-INF/classes/static/assets/*.js` and `*.css` (Optimized frontend chunks)
- Full Spring Boot backend runtime and dependencies

### 2. Execute Standalone Production JAR
Run the single production artifact on any chosen port without needing Node.js or Vite:
```powershell
java -Dserver.port=8080 `
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/interntrack_demo_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC" `
  -Dspring.datasource.username="root" `
  -Dspring.datasource.password="your_mysql_password" `
  -Dsecurity.jwt.secret="YOUR_PRODUCTION_HMAC_SHA512_SECRET_KEY_MINIMUM_64_CHARACTERS" `
  -jar backend/target/interntrack-api-0.0.1-SNAPSHOT.jar
```
*(Note: Always use an isolated development or demo database like `interntrack_demo_db` or `interntrack_dev`. Never point commands to the protected `interntrack_db`.)*
Navigate to `http://localhost:8080/` in any browser. All SPA client-side routes (`/dashboard`, `/applications`, `/interviews`, etc.) and REST APIs (`/api/v1/**`) will operate seamlessly from this single port.

---

## Testing & Verification Suite

InternTrack enforces quality across unit, integration, accessibility, and end-to-end testing layers.

### 1. Backend Test Suite (JUnit 5 + MockMvc)
Executes 508 unit and integration tests against an isolated H2 in-memory database:
```powershell
cd backend
.\mvnw.cmd test
```
To run the SPA routing integration test suite specifically:
```powershell
.\mvnw.cmd test -Dtest=SpaRoutingIntegrationTest
```

### 2. Frontend Unit Test Suite (Vitest + Testing Library)
Executes 68 tests across keyboard navigation, roving tabindex, modal traps, and batch updates:
```powershell
cd frontend
npm run test
```

### 3. Chromium End-to-End Suite (Playwright)
Executes 13 browser integration tests verifying UI flows and accessibility:
```powershell
cd frontend
npx playwright test --project=chromium
```

### 4. Accessibility & Performance Benchmarks (Local Observations)
- **Local Lighthouse Accessibility Score:** `100 / 100` on local audited routes.
- **Local Lighthouse Performance Score:** `95+` observed on populated application views under local test conditions (local benchmark observations, not production guarantees).
- **Accessibility Standards:** Built toward **WCAG 2.1 AA** standards (4.5:1 text contrast, accessible form labels, explicit aria-expanded/aria-controls bindings). Automated tests and Lighthouse audits verify core automated criteria; full formal compliance requires comprehensive manual assistive-technology evaluation.

---

## Architectural Characteristics & Production Considerations

- **HTTPS & Secure Cookie Policy:** In local development, `COOKIE_SECURE=false` allows refresh tokens to be set over HTTP. When deploying behind a reverse proxy (e.g., Nginx, AWS ALB, Cloudflare), set `COOKIE_SECURE=true`. The application pre-configures `server.forward-headers-strategy=framework` to seamlessly process `X-Forwarded-Proto` and `X-Forwarded-For` headers.
- **Single-Node In-Memory Rate Limiting:** The Bucket4j rate-limiting engine stores bucket tokens in JVM memory. This provides nanosecond token checks without network hops for single-node deployments. If horizontally scaling across multiple container replicas, an upstream reverse proxy rate limiter or Redis-backed Bucket4j extension should be configured.
- **Document Storage Abstraction:** The `documents` table stores document metadata and protocol-sanitized storage paths (`storage_path`). For cloud-native deployments, the document handler can be pointed to an S3/GCS presigned URL provider without altering relational references.
