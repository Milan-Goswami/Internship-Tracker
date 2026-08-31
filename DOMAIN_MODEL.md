# InternTrack — Conceptual Domain Model & Relational Architecture

**Document Status:** Reconciled Architecture Specification  
**Version:** 1.1.0  

---

## 1. Conceptual Domain Overview

InternTrack maintains a strict **Three-Tier Relational Hierarchy**:

```
+-----------------------------------------------------------------------------------+
| 1. COMPANY REPOSITORY                                                             |
|    - Canonical employer profiles (Name, Domain, Logo, Industry)                   |
+------------------------------------------+----------------------------------------+
                                           | 1 : N
                                           v
+-----------------------------------------------------------------------------------+
| 2. OPPORTUNITY CATALOG (Objective Job Specifications)                             |
|    - Sourced from: Official Data Providers (System) OR User Custom Creation       |
|    - Ownership: System Global (created_by_user_id = NULL) OR                      |
|                 User Custom (created_by_user_id = User.id)                        |
|    - Attributes: Title, Description, Work Mode, Location, Salary, Deadline, Skills|
+------------------------------------------+----------------------------------------+
                                           | 1 : N
                                           v
+-----------------------------------------------------------------------------------+
| 3. USER APPLICATION LIFECYCLE (Candidate-Specific State)                          |
|    - Candidate: User (Strict Private Ownership)                                   |
|    - Status: SAVED -> APPLIED -> SCREENING -> INTERVIEWING -> OFFER -> ...         |
|    - Stage History: saved_at, applied_at, screening_at, interviewing_at, closed_at|
|    - Associated: Interviews (1:N), Documents (N:1), Reminders (1:N), Notes        |
+-----------------------------------------------------------------------------------+
```

---

## 2. Entity Specifications

### 2.1 `User`
- **Purpose:** Core identity and authentication principal.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `email` (VARCHAR(255), Unique, Not Null)
  - `password_hash` (VARCHAR(255), Not Null)
  - `full_name` (VARCHAR(100), Not Null)
  - `role` (ENUM: ROLE_USER, ROLE_ADMIN, Not Null, Default ROLE_USER)
  - `is_active` (BOOLEAN, Default True)
  - `created_at` (TIMESTAMP, Not Null)
  - `updated_at` (TIMESTAMP, Not Null)
- **Relationships:**
  - `1 : 1` with `UserProfile` (Cascade ALL)
  - `1 : N` with `UserSkill` (Cascade ALL)
  - `1 : N` with `Opportunity` (Custom created opportunities)
  - `1 : N` with `Application` (Cascade ALL)
  - `1 : N` with `Document` (Cascade ALL)
  - `1 : N` with `Reminder` (Cascade ALL)
  - `1 : N` with `RefreshToken` (Cascade ALL)

### 2.2 `UserProfile`
- **Purpose:** Candidate preferences and target search parameters.
- **Attributes:**
  - `id` (BIGINT, PK, FK -> User.id)
  - `target_role_title` (VARCHAR(150))
  - `preferred_locations` (VARCHAR(255))
  - `target_salary_min` (DECIMAL(12,2), Optional)
  - `target_salary_max` (DECIMAL(12,2), Optional)
  - `preferred_work_mode` (ENUM: REMOTE, HYBRID, ONSITE, ANY, Default ANY)
  - `updated_at` (TIMESTAMP, Not Null)

### 2.3 `Company`
- **Purpose:** Normalized employer entity to eliminate redundant company records.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `name` (VARCHAR(150), Not Null)
  - `canonical_name` (VARCHAR(150), Not Null, Lowercase stripped for dedup)
  - `domain` (VARCHAR(100), Nullable, e.g. "stripe.com")
  - `logo_url` (VARCHAR(500), Nullable)
  - `industry` (VARCHAR(100), Nullable)
  - `headquarters_location` (VARCHAR(150), Nullable)
  - `created_at` (TIMESTAMP, Not Null)
- **Unique Constraint:** `uk_company_canonical` on (`canonical_name`)

### 2.4 `Skill`
- **Purpose:** Master taxonomy of technical skills for standardized matching.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `name` (VARCHAR(100), Not Null, e.g. "React", "Spring Boot")
  - `canonical_name` (VARCHAR(100), Not Null, Unique, e.g. "react", "springboot")
  - `category` (ENUM: LANGUAGE, FRAMEWORK, DATABASE, TOOL, CLOUD, METHODOLOGY)
- **Unique Constraint:** `uk_skill_canonical` on (`canonical_name`)

### 2.5 `UserSkill` (Junction)
- **Purpose:** Candidate's verified/claimed skills.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `user_id` (BIGINT, Not Null, FK -> User.id)
  - `skill_id` (BIGINT, Not Null, FK -> Skill.id)
  - `proficiency_level` (ENUM: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, Default INTERMEDIATE)
  - `years_of_experience` (INT, Nullable)
- **Unique Constraint:** `uk_user_skill` on (`user_id`, `skill_id`)

