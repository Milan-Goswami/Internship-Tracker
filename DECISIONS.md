# InternTrack — Architectural & Product Decision Log

**Document Status:** Reconciled Architecture Decision Records (ADRs)  
**Version:** 1.2.0  

---

## Architectural Decision Records (ADRs)

---

### ADR-01: Global vs. User-Owned Opportunities (Decoupled Model)
- **Decision:** Model `Opportunity` with a nullable `created_by_user_id` foreign key. An opportunity with `created_by_user_id = NULL` represents a global/system posting; an opportunity with `created_by_user_id = User.id` represents a user-created custom opportunity.
- **Reason:** Real-world internship opportunities exist independently of any individual applicant. Multiple candidates applying to the same role must reference the same canonical opportunity record rather than duplicating job descriptions and skill tags for every user.
- **Alternatives Considered:**
  - *Strict user-owned opportunity:* Every user creates and owns their own copy of an opportunity.
  - *Separate tables (`GlobalOpportunity` and `CustomOpportunity`):* Splitting opportunities into two distinct entity hierarchies.
- **Why Rejected:**
  - Strict user ownership causes massive data duplication and prevents multi-candidate skill demand analytics.
  - Separate tables complicate the relational mapping in `Application` (requiring polymorphic associations or two separate foreign keys).
- **Consequences:** Queries for public opportunities filter by `created_by_user_id IS NULL OR created_by_user_id = :currentUserId`.

---

