# InternTrack — Phased Implementation Roadmap & Milestones

**Document Status:** Reconciled Project Plan  
**Version:** 1.2.0  

---

## 1. Phased Execution Overview

This roadmap defines the 15-phase sequential implementation strategy for InternTrack.  
Each phase contains independently testable milestones with strict verification criteria. No code is generated for a phase until all preceding phases pass verification.

```
[ Phase 0: Discovery & Arch ] (COMPLETED & VERIFIED)
            │
            ▼
[ Phase 1: Project Init ] ───> [ Phase 2: Database Schema ] ───> [ Phase 3: Auth & Security ]
                                                                               │
                                                                               ▼
[ Phase 6: Interview Hub ] <─── [ Phase 5: Application Pipeline ] <─── [ Phase 4: Opportunity Catalog ]
            │
            ▼
[ Phase 7: Skill Gap Engine ] ─> [ Phase 8: Dashboard & Reminders ] ─> [ Phase 9: React Design & SPA ]
                                                                               │
                                                                               ▼
[ Phase 12: Perf & Accessibility ] <── [ Phase 11: Security Audit ] <── [ Phase 10: Full Test Suite ]
            │
            ▼
[ Phase 13: Prod Packaging ] ──> [ Phase 14: Portfolio Polish ]
```

---

## 2. Phase-by-Phase Breakdown & Acceptance Criteria

---

### Phase 0: Product Discovery & Architecture (STATUS: COMPLETE & VERIFIED)
- **Objective:** Establish PRD, UX research, domain model, API contracts, security architecture, and design tokens with all approved architectural corrections.
- **Deliverables:** Planning artifacts 1 to 10 reconciled in workspace.
- **Verification:** User review and final approval.

---

### Phase 1: Project Initialization
- **Objective:** Initialize version control, Spring Boot 3.3.5 backend (Maven + Java 21 LTS), and React frontend (Vite + JavaScript).
- **Milestones:**
  - `M1.1`: Spring Boot 3.3.5 application structure (`com.interntrack.api`), `pom.xml` with dependencies (Web, Data JPA, Security, MySQL Connector, Validation, JJWT 0.12.x).
  - `M1.2`: React frontend structure (`src/`), Vanilla CSS Design Token base setup.
- **Verification:** Backend compiles with `./mvnw clean compile`; Frontend builds clean with `npm run build`.

---

### Phase 2: Database Schema & Entity Mapping
- **Objective:** Configure MySQL connectivity, schema migrations, and JPA Entity mapping.
- **Milestones:**
  - `M2.1`: DDL schema scripts for all 12 entities (`User`, `UserProfile`, `Skill`, `UserSkill`, `Company`, `Opportunity`, `OpportunitySkill`, `Application`, `Interview`, `Document`, `Reminder`, `RefreshToken`).
  - `M2.2`: JPA Entity classes with relationships, constraints, and audit timestamps (`saved_at`, `applied_at`, etc.).
- **Verification:** Integration test verifying table creation, composite keys, and foreign key cascades.

---

### Phase 3: Authentication & Security Engine
- **Objective:** Implement registration, login, BCrypt hashing ($k=12$), JWT signing, and Refresh Token rotation with reuse detection.
- **Milestones:**
  - `M3.1`: User Registration endpoint (`POST /api/v1/auth/register`) with input validation.
  - `M3.2`: User Login endpoint (`POST /api/v1/auth/login`) issuing JWT access token and HttpOnly refresh cookie.
  - `M3.3`: Refresh Token Rotation endpoint (`POST /api/v1/auth/refresh`) with SHA-256 database hashing and reuse breach invalidation.
  - `M3.4`: JWT Verification Filter enforcing security and populating `SecurityContextHolder`.
- **Verification:** JUnit/Mockito tests verifying login, token expiration, rotation, and rejection of reused/revoked refresh tokens.

---

### Phase 4: Company & Opportunity Catalog API
- **Objective:** Expose endpoints for cataloging companies and job opportunities (supporting global system posts and user custom posts).
- **Milestones:**
  - `M4.1`: Company search and creation endpoints (`/api/v1/companies`).
  - `M4.2`: Opportunity catalog CRUD endpoints (`/api/v1/opportunities`) with provider and custom post attributes.
- **Verification:** API tests for Opportunity creation, updating, and skill association.

---

