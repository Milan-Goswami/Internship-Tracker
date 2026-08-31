# InternTrack — System Architecture & Modular Monolith Design

**Document Status:** Reconciled Architecture Specification  
**Version:** 1.2.0  

---

## 1. High-Level Modular Monolith Architecture

InternTrack is architected as a **Modular Monolith** in Java 21 / Spring Boot 3.3.5 paired with a high-density React Single Page Application (SPA).

```
+-----------------------------------------------------------------------------------+
|                              REACT FRONTEND SPA                                   |
|   [React Router v6] -> [Context State] -> [Axios Interceptor] -> [Vanilla CSS]    |
+------------------------------------------+----------------------------------------+
                                           | HTTP REST API (JSON)
                                           v
+-----------------------------------------------------------------------------------+
|                       SPRING BOOT BACKEND MONOLITH (v3.3.5)                       |
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
| **Framework** | Spring Boot | **3.3.5** | Standard production baseline for Java 21; native Jakarta EE 10, Spring Security 6.3, and Spring Data JPA support. |
| **Persistence** | Spring Data JPA / Hibernate | **6.5.x** | Enterprise ORM, parameterized JPQL queries, connection pooling via HikariCP. |
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

1. **Dashboard KPI Load:** Target $< 250\text{ms}$ execution for 300 applications.
2. **Applications Table Pagination:** Target $< 150\text{ms}$ response for 50 records with sorting.
3. **Skill Gap Set Intersection:** Target $< 10\text{ms}$ execution in-memory.