### 2.6 `Opportunity`
- **Purpose:** Objective job/internship posting specification.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `company_id` (BIGINT, Not Null, FK -> Company.id)
  - `created_by_user_id` (BIGINT, Nullable, FK -> User.id) — `NULL` for global/system opportunities, `User.id` for user-created custom posts.
  - `is_custom` (BOOLEAN, Not Null, Default False) — `True` if created manually by a user.
  - `source_provider` (VARCHAR(50), Not Null, Default "USER_MANUAL") — Identifier for ingestion provider (e.g. "USER_MANUAL", "REMOTEOK", "USAJOBS").
  - `external_source_id` (VARCHAR(255), Nullable) — ID from external provider (must be evaluated together with `source_provider`).
  - `title` (VARCHAR(200), Not Null)
  - `description` (TEXT, Nullable — snapshot of job requirements)
  - `location_type` (ENUM: REMOTE, HYBRID, ONSITE, Not Null)
  - `location_city` (VARCHAR(150), Nullable)
  - `stipend_amount` (DECIMAL(12,2), Nullable)
  - `stipend_currency` (VARCHAR(10), Default "USD")
  - `stipend_period` (ENUM: HOURLY, MONTHLY, YEARLY, Default HOURLY)
  - `duration_months` (INT, Nullable, e.g. 3)
  - `application_deadline` (TIMESTAMP, Nullable)
  - `source_url` (VARCHAR(1000), Nullable)
  - `status` (ENUM: ACTIVE, EXPIRED, ARCHIVED, Not Null, Default ACTIVE)
  - `raw_hash` (VARCHAR(64), Nullable — SHA256 deduplication hash)
  - `created_at` (TIMESTAMP, Not Null)
  - `updated_at` (TIMESTAMP, Not Null)
  - `expires_at` (TIMESTAMP, Nullable)
- **Constraints & Indexes:**
  - `uk_opp_provider_ext_id` on (`source_provider`, `external_source_id`)
  - `idx_opp_raw_hash` on (`raw_hash`)
  - `idx_opp_status_deadline` on (`status`, `application_deadline`)

### 2.7 `OpportunitySkill` (Junction)
- **Purpose:** Skills tagged on an opportunity with requirement weighting.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `opportunity_id` (BIGINT, Not Null, FK -> Opportunity.id)
  - `skill_id` (BIGINT, Not Null, FK -> Skill.id)
  - `is_required` (BOOLEAN, Not Null, Default True) — `True` = Required (75% weight), `False` = Preferred (25% weight).
- **Unique Constraint:** `uk_opp_skill` on (`opportunity_id`, `skill_id`)

### 2.8 `Application`
- **Purpose:** Candidate's private application lifecycle record for an opportunity.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `user_id` (BIGINT, Not Null, FK -> User.id)
  - `opportunity_id` (BIGINT, Not Null, FK -> Opportunity.id)
  - `status` (ENUM: SAVED, APPLIED, SCREENING, INTERVIEWING, OFFER, ACCEPTED, REJECTED, WITHDRAWN, Not Null, Default SAVED)
  - `resume_document_id` (BIGINT, Nullable, FK -> Document.id)
  - `cover_letter_document_id` (BIGINT, Nullable, FK -> Document.id)
  - `personal_notes` (TEXT, Nullable)
  - `user_rating` (INT, Nullable, Range 1 to 5)
  - `saved_at` (TIMESTAMP, Not Null) — First timestamp when opportunity was saved/tracked.
  - `applied_at` (TIMESTAMP, Nullable) — First timestamp when application was submitted.
  - `screening_at` (TIMESTAMP, Nullable) — First timestamp when screening was scheduled.
  - `interviewing_at` (TIMESTAMP, Nullable) — First timestamp when interview stage was entered.
  - `closed_at` (TIMESTAMP, Nullable) — Timestamp when application reached terminal outcome (ACCEPTED, REJECTED, WITHDRAWN).
  - `updated_at` (TIMESTAMP, Not Null) — Most recent update timestamp.
- **Unique Constraint:** `uk_app_user_opp` on (`user_id`, `opportunity_id`) — Prevents duplicate applications to the same active opportunity.
- **Index:** `idx_app_user_status` on (`user_id`, `status`)

#### Timestamp Semantics & Transition Rules:
- **First Entry Semantics:** `saved_at`, `applied_at`, `screening_at`, `interviewing_at`, and `closed_at` record the **first time** an application enters that specific lifecycle phase. This ensures funnel conversion calculations remain mathematically accurate even if an application transitions backward (e.g. `INTERVIEWING` $\rightarrow$ `APPLIED` due to user correction).
- **Terminal Transitions:** Moving to `ACCEPTED`, `REJECTED`, or `WITHDRAWN` populates `closed_at = NOW()`.
- **Reopening:** If an application moves from `REJECTED` back to an active state, `closed_at` is reset to `NULL` while preserving prior stage timestamps.

