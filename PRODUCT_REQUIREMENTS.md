# InternTrack — Product Requirements Document (PRD)

**Document Status:** Reconciled Product Specification  
**Version:** 1.2.0  
**Author:** Lead Product Architect & Senior Full-Stack Software Architect  
**Target Platform:** Web (Desktop-first, Fully Responsive)  

---

## 1. Product Vision & Executive Summary

**InternTrack** is a production-grade **Internship Application Intelligence Platform** built specifically for computer science students, fresh graduates, and early-career job seekers.

Unlike generic task managers or fragile spreadsheets, InternTrack provides a structured, high-density workflow tool to manage the complete end-to-end job search lifecycle.

### Core Workflow Philosophy:
```
[ Discover Opportunity ] ──> [ View Specs & Skills ] ──> [ Save to Pipeline (Application) ]
                                                                      │
                                                                      ▼
[ Final Outcome ] <── [ Multi-Round Interviews ] <── [ Submit Application ]
```

### Core Invariant:
**Every Application record MUST reference a canonical Opportunity record.** If a user tracks an unlisted external role, the platform creates a custom Opportunity first, then attaches the user's Application lifecycle record.

---

## 2. Target Users & User Personas

### 2.1 Target Audience
- **Primary:** Undergraduate & Graduate Computer Science / Engineering Students applying to 50–300+ internships per recruitment cycle.
- **Secondary:** Bootcamp graduates and career switchers entering junior software engineering roles.

### 2.2 User Personas

#### Primary Persona: Alex Chen (Undergraduate Computer Science Student)
- **Profile:** Junior at a university applying for Summer Software Engineering Internships.
- **Pain Points:** Loses track of job requirements when recruiters call weeks later ("context collapse"); misses online assessment (OA) deadlines; mixes up resume versions.
- **Goals:** High-density command center to manage applications, track multi-round interviews, and review deterministic skill alignment.

#### Secondary Persona: Priya Sharma (Career Switcher / Bootcamp Graduate)
- **Profile:** Junior developer transitioning into tech.
- **Pain Points:** Unclear skill alignment before applying; spends hours on custom applications for roles requiring unlearned tools.
- **Goals:** Objective skill gap diagnostics highlighting missing candidate skills to prioritize high-probability applications.

---

## 3. Core Problems & Value Proposition

| Core Problem | Spreadsheet / Generic Tool Failure | InternTrack Solution |
| :--- | :--- | :--- |
| **Context Collapse** | Job descriptions expire or are removed online after applying. | Cached opportunity snapshot preserving requirements, salary, and tech stack. |
| **Relational Data Chaos** | Spreadsheets fail to represent 1:N interview rounds and document links per role. | First-class relational domain: `Application` $\rightarrow$ `Interview` (1:N), `Document` (N:1), `Reminder` (1:N). |
| **Opaque Skill Alignment** | Manual guesswork on whether candidate skills match the job description. | Deterministic skill gap analyzer with weighted math and missing skill chips. |
| **Deadlines & Follow-up Decay** | OA and interview deadlines get buried across multiple personal calendars. | Integrated Reminders hub tied directly to applications and dashboard action cards. |

---

## 4. Deterministic Skill Matching Specification

InternTrack employs a **100% deterministic, explainable set-matching engine** (zero AI/LLM hallucinations for MVP).

### 4.1 Mathematical Scoring Formula

Let $U$ be the candidate's canonical skill set, $S_{\text{req}}$ be the set of required opportunity skills, and $S_{\text{pref}}$ be the set of preferred opportunity skills.

$$\text{RequiredScore} = \frac{|U \cap S_{\text{req}}|}{|S_{\text{req}}|}, \quad \text{PreferredScore} = \frac{|U \cap S_{\text{pref}}|}{|S_{\text{pref}}|}$$

#### Evaluation Cases:
- **Case A ($|S_{\text{req}}| > 0$ and $|S_{\text{pref}}| > 0$):**
  $$\text{OverallScore} = \operatorname{round}\left(0.75 \times \text{RequiredScore} + 0.25 \times \text{PreferredScore}\right) \times 100$$
- **Case B ($|S_{\text{req}}| > 0$ and $|S_{\text{pref}}| = 0$):**
  $$\text{OverallScore} = \operatorname{round}\left(\text{RequiredScore}\right) \times 100$$
