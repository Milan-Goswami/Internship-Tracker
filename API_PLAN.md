# InternTrack — REST API Specification & Endpoint Architecture

**Document Status:** Reconciled API Specification  
**Version:** 1.2.0  

---

## 1. REST Standards & Response Envelopes

### 1.1 Base URL & Versioning
- All endpoints are versioned under `/api/v1/`.

### 1.2 Standardized HTTP Status Code Mapping

| Status Code | Reason / Semantic Meaning | Typical Endpoints |
| :--- | :--- | :--- |
| **`200 OK`** | Request succeeded. Response body contains resource data. | `GET`, `PUT`, `PATCH` endpoints |
| **`201 Created`** | Resource successfully created. Returns `Location: /api/v1/.../{id}` header. | `POST /applications`, `POST /opportunities`, `POST /reminders` |
| **`204 No Content`** | Request succeeded. No response body returned. | `DELETE /applications/{id}`, `DELETE /reminders/{id}` |
| **`400 Bad Request`** | Malformed request body, unparseable JSON, or parameter type mismatch. | Global exception handler |
| **`401 Unauthorized`** | Missing, expired, or invalid JWT access token. | Security filter interceptor |
| **`403 Forbidden`** | Authenticated user lacks permission to access resource (IDOR check failure). | Ownership validation check |
| **`404 Not Found`** | Resource with specified identifier does not exist. | `GET /applications/{id}`, `GET /opportunities/{id}` |
| **`409 Conflict`** | Request conflicts with current state (e.g. duplicate application to same opp). | `POST /applications`, `POST /auth/register` |
| **`422 Unprocessable`**| Syntactically valid JSON failing domain validation rules (e.g. future date). | `@Valid` DTO validation failures |
| **`429 Too Many Requests`** | Rate limit exceeded. | `POST /auth/login`, `POST /auth/register` |
| **`500 Internal Error`**| Unhandled server exception. Response contains sanitized error code. | Global fallback handler |

---

### 1.3 Unified JSON Response Envelopes

#### Success Envelope
```json
{
  "success": true,
  "data": { ... },
  "message": "Application created successfully",
  "timestamp": "2026-08-31T18:00:00Z"
}
```

#### Paginated List Envelope
```json
{
  "success": true,
  "data": [ ... ],
  "pagination": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 48,
    "totalPages": 3,
    "isLast": false
  },
  "timestamp": "2026-08-31T18:00:00Z"
}
```

#### Error Envelope
```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_APPLICATION",
    "message": "An active application already exists for this opportunity",
    "details": [
      {
        "field": "opportunityId",
        "issue": "Opportunity ID 104 is already linked to Application ID 42"
      }
    ]
  },
  "timestamp": "2026-08-31T18:00:00Z"
}
```

---

## 2. Resource Groups & Endpoints Breakdown

---

### Resource Group 1: Authentication (`/api/v1/auth`)

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | `201`, `400`, `409`, `422`, `429` | Public | Register new candidate account |
| `POST` | `/api/v1/auth/login` | `200`, `400`, `401`, `429` | Public | Authenticate user & return JWT + Refresh Cookie |
| `POST` | `/api/v1/auth/refresh` | `200`, `401`, `403` | Cookie + Header | Rotate refresh token & issue new access JWT |
| `POST` | `/api/v1/auth/logout` | `204`, `401` | Bearer JWT | Revoke active refresh token & clear cookies |

---

### Resource Group 2: User Profile & Skills (`/api/v1/users/me` & `/api/v1/skills`)

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | `200`, `401` | Bearer JWT | Fetch current user profile & preferences |
| `PUT` | `/api/v1/users/me` | `200`, `400`, `401`, `422` | Bearer JWT | Update profile & target search criteria |
| `GET` | `/api/v1/skills/search` | `200`, `401` | Bearer JWT | Autocomplete search in canonical skill taxonomy |
| `GET` | `/api/v1/skills/user` | `200`, `401` | Bearer JWT | List candidate's personal skills & proficiencies |
| `POST` | `/api/v1/skills/user` | `201`, `400`, `401`, `409` | Bearer JWT | Add skill to candidate profile |
| `DELETE` | `/api/v1/skills/user/{skillId}` | `204`, `401`, `404` | Bearer JWT | Remove skill from profile |

---