### 2.9 `Interview`
- **Purpose:** Multi-round interview logging attached to an application.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `application_id` (BIGINT, Not Null, FK -> Application.id)
  - `round_number` (INT, Not Null)
  - `round_title` (VARCHAR(150), Not Null, e.g. "Technical OA", "System Design")
  - `round_type` (ENUM: RECRUITER_SCREEN, TECHNICAL_OA, LIVE_CODING, SYSTEM_DESIGN, BEHAVIORAL, EXECUTIVE_FINAL, Not Null)
  - `scheduled_at` (TIMESTAMP, Not Null, stored in UTC)
  - `duration_minutes` (INT, Default 60)
  - `interviewer_names` (VARCHAR(255), Nullable)
  - `interviewer_emails` (VARCHAR(255), Nullable)
  - `prep_notes` (TEXT, Nullable)
  - `reflection_notes` (TEXT, Nullable)
  - `outcome` (ENUM: PENDING, PASSED, FAILED, RESCHEDULED, Default PENDING)
  - `created_at` (TIMESTAMP, Not Null)
  - `updated_at` (TIMESTAMP, Not Null)

### 2.10 `Document`
- **Purpose:** Candidate resume versions, cover letters, and portfolio references.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `user_id` (BIGINT, Not Null, FK -> User.id)
  - `title` (VARCHAR(200), Not Null, e.g. "Resume_Backend_Java_v2.pdf")
  - `document_type` (ENUM: RESUME, COVER_LETTER, PORTFOLIO, TRANSCRIPT, Not Null)
  - `storage_path` (VARCHAR(1000), Not Null — URL or relative file reference)
  - `version_tag` (VARCHAR(50), Nullable, e.g. "v2.1")
  - `uploaded_at` (TIMESTAMP, Not Null)

### 2.11 `Reminder`
- **Purpose:** User-specific follow-ups, OA deadlines, and interview preparation tasks.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `user_id` (BIGINT, Not Null, FK -> User.id)
  - `application_id` (BIGINT, Nullable, FK -> Application.id)
  - `title` (VARCHAR(255), Not Null)
  - `due_at` (TIMESTAMP, Not Null, stored in UTC)
  - `completed` (BOOLEAN, Not Null, Default False)
  - `reminder_type` (ENUM: OA_DEADLINE, INTERVIEW_PREP, FOLLOW_UP, CUSTOM, Not Null, Default CUSTOM)
  - `created_at` (TIMESTAMP, Not Null)
  - `updated_at` (TIMESTAMP, Not Null)
- **Index:** `idx_reminder_user_due` on (`user_id`, `completed`, `due_at`)

### 2.12 `RefreshToken`
- **Purpose:** Secure session persistence and token rotation tracking.
- **Attributes:**
  - `id` (BIGINT, PK, Auto-increment)
  - `user_id` (BIGINT, Not Null, FK -> User.id)
  - `token_hash` (VARCHAR(255), Not Null, Unique — SHA256 hash of refresh token string)
  - `expires_at` (TIMESTAMP, Not Null)
  - `revoked` (BOOLEAN, Not Null, Default False)
  - `replaced_by_token_hash` (VARCHAR(255), Nullable — for reuse detection)
  - `created_at` (TIMESTAMP, Not Null)
  - `revoked_at` (TIMESTAMP, Nullable)
- **Unique Constraint:** `uk_refresh_token_hash` on (`token_hash`)

---

## 3. Relational Cardinality & Cascade Rules

| Parent Entity | Relationship | Child Entity | Foreign Key | Cascade / Delete Behavior |
| :--- | :---: | :--- | :--- | :--- |
| `User` | `1 : 1` | `UserProfile` | `UserProfile.id` | **CASCADE DELETE** |
| `User` | `1 : N` | `UserSkill` | `UserSkill.user_id` | **CASCADE DELETE** |
| `User` | `1 : N` | `Application` | `Application.user_id` | **CASCADE DELETE** |
| `User` | `1 : N` | `Document` | `Document.user_id` | **CASCADE DELETE** |
| `User` | `1 : N` | `Reminder` | `Reminder.user_id` | **CASCADE DELETE** |
| `User` | `1 : N` | `RefreshToken` | `RefreshToken.user_id` | **CASCADE DELETE** |
| `User` | `1 : N` | `Opportunity` | `Opportunity.created_by_user_id` | **SET NULL ON DELETE** (Preserves custom opps if user leaves, or cascade if private) |
| `Company` | `1 : N` | `Opportunity` | `Opportunity.company_id` | **RESTRICT DELETE** (Cannot delete company with active opportunities) |
| `Opportunity` | `1 : N` | `OpportunitySkill` | `OpportunitySkill.opportunity_id` | **CASCADE DELETE** |
| `Opportunity` | `1 : N` | `Application` | `Application.opportunity_id` | **RESTRICT DELETE** (Cannot delete opportunity with existing candidate applications) |
| `Application` | `1 : N` | `Interview` | `Interview.application_id` | **CASCADE DELETE** |
| `Application` | `1 : N` | `Reminder` | `Reminder.application_id` | **SET NULL ON DELETE** (Deleting application preserves reminder or cascade) |
| `Document` | `1 : N` | `Application` | `Application.resume_document_id` | **SET NULL ON DELETE** |