### Phase 5: Application Tracking Pipeline API
- **Objective:** Core application lifecycle manager supporting stage transitions, dense table filtering, batch status updates, and stage timestamps.
- **Milestones:**
  - `M5.1`: Application creation (`POST /applications`, returning `201 Created` or `409 Conflict` on duplicate). Enforces mandatory `opportunity_id`.
  - `M5.2`: Stage transition updates (`PATCH /applications/{id}/status` and `PATCH /applications/batch-status`).
  - `M5.3`: Service-level ownership validation on all routes preventing IDOR data leaks.
- **Verification:** Unit tests verifying stage transition constraints, timestamp recordings, and user data isolation.

---

### Phase 6: Interview Management API
- **Objective:** Multi-round interview logging, preparation note tracking, and scheduled round management.
- **Milestones:**
  - `M6.1`: Interview round endpoints (`/applications/{id}/interviews`).
  - `M6.2`: Round outcome transition logic (Pending $\rightarrow$ Passed/Failed/Rescheduled).
- **Verification:** Integration tests verifying interview round ordering and application cascading.

---

### Phase 7: Deterministic Skill Gap Engine
- **Objective:** Implement normalized skill gap algorithm (Cases A through D with 75% required / 25% preferred weighting).
- **Milestones:**
  - `M7.1`: Canonical skill string normalization and alias lookup.
  - `M7.2`: Mathematical set intersection engine calculating bounded percentage score and missing skill list.
- **Verification:** Unit tests asserting 100% match, partial match, 0% match, and zero-skill posting edge cases.

---

### Phase 8: Dashboard, Reminders & Funnel Analytics API
- **Objective:** Aggregate metrics for command center dashboard and deadline tasks.
- **Milestones:**
  - `M8.1`: Reminders CRUD endpoints (`/api/v1/reminders`).
  - `M8.2`: Dashboard summary endpoint (`/api/v1/analytics/summary`) and funnel conversion calculator (`/api/v1/analytics/funnel`).
- **Verification:** Unit tests verifying metric calculation accuracy against test datasets.

---

### Phase 9: React Frontend — Architecture & Core Pages
- **Objective:** Build frontend SPA incorporating CSS Design System and API integration.
- **Milestones:**
  - `M9.1`: Global Layout (Sidebar, Top Command Bar, CSS Design Tokens).
  - `M9.2`: Authentication Views (Login, Register).
  - `M9.3`: Applications Workspace (Kanban Pipeline & Dense Multi-Sort Table with batch actions).
  - `M9.4`: Opportunity Catalog & Skill Match Drawer.
  - `M9.5`: Interview Hub & Prep Notes Workspace.
  - `M9.6`: Command Center Dashboard & Reminders Panel.
- **Verification:** Browser preview verification of responsive design, 7 UI states, and keyboard navigation.

---

### Phase 10: Full Test Suite Execution
- **Objective:** End-to-end integration and automated unit testing across all layers using synthetic test fixtures.
- **Milestones:**
  - `M10.1`: Backend test suite with high coverage on business services and security filters.
  - `M10.2`: End-to-end verification of application lifecycle flows.
- **Verification:** All tests pass green.

---

### Phase 11: Security Hardening
- **Objective:** Security audit and OWASP Top 10 compliance review.
- **Milestones:**
  - `M11.1`: Audit CORS configuration, JWT secret handling, and input sanitization.
  - `M11.2`: Rate limiting verification on auth endpoints.
- **Verification:** Zero high/medium security vulnerabilities detected.

---

### Phase 12: Performance & Accessibility Polish
- **Objective:** WCAG 2.1 AA audit and query optimization.
- **Milestones:**
  - `M12.1`: Keyboard navigation sweep (`Tab`, `Enter`, `/`, `j`/`k`).
  - `M12.2`: Database query indexing and lazy loading verification against target benchmarks.
- **Verification:** Lighthouse score $> 90$ in Performance and Accessibility.

---

### Phase 13: Production Packaging
- **Objective:** Production build packaging (Runnable JAR + static React asset bundle).
- **Milestones:**
  - `M13.1`: Maven frontend-maven-plugin packaging or standalone production setup.
- **Verification:** Successfully boot single production artifact and test end-to-end flow.

---

### Phase 14: Portfolio Presentation & Documentation Polish
- **Objective:** Prepare comprehensive README.md, architecture walkthrough, API documentation, and screenshots.
- **Milestones:**
  - `M14.1`: Final project documentation polish.
- **Verification:** Project is portfolio-ready for recruiters and senior engineers.