### Resource Group 3: Companies (`/api/v1/companies`)

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/companies/search` | `200`, `401` | Bearer JWT | Search verified company profiles |
| `POST` | `/api/v1/companies` | `201`, `400`, `401`, `409` | Bearer JWT | Create employer profile (if unlisted) |
| `GET` | `/api/v1/companies/{id}` | `200`, `401`, `404` | Bearer JWT | Get company details & associated opportunities |

---

### Resource Group 4: Opportunity Catalog (`/api/v1/opportunities`)
*Core Invariant:* Opportunities represent objective job postings. This endpoint manages browsing the catalog and creating custom posts.

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/opportunities` | `200`, `401` | Bearer JWT | Browse & search accessible opportunity catalog (Global + user-custom, Paginated) |
| `POST` | `/api/v1/opportunities` | `201`, `400`, `401`, `422` | Bearer JWT | Create custom opportunity (`Location: /api/v1/opportunities/{id}`) |
| `GET` | `/api/v1/opportunities/{id}` | `200`, `401`, `404` | Bearer JWT | Get opportunity specifications & required skills |
| `PUT` | `/api/v1/opportunities/{id}` | `200`, `400`, `401`, `403`, `404` | Bearer JWT | Update custom opportunity (Owner check) |
| `DELETE` | `/api/v1/opportunities/{id}` | `204`, `401`, `403`, `404` | Bearer JWT | Delete custom opportunity (Owner check) |
| `GET` | `/api/v1/opportunities/{id}/skill-match` | `200`, `401`, `404` | Bearer JWT | Compute deterministic skill match breakdown |

---

### Resource Group 5: Application Lifecycle (`/api/v1/applications`)
*Core Invariant:* Every Application MUST reference an existing `Opportunity` (`opportunityId` is mandatory). User interactions such as "Saving", "Applying", and "Interviewing" are lifecycle stages of `Application`.

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/applications` | `200`, `401` | Bearer JWT | Retrieve authenticated user's application lifecycle records (Filter by status e.g. SAVED, APPLIED, Sort, Paginated) |
| `POST` | `/api/v1/applications` | `201`, `400`, `401`, `409`, `422` | Bearer JWT | Create application lifecycle record for an opportunity (Requires `opportunityId`; `Location: .../{id}`, `409` if duplicate) |
| `GET` | `/api/v1/applications/{id}` | `200`, `401`, `403`, `404` | Bearer JWT | Get application details, notes, & rounds |
| `PATCH` | `/api/v1/applications/{id}/status` | `200`, `400`, `401`, `403`, `404` | Bearer JWT | Update lifecycle stage (e.g. SAVED $\rightarrow$ APPLIED $\rightarrow$ INTERVIEWING) |
| `PATCH` | `/api/v1/applications/batch-status` | `200`, `400`, `401` | Bearer JWT | Batch update status across multiple applications |
| `PUT` | `/api/v1/applications/{id}` | `200`, `400`, `401`, `403`, `404` | Bearer JWT | Update application notes, rating, document links |
| `DELETE` | `/api/v1/applications/{id}` | `204`, `401`, `403`, `404` | Bearer JWT | Archive / delete application record |

---

### Resource Group 6: Interview Management (`/api/v1/applications/{appId}/interviews`)

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/applications/{appId}/interviews` | `200`, `401`, `403`, `404` | Bearer JWT | List rounds for a specific application |
| `POST` | `/api/v1/applications/{appId}/interviews` | `201`, `400`, `401`, `403`, `422` | Bearer JWT | Schedule interview round (`Location: .../{id}`) |
| `PUT` | `/api/v1/interviews/{interviewId}` | `200`, `400`, `401`, `403`, `404` | Bearer JWT | Update round prep notes, date, or outcome |
| `DELETE` | `/api/v1/interviews/{interviewId}` | `204`, `401`, `403`, `404` | Bearer JWT | Delete interview round |

---

### Resource Group 7: Deadline & Follow-up Reminders (`/api/v1/reminders`)

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/reminders` | `200`, `401` | Bearer JWT | List active user reminders (Filter: `?dueBefore=...&completed=false`) |
| `POST` | `/api/v1/reminders` | `201`, `400`, `401`, `422` | Bearer JWT | Create deadline/follow-up task (`Location: .../{id}`) |
| `PATCH` | `/api/v1/reminders/{id}/complete` | `200`, `401`, `403`, `404` | Bearer JWT | Mark reminder as completed |
| `DELETE` | `/api/v1/reminders/{id}` | `204`, `401`, `403`, `404` | Bearer JWT | Delete reminder |

---

### Resource Group 8: Documents Library (`/api/v1/documents`)
*MVP Strategy:* Manages document metadata and external URLs (e.g. Google Drive / LinkedIn PDF links).

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/documents` | `200`, `401` | Bearer JWT | List candidate's registered document metadata & URLs |
| `POST` | `/api/v1/documents` | `201`, `400`, `401`, `422` | Bearer JWT | Register document metadata & external link (`Location: .../{id}`) |
| `DELETE` | `/api/v1/documents/{id}` | `204`, `401`, `403`, `404` | Bearer JWT | Delete document entry |

---

### Resource Group 9: Analytics & Intelligence (`/api/v1/analytics`)

| Method | Endpoint | Status Codes | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/summary` | `200`, `401` | Bearer JWT | Get core dashboard KPI metrics |
| `GET` | `/api/v1/analytics/funnel` | `200`, `401` | Bearer JWT | Get stage conversion ratios based on stage timestamps |
| `GET` | `/api/v1/analytics/skill-deficits` | `200`, `401` | Bearer JWT | Aggregate top missing skills across tracked opps |
