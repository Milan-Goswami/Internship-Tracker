# InternTrack — Information Architecture & Screen Specifications

**Document Status:** Reconciled Specification Draft  
**Version:** 1.1.0  

---

## 1. Application Navigation Model

InternTrack uses a **Persistent Left Sidebar + Top Command Bar** layout structure designed for fast navigation and high screen real-estate usage.

```
+-----------------------------------------------------------------------------------+
|  [Logo] InternTrack   [Search... Ctrl+K]               [+ New App]  [User Profile]|
+---------------+-------------------------------------------------------------------+
|               |                                                                   |
|  NAVIGATION   |  MAIN WORKSPACE CONTENT AREA                                      |
|  - Dashboard  |                                                                   |
|  - Applications|  (Renders Active Screen View:                                    |
|  - Opportunities|   Dashboard / Kanban / Table / Skill Matrix / Analytics)          |
|  - Interviews |                                                                   |
|  - Reminders  |                                                                   |
|  - Skill Matrix|                                                                   |
|  - Documents  |                                                                   |
|  - Analytics  |                                                                   |
|               |                                                                   |
+---------------+-------------------------------------------------------------------+
|  [Collapse]   |  STATUS BAR / KEYBOARD SHORTCUT HELPER                            |
+---------------+-------------------------------------------------------------------+
```

### Top Command Bar
- **Global Search Box (`Ctrl+K`):** Instant search across applications, company names, skills, and notes.
- **Quick Action Button (`+ New Application`):** Opens a modal or drawer to quickly log a job opportunity or application.
- **User Profile Menu:** Account settings, security, export data, and logout.

### Left Sidebar (Collapsible)
1. **Dashboard (`/dashboard`):** Daily action hub, upcoming interviews, urgent deadlines, key search metrics.
2. **Applications (`/applications`):** Dual-view pipeline workspace (Kanban Board & Dense Filterable Table with batch actions).
3. **Opportunities (`/opportunities`):** Job catalog & discovery archive (saved posts, company info, skill specs, custom posts).
4. **Interviews (`/interviews`):** Chronological interview calendar, prep hub, and round history.
5. **Reminders (`/reminders`):** Dedicated deadline, OA completion, and follow-up task manager.
6. **Skill Matrix (`/skills`):** Personal skill profile manager & aggregate skill gap analysis.
7. **Documents (`/documents`):** Resume version library, cover letters, and portfolio link manager.
8. **Analytics (`/analytics`):** Funnel reports, response rates, source conversions, and outcome breakdown.
9. **Settings (`/settings`):** Profile preferences, target preferences, security credentials.

---

## 2. Comprehensive Screen Specifications

---

### Screen 1: Dashboard (`/dashboard`)
- **Purpose:** Primary command center answering: "What requires my attention today?", "What interviews are coming up?", and "How is my job search performing?"
- **Primary User Action:** Click on upcoming interview or task to start preparation / complete action.
- **Secondary Actions:** Click "+ New Application" or filter dashboard scope by time window (7 days / 30 days / All Time).
- **Information Displayed:**
  - **Action Hub Banner:** Highlighted urgent items (e.g., "OA due in 24h for Stripe", "Technical Interview tomorrow at 2:00 PM with Datadog").
  - **Metric Quick-Cards (4 max):** Active Applications, Interview Rate (%), Pending OAs, Offers Received.
  - **Upcoming Interviews Panel:** Chronological list of next 3 scheduled rounds with direct prep notes link.
  - **Application Pipeline Mini-Funnel:** Visual bar chart showing conversion across stages.
  - **Active Reminders Widget:** Impending deadlines and follow-up action items.
  - **Recent Activity Feed:** Latest status updates and notes added.

---

### Screen 2: Applications Workspace (`/applications`)
- **Purpose:** Central management center for tracking all job applications across their lifecycle.
- **Primary User Action:** Update application stage (via Kanban drag/click or Table dropdown) and log interview invitations.
- **Secondary Actions:** Toggle view mode (Kanban / Table), filter by stage/company/skill, search text, execute batch status updates.
- **View Modes:**
  1. **Kanban Pipeline Board:** Columns for *Saved*, *Applied*, *Screening*, *Interviewing*, *Offer*, *Accepted*, *Rejected*, *Withdrawn*.
  2. **Dense Data Table:** High-density rows displaying: Company, Role Title, Stage, Location/Mode, Match %, Applied Date, Next Action, Actions menu.
- **Batch Actions:** Multi-row checkbox selection in Table view with bulk actions (e.g. Mark as Rejected, Archive, Delete).
- **Filter Controls:** Stage filter multi-select, Work Mode (Remote/Hybrid/Onsite), Date Range, Skill filter.
- **Sort Controls:** Sort by Applied Date, Company Name, Skill Match %, Next Interview Date.

---