- **Case C ($|S_{\text{req}}| = 0$ and $|S_{\text{pref}}| > 0$):**
  $$\text{OverallScore} = \operatorname{round}\left(\text{PreferredScore}\right) \times 100$$
- **Case D ($|S_{\text{req}}| = 0$ and $|S_{\text{pref}}| = 0$):**
  $$\text{matchPercentage} = \text{null}, \quad \text{matchLabel} = \text{"No Skills Specified"}$$

*All calculated percentage scores are strictly bounded between $0\%$ and $100\%$.*

### 4.2 Canonical Skill Normalization & Aliases
All input skill strings are normalized via lowercase conversion, whitespace trimming, and alias mapping:
- `C++` / `Cpp` $\rightarrow$ `cpp`
- `Node.js` / `NodeJS` / `Node` $\rightarrow$ `nodejs`
- `React.js` / `ReactJS` / `React` $\rightarrow$ `react`
- `.NET Core` / `ASP.NET` / `.NET` $\rightarrow$ `dotnetcore`
- `Spring Boot` / `SpringBoot` $\rightarrow$ `springboot`
- `PostgreSQL` / `Postgres` $\rightarrow$ `postgresql`

---

## 5. Real Data Integration & Zero Fake Production Data Policy

### Policy Distinction:
1. **Production Environment:** Strictly **Zero Fake Data**. No fabricated job postings or synthesized company records. Data originates solely from legitimate public APIs, verified feeds, or direct user entry.
2. **Automated Testing Environment:** Synthetic mock fixtures and controlled test datasets are expected, encouraged, and required for fast, isolated unit and integration testing.

---

## 6. Document Storage Strategy (MVP vs Post-MVP)

- **MVP Strategy (Option A):** Manage document metadata (title, version tag, document type) paired with external URL references (Google Drive, OneDrive, Dropbox, Portfolio links).
- **Post-MVP Strategy (Option C):** Direct binary upload to S3-compatible cloud object storage with presigned URLs.

---

## 7. Functional Requirements Matrix (MoSCoW Prioritization)

| ID | Module | Feature Description | Priority |
| :--- | :--- | :--- | :---: |
| **FR-01** | Auth | User registration, BCrypt password hashing ($k=12$), and JWT login. | **MUST HAVE** |
| **FR-02** | Auth | Secure refresh token rotation with reuse detection in HTTP-only cookie. | **MUST HAVE** |
| **FR-03** | Opps | Opportunity catalog (Company, title, location, salary, required skills, custom posts). | **MUST HAVE** |
| **FR-04** | Apps | Application lifecycle tracking across stages (Saved $\rightarrow$ Applied $\rightarrow$ ... $\rightarrow$ Offer/Closed). | **MUST HAVE** |
| **FR-05** | Apps | Dual UI Views (1-click toggle between Kanban Pipeline and Dense Multi-Sort Table). | **MUST HAVE** |
| **FR-06** | Skills | Deterministic Skill Gap Engine (Cases A–D, alias normalization, missing skills output). | **MUST HAVE** |
| **FR-07** | Interv | Multi-round interview logging (Round type, scheduled UTC date/time, prep notes, outcome). | **MUST HAVE** |
| **FR-08** | Docs | Document metadata & external resume version URL linker. | **MUST HAVE** |
| **FR-09** | Dash | Command center dashboard (Urgent OA/Interview cards, pipeline funnel metrics). | **MUST HAVE** |
| **FR-10** | Tasks | Deadline and follow-up reminders (`/api/v1/reminders`). | **SHOULD HAVE** |
| **FR-11** | Apps | Batch status updates across multiple applications in Table view. | **SHOULD HAVE** |
| **FR-12** | Ext | Browser extension for 1-click scraping & automated email status sync. | **POST-MVP** |

---

## 8. Target Performance Benchmarks (Empirical Measurement Targets)

*Note: These are target engineering benchmarks to be empirically measured post-implementation, not absolute SLA guarantees:*

- **Dashboard KPI Query:** Target $< 250\text{ms}$ response time for a candidate with 300 applications and 20 interview rounds.
- **Applications Table Page Request:** Target $< 150\text{ms}$ for paginated retrieval of 50 items with multi-column sorting.
- **Skill Match Computation:** Target $< 10\text{ms}$ in-memory execution per opportunity.
- **Frontend Table Render:** Target 60 FPS (zero UI thread freezing) when rendering 50 dense table rows.