### ADR-02: Strict Application Ownership & Lifecycle State
- **Decision:** Maintain `Application` as a strictly user-owned entity (`user_id` NOT NULL) referencing an `Opportunity`. The application tracks personal stage transitions (`SAVED`, `APPLIED`, `SCREENING`, `INTERVIEWING`, `OFFER`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`) along with first-entry stage timestamps (`saved_at`, `applied_at`, `screening_at`, `interviewing_at`, `closed_at`).
- **Reason:** Candidates need private notes, interview rounds, custom resume version links, and ratings that must never be visible to other applicants. First-entry timestamps provide accurate conversion funnel analytics even if a user moves an application backward.
- **Alternatives Considered:**
  - *Merging Opportunity and Application into one table.*
  - *Separate `SavedOpportunity` table outside `Application`.*
- **Why Rejected:**
  - Merging violates 3NF and prevents multiple candidates from tracking the same job.
  - A separate `SavedOpportunity` table adds unnecessary complexity when `SAVED` is simply the initial stage of the application lifecycle pipeline.
- **Consequences:** Moving an application stage populates the respective first-entry timestamp if currently null, providing clean audit data.

---

### ADR-03: Application $\rightarrow$ Opportunity Invariant
- **Decision:** Every `Application` MUST reference an `Opportunity` (`Application.opportunity_id BIGINT NOT NULL`). If a candidate discovers an unlisted job, the workflow requires creating a custom `Opportunity` first, then creating the `Application` record.
- **Reason:** Enforces relational integrity across the platform, ensures consistent skill gap calculations, and prevents orphaned application records lacking company and requirement metadata.
- **Alternatives Considered:**
  - *Allowing `Application.opportunity_id` to be nullable for direct applications.*
- **Why Rejected:**
  - Nullable opportunities would require duplicating company, job title, and requirement fields on the `Application` entity, breaking database normalization (2NF/3NF).
- **Consequences:** The frontend "+ New Application" modal will automatically handle the two-step creation (create custom Opportunity $\rightarrow$ create Application) seamlessly in a single user action.

---

### ADR-04: Real External Data Policy vs. Synthetic Test Fixtures
- **Decision:** In production, InternTrack strictly prohibits fabricated/fake job postings or synthetic company profiles; all production data must originate from legitimate public APIs, verified RSS feeds, or direct user entry. In automated testing (unit, slice, and integration tests), synthetic test fixtures are fully permitted and expected.
- **Reason:** InternTrack is designed as a serious, usable intelligence platform where false data destroys user trust. However, unit and integration tests must remain isolated, deterministic, and fast by using synthetic mock datasets.
- **Alternatives Considered:**
  - *Seeding dummy mock data in production.*
  - *Disallowing synthetic test data in automated tests.*
- **Why Rejected:**
  - Dummy production data renders skill analytics and deadline tracking useless.
  - Calling live external APIs during CI/CD test runs introduces flakiness and rate-limit failures.
- **Consequences:** Production database seeds contain only verified canonical skills; integration tests use dedicated H2/Testcontainers test fixtures.

---

### ADR-05: Provider Abstraction & Data Ingestion Architecture
- **Decision:** Decouple external data providers from the domain model using a `ProviderAdapter` interface, separating `source_provider` and `external_source_id` in the database, backed by a SHA-256 deduplication hash (`raw_hash`).
- **Reason:** External job providers have disparate schema formats and rate limits. The core database must remain provider-agnostic so new sources can be added without altering existing schema.
- **Alternatives Considered:**
  - *Hardcoding specific provider fields (e.g. `adzuna_id`, `remoteok_id`) directly on `Opportunity`.*
- **Why Rejected:**
  - Hardcoding violates open-closed principles and forces database migrations whenever a provider is added or retired.
- **Consequences:** Adding a new provider requires only writing an adapter class implementing field mapping and deduplication hashing.

---

### ADR-06: Deterministic Skill Matching Formula (Cases A–D)
- **Decision:** Implement a 100% deterministic mathematical set-matching engine ($75\%$ required skills weight, $25\%$ preferred skills weight) covering four explicit cases (A: both $>0$, B: required only, C: preferred only, D: zero skills $\rightarrow$ null score with `"No Skills Specified"`), accompanied by canonical alias normalization (`C++` $\rightarrow$ `cpp`, `Node.js` $\rightarrow$ `nodejs`).
- **Reason:** Eliminates LLM API latency, costs, and hallucination risks. Provides candidates with an explainable, predictable metric that can be verified mathematically and unit tested with 100% assertion coverage.
- **Alternatives Considered:**
  - *LLM prompt-based semantic skill scoring for MVP.*
  - *Unweighted matching formula ($|User \cap All| / |All|$).*
- **Why Rejected:**
  - LLMs add external network failure points, financial costs per request, and unpredictable score variance.
  - Unweighted matching fails to prioritize mandatory job requirements over optional nice-to-haves.
- **Consequences:** Complex prose requirements must be tagged with canonical skill keywords for matching to execute.

---

### ADR-07: Refresh Token Storage, Rotation & Reuse Detection
- **Decision:** Store refresh tokens in the database strictly as **SHA-256 hashes**, transmit them via `HttpOnly; Secure; SameSite=Strict` cookies, and enforce **Token Rotation** on every refresh request. If an already-revoked refresh token is presented, trigger automated **Reuse Detection** to immediately invalidate all active sessions for that user.
- **Reason:** Plaintext token storage exposes sessions to database leaks. Token rotation limits token lifespan, and reuse detection immediately neutralizes stolen token replay attacks.
- **Alternatives Considered:**
  - *Stateless JWT refresh tokens without database persistence.*
  - *Storing plaintext refresh token UUIDs in MySQL.*
- **Why Rejected:**
  - Purely stateless refresh tokens cannot be revoked on logout or security breach.
  - Plaintext database storage violates basic cryptographic storage standards.
- **Consequences:** Requires a `refresh_tokens` table and database lookup on token renewal requests.

---

### ADR-08: Document Storage Strategy for MVP (Metadata + External URL)
- **Decision:** For the MVP release, implement **Option A: Document Metadata + External URL** (storing title, document type, version tag, and external URL to Google Drive, Dropbox, OneDrive, or personal portfolio). Binary multipart file uploads to S3-compatible object storage (Option C) are designated for Post-MVP.
- **Reason:** Allows candidates to organize and link specific resume versions and portfolio links immediately without introducing complex cloud credentials, multi-part memory buffers, or local filesystem cleanup risks during core pipeline development.
- **Alternatives Considered:**
  - *Option B: Local server binary file storage.*
  - *Option C: Direct AWS S3 / Cloud Object Storage upload in Phase 1.*
- **Why Rejected:**
  - Option B introduces security risks (malicious file execution, disk exhaustion) and fails in containerized/stateless deployments.
  - Option C adds external cloud infrastructure costs and setup friction before core tracking workflows are validated.
- **Consequences:** The `Document` entity's `storage_path` column stores URL strings in MVP and can seamlessly transition to S3 object keys in Post-MVP without altering entity relationships.

---

### ADR-09: Spring Boot 3.3.5 & Java 21 LTS Production Baseline
- **Decision:** Standardize the backend technology stack on **Spring Boot 3.3.5** and **Java 21 LTS**, using Spring MVC, Spring Data JPA / Hibernate 6.5+, Spring Security 6.3+, and MySQL 8.0+.
- **Reason:** Spring Boot 3.3.5 provides the standard, battle-tested production foundation for Java 21 LTS (supporting Virtual Threads, Pattern Matching, and Records) with complete Jakarta EE 10 compatibility and mature integration across JJWT and MySQL Connector/J.
- **Alternatives Considered:**
  - *Spring Boot 3.2.x:* Older minor release line.
  - *Spring Boot 2.7.x:* Legacy line, end-of-life.
- **Why Rejected:**
  - Spring Boot 3.3.5 offers the best balance of ecosystem stability, security patch support, and Java 21 performance.
- **Consequences:** Uses `jakarta.*` packages and Spring Security 6 lambda configuration DSL.

---

### ADR-10: MVP Scope Boundary & Workflow Priority
- **Decision:** Prioritize the core candidate workflow: **Discover $\rightarrow$ View $\rightarrow$ Save $\rightarrow$ Apply $\rightarrow$ Track $\rightarrow$ Interview $\rightarrow$ Outcome**. Classify Batch Updates and Reminders as *Should Have*, while deferring Browser Extensions, Multipart PDF S3 uploads, and Email sync to *Post-MVP*.
- **Reason:** Ensures rapid delivery of a rock-solid, fully testable core application without getting bogged down in complex secondary integrations.
- **Alternatives Considered:**
  - *Building browser scraping extension and AI resume tailoring in Phase 1.*
- **Why Rejected:**
  - Attempting to build secondary extensions before the core pipeline API is verified leads to fragile software architecture.
- **Consequences:** Focuses Phase 1–9 development strictly on the core pipeline, skill matching, and interview hub.