### Screen 3: Opportunities Catalog (`/opportunities`)
- **Purpose:** Database of job postings saved prior to formal application or cataloged for skill gap evaluation.
- **Primary User Action:** "Convert to Application" button (moves opportunity from saved catalog into active `Applied` state).
- **Secondary Actions:** Add custom opportunity, edit requirements, view deterministic skill match breakdown.
- **Information Displayed:** Company name, logo, role title, stipend/salary range, required skills list, match badge, original URL.

---

### Screen 4: Interview Hub (`/interviews`)
- **Purpose:** Preparation center and schedule view for all active interview processes.
- **Primary User Action:** Open interview prep drawer to review technical topics, company background, and prep notes.
- **Secondary Actions:** Schedule new interview round, log post-interview reflection, mark round outcome (Passed / Failed / Pending).
- **Information Displayed:** Timeline view of interview rounds grouped by status (*Upcoming*, *Completed*, *Needs Reflection*). Each card displays Company, Role, Round Type, Date/Time, Interviewer names, and Quick Prep link.

---

### Screen 5: Reminders & Deadlines (`/reminders`)
- **Purpose:** Central task workspace for managing OA deadlines, application follow-ups, and interview prep milestones.
- **Primary User Action:** Mark reminder as completed or create new reminder.
- **Secondary Actions:** Filter by overdue/today/upcoming, associate reminder with specific application.
- **Information Displayed:** Checkbox task list grouped by urgency (Overdue, Due Today, This Week, Later) with reminder type tag and linked company badge.

---

### Screen 6: Skill Matrix (`/skills`)
- **Purpose:** Profile skill inventory management and cross-opportunity skill demand analysis.
- **Primary User Action:** Add/remove user skills and set proficiency levels.
- **Secondary Actions:** View top requested skills across all saved opportunities vs. current user skills.
- **Information Displayed:** Categorized user skill chips (Languages, Frameworks, Databases, Tools), target opportunity skill cloud, and top skill deficit frequency chart.

---

### Screen 7: Documents Library (`/documents`)
- **Purpose:** Track resume versions, tailored cover letters, and portfolio links associated with applications.
- **Primary User Action:** Register new resume version (e.g., `Resume_Backend_Java_v2.pdf`).
- **Secondary Actions:** Link document to applications, view document usage history.

---

## 3. Seven Universal UI States Specification

Every major view and component in InternTrack MUST implement the following 7 states explicitly:

```
                          +-------------------------+
                          |   1. FIRST-USE EMPTY    |
                          +------------+------------+
                                       |
                                       v
+-------------------------+   +-------------------------+   +-------------------------+
|   3. LOADING STATE      |-->|   2. POPULATED STATE    |-->|   7. SUCCESS STATE      |
|  (Skeleton Shimmers)    |   |    (Active Data Grid)   |   |   (Toast Feedback)      |
+-------------------------+   +------------+------------+   +-------------------------+
                                       |
                                       +--------------------+
                                       |                    |
                                       v                    v
                          +-------------------------+  +-------------------------+
                          |   4. ERROR STATE        |  |   5. NO-RESULTS STATE   |
                          | (Actionable Retry CTA)  |  |  (Clear Filters CTA)    |
                          +-------------------------+  +-------------------------+
                                       |
                                       v
                          +-------------------------+
                          |  6. RESTRICTED STATE    |
                          | (Auth Session Expired)  |
                          +-------------------------+
```

### State Definitions & Requirements:

1. **First-Use Empty State:**
   - *Requirement:* NEVER display generic "No data found".
   - *Design:* Displays a welcoming, structured panel explaining what the view is for, why it is currently empty, and a clear primary CTA button.
   - *Example (Applications Table):*  
     "**No Applications Tracked Yet**  
      Start building your application pipeline by saving an internship opportunity or adding a submitted application."  
      `[ + Add Your First Application ]`

2. **Populated State:**
   - *Requirement:* Clean, high-density presentation of data items with active controls, hover highlight states, and interactive action buttons.

3. **Loading State:**
   - *Requirement:* Zero layout shifts. Uses custom skeleton shimmers matching the exact structural layout of table rows or Kanban cards. No spinner overload.

4. **Error State:**
   - *Requirement:* Explains clearly what failed (e.g., network timeout vs. server 500) and provides a direct, single-click retry CTA.
   - *Example:* "**Unable to Load Applications**  
     We encountered a connection issue fetching your application list.  
     `[ Retry Loading ]`"

5. **No-Results State (Search/Filter Empty):**
   - *Requirement:* Distinguishes search filtering from empty data. Shows what search query or filter produced zero matches and provides a 1-click reset button.
   - *Example:* "**No applications match 'Python' in 'Interviewing' stage**  
     Try broadening your search term or clearing active filters."  
     `[ Reset All Filters ]`

6. **Permission-Restricted State:**
   - *Requirement:* Displayed if JWT token expires or access to resource is unauthenticated. Explains session status with a clean redirect to login.

7. **Success State:**
   - *Requirement:* Immediate visual confirmation (optimistic UI update + subtle non-blocking toast banner in bottom-right corner, e.g., "Application moved to Interviewing stage").
