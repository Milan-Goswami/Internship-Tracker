# InternTrack — Complete User Manual & Beginner Guide

> **Document Status:** Authoritative Product Manual & Beginner Guide  
> **Release:** v1.0.0 (Production Standalone & Local Development)  
> **Target Audience:** Any candidate or student applying to software engineering internships and junior roles. No programming, database, or API knowledge required.  
> **Workspace Root:** `D:\InternTrack`

---

## Table of Contents

1. [Chapter 1: What is InternTrack and Why Does It Exist?](#chapter-1-what-is-interntrack-and-why-does-it-exist)
2. [Chapter 2: Who the Product is Designed For](#chapter-2-who-the-product-is-designed-for)
3. [Chapter 3: The Problem It Solves Compared with Spreadsheets](#chapter-3-the-problem-it-solves-compared-with-spreadsheets)
4. [Chapter 4: What You See When You Open the Website](#chapter-4-what-you-see-when-you-open-the-website)
5. [Chapter 5: How to Register, Log In, Log Out, and Account Recovery](#chapter-5-how-to-register-log-in-log-out-and-account-recovery)
6. [Chapter 6: A Complete Tour of Every Page and Navigation Item](#chapter-6-a-complete-tour-of-every-page-and-navigation-item)
7. [Chapter 7: Command Center Dashboard: Meaning of Each Metric and Card](#chapter-7-command-center-dashboard-meaning-of-each-metric-and-card)
8. [Chapter 8: Applications Workspace: Creating, Editing, Moving, Sorting, and Deleting](#chapter-8-applications-workspace-creating-editing-moving-sorting-and-deleting)
9. [Chapter 9: Kanban Board vs. Dense Table View: When to Use Each](#chapter-9-kanban-board-vs-dense-table-view-when-to-use-each)
10. [Chapter 10: Every Application Status Stage and Its Meaning](#chapter-10-every-application-status-stage-and-its-meaning)
11. [Chapter 11: Companies & Opportunities Catalog: Complete Supported Workflows](#chapter-11-companies--opportunities-catalog-complete-supported-workflows)
12. [Chapter 12: Candidate Skills: Adding Skills and Setting Proficiency](#chapter-12-candidate-skills-adding-skills-and-setting-proficiency)
13. [Chapter 13: Deterministic Skill-Gap Analysis: How Match Scores Work](#chapter-13-deterministic-skill-gap-analysis-how-match-scores-work)
14. [Chapter 14: Interview Preparation Hub: Where to Enter Study Notes](#chapter-14-interview-preparation-hub-where-to-enter-study-notes)
15. [Chapter 15: Post-Interview Reflections: Recording Feedback and Outcomes](#chapter-15-post-interview-reflections-recording-feedback-and-outcomes)
16. [Chapter 16: Reminders & Deadlines: Managing Tasks and Filtering](#chapter-16-reminders--deadlines-managing-tasks-and-filtering)
17. [Chapter 17: Documents Library: Registering Resume and Portfolio Links](#chapter-17-documents-library-registering-resume-and-portfolio-links)
18. [Chapter 18: Profile & Career Preferences: Updating Your Target Settings](#chapter-18-profile--career-preferences-updating-your-target-settings)
19. [Chapter 19: Global Search, Keyboard Shortcuts, and Accessibility](#chapter-19-global-search-keyboard-shortcuts-and-accessibility)
20. [Chapter 20: Empty States, Loading Indicators, Offline Modes, and Errors](#chapter-20-empty-states-loading-indicators-offline-modes-and-errors)
21. [Chapter 21: How to Confirm That Your Changes Are Saved](#chapter-21-how-to-confirm-that-your-changes-are-saved)
22. [Chapter 22: Frequently Asked Beginner Questions (FAQ)](#chapter-22-frequently-asked-beginner-questions-faq)
23. [Chapter 23: Complete Step-by-Step Walkthrough for a First-Time User](#chapter-23-complete-step-by-step-walkthrough-for-a-first-time-user)
24. [Chapter 24: Final Feature Verification Checklist](#chapter-24-final-feature-verification-checklist)

---

## Chapter 1: What is InternTrack and Why Does It Exist?

InternTrack is your personal **Application Intelligence Command Center**. When you are applying for software engineering internships, research fellowships, or graduate roles, you are juggling dozens of job postings, company portals, coding tests, and follow-up deadlines.

InternTrack gives you a private, distraction-free web workspace running directly in your browser. It organizes every application into a clear visual pipeline, evaluates your technical skills against job requirements, tracks interview rounds with preparation and reflection notes, and alerts you to upcoming deadlines.

InternTrack is built as a single, privacy-respecting application: your data belongs entirely to your account, isolated from every other user.

---

## Chapter 2: Who the Product is Designed For

InternTrack was engineered specifically for:
- **Computer Science & MCA Students:** Managing high-volume internship cycles across tech companies, startups, and campus placement drives.
- **Self-Taught Developers & Career Switchers:** Needing objective clarity on which technical skills they have and which missing skills they need to learn for target roles.
- **New Graduates & Junior Engineers:** Preparing for multi-stage technical interviews (online assessments, coding rounds, system design, and hiring manager chats).
- **Candidates Seeking Structure:** Anyone tired of losing track of where they applied, what resume version they submitted, and when an online assessment is due.

---

## Chapter 3: The Problem It Solves Compared with Spreadsheets

| Challenge in Spreadsheets / Notes | How InternTrack Solves It |
| :--- | :--- |
| **Messy, unformatted text rows:** Deadlines and interview links get buried in columns. | **Visual Kanban & Dense Table:** Move applications between stages with drag-and-drop or batch updates. |
| **Guesswork about job fit:** Reading 50 bullet points in a job description to guess if you qualify. | **Instant Skill-Gap Engine:** Compares your skills against the job posting and gives you an exact percentage match with green (matched) and red (missing) tags. |
| **Scattered interview prep notes:** Random Google Docs or scratchpads that you cannot find right before an interview call. | **Integrated Interview Hub:** Every interview round has dedicated "Preparation Notes" and "Post-Interview Reflection" areas linked directly to the job. |
| **Missed Online Assessment (OA) deadlines:** Links expire before you open them. | **Priority Reminders with One-Click Completion:** Overdue and urgent deadlines are highlighted on your dashboard. |
| **Resume version confusion:** Forgetting which resume link you gave to which company. | **Document Registry:** Store verified HTTP or HTTPS links to your specific resume, transcript, and portfolio versions. |

---

## Chapter 4: What You See When You Open the Website

When you open `http://localhost:8080` in your web browser:
1. **If you are not logged in:** You will see the sleek, dark-slate **Sign In** screen. It features a clean login box, email and password fields, and a link to **"Create Account"**.
2. **If you are logged in:** You will land on your **Command Center Dashboard**.
   - **Left Sidebar:** A dark navigation panel listing all 9 main sections of the website.
   - **Top Command Bar:** Contains the Global Search bar (press `/` or `Ctrl+K`), an **Online/Offline** connection badge, your user avatar/email, and a **Log Out** button.
   - **Main Viewport:** Shows 4 key metric cards (Total Applications, Active Pipeline, Interview Rate, Offers Received), an Urgent Reminders list, Upcoming Interviews, and Quick Action buttons.

---

## Chapter 5: How to Register, Log In, Log Out, and Account Recovery

### 5.1 How to Register a New Account
1. On the **Sign In** screen, click **"Create Account"** at the bottom.
2. Enter your details:
   - **Full Name:** e.g., `Alex Rivera`
   - **Email Address:** e.g., `alex.rivera@example.com`
   - **Password:** Minimum 8 characters (e.g., `Password123!`).
   - **Confirm Password:** Type the exact same password.
3. Click the blue **"Create Account"** button.
4. You will immediately be logged in and taken to your personal Dashboard.

> [!NOTE]
> **Registration Rate Limiting:** For security against automated bots, the system allows up to **3 registrations per hour from the same computer/IP address**. If you register more than 3 accounts in an hour, you will see an HTTP 429 message: *"Too many registration attempts. Please try again later."*

### 5.2 How to Log In
1. Enter your registered email address and password.
2. Click **"Sign In"**.
3. Your login stays active for 15 minutes via a secure access token, and refreshes silently in the background for up to 7 days using an `HttpOnly`, `SameSite=Strict` cookie. You will not be logged out while actively using the site.

### 5.3 How to Log Out
1. Look at the top-right corner of the screen.
2. Click the **"Log Out"** button next to your email address.
3. Your session is terminated on the server, the refresh cookie is cleared, and you are redirected to `/login`.

### 5.4 Account Recovery & Password Guidelines
- The password field requires at least 8 characters.
- If you enter an incorrect password, you will see: *"Invalid email or password"*.
- For privacy and local offline operation, password recovery is not tied to an external email provider; store your password securely in your password manager.

---

## Chapter 6: A Complete Tour of Every Page and Navigation Item

The left sidebar gives you instant access to all 9 core functional areas:

1. 📊 **Dashboard (`/dashboard`):** High-level summary of your job search progress, upcoming deadlines, and interview schedule.
2. 📋 **Applications (`/applications`):** Your main workspace. View your applications as a visual **Kanban Board** or switch to the **Table View** for bulk edits.
3. 💼 **Opportunities (`/opportunities`):** Explore available internship positions, view company information, and run skill-gap analyses.
4. 🎙️ **Interviews (`/interviews`):** Track all interview rounds, write preparation study notes, record interviewers, and debrief afterwards.
5. ⏰ **Reminders (`/reminders`):** Manage Online Assessment (OA) deadlines, follow-up emails, and offer decision dates with filter tabs.
6. 🎯 **Skills Matrix (`/skills`):** Catalog your personal technical skills, rate your proficiency, and review skill deficits across the job market.
7. 📄 **Documents (`/documents`):** Register external HTTP or HTTPS links to your resumes, cover letters, and portfolio sites.
8. 📈 **Analytics (`/analytics`):** Track conversion rates, application funnel pipelines, and stage distribution charts.
9. ⚙️ **Settings (`/settings`):** Configure your target job title, preferred work mode (Remote, Hybrid, Onsite), and target salary.

---

## Chapter 7: Command Center Dashboard: Meaning of Each Metric and Card

Your Dashboard provides real-time situational awareness:

### 7.1 Key Metric Cards
- **Total Applications:** The total number of jobs you have tracked in any status.
- **Active Pipeline:** Applications currently in progress (`SAVED`, `APPLIED`, `SCREENING`, or `INTERVIEWING`). Excludes closed applications (`ACCEPTED`, `REJECTED`, `WITHDRAWN`).
- **Interview Rate:** The percentage of applied jobs that reached the interview stage:
  $$\text{Interview Rate} = \frac{\text{Applications with Status } \ge \text{INTERVIEWING}}{\text{Applications with Status } \ge \text{APPLIED}} \times 100\%$$
- **Offers Received:** The total number of job offers currently in your pipeline (`OFFER` or `ACCEPTED`).

### 7.2 Actionable Dashboard Widgets
- **Urgent Reminders:** Displays reminders due within the next 48 hours or currently overdue.
- **Upcoming Interviews:** Lists upcoming interview dates with company names and round titles.
- **Quick Action Buttons:** Direct shortcuts to **"+ New Application"**, **"+ Add Skill"**, and **"+ Schedule Interview"**.

---

## Chapter 8: Applications Workspace: Creating, Editing, Moving, Sorting, and Deleting

### 8.1 How to Create a New Application
1. Navigate to **Applications** in the sidebar (or press the `n` shortcut on your keyboard).
2. Click the blue **"+ New Application"** button in the top right.
3. In the modal dialog:
   - **Select Opportunity:** Choose the job posting you are applying to.
   - **Initial Status:** Defaults to `SAVED` (or select `APPLIED` if you already submitted it).
   - **Personal Notes:** Add any personal notes (e.g., *"Applied with referral from John"*).
   - **Rating:** Rate your enthusiasm for this role (1 to 5 stars).
4. Click **"Save Application"**.

### 8.2 How to Move an Application to Another Stage
- **In Kanban View:** Click the status dropdown directly on any application card and choose the new stage (e.g., change from `SAVED` to `APPLIED`). The card immediately moves to the new column.
- **In Table View:** Click the status dropdown in the row to update it instantly.

### 8.3 How to Edit Application Notes or Rating
1. Click on the card title in Kanban view or the **"Edit"** button in Table view.
2. Update your notes or rating.
3. Click **"Save Changes"**.

### 8.4 How to Perform Batch Updates on Multiple Applications
1. In the **Applications** page, switch to the **Table View** by clicking the **"Table"** view toggle button.
2. Select the checkboxes on the left side of the rows you want to update.
3. A floating **Batch Action Toolbar** will appear at the bottom of the screen.
4. Select the target status (e.g., `REJECTED` or `INTERVIEWING`).
5. Click **"Apply Status"**. All selected applications update simultaneously.

### 8.5 How to Delete an Application
1. In Table view, click the **"Delete"** button (or click **"Delete"** inside the Application Modal).
2. A confirmation dialog will ask: *"Are you sure you want to delete this application?"*
3. Click **"Confirm Delete"**.

---

## Chapter 9: Kanban Board vs. Dense Table View: When to Use Each

InternTrack provides two complementary views for your applications:

```
+---------------------------------------------------------------------------------+
|                                 VIEW TOGGLE                                     |
|           [ Kanban View (Visual) ]         [ Table View (High-Density) ]        |
+---------------------------------------------------------------------------------+
```

- **Use the Kanban Board when:**
  - You want a quick visual overview of where your applications stand across hiring stages.
  - You are reviewing your active pipeline and deciding which company to follow up with next.
  - You want to focus on active columns (`SAVED`, `APPLIED`, `SCREENING`, `INTERVIEWING`, `OFFER`).
  - *Tip:* Click **"Show Closed Columns"** to reveal `ACCEPTED`, `REJECTED`, and `WITHDRAWN`.
- **Use the Table View when:**
  - You have 20+ applications and need to scan through them quickly.
  - You want to sort by company name, application date, or rating.
  - You want to update multiple applications at once using the batch toolbar.
  - You are navigating using your keyboard (`j`/`k` and arrow keys).

---

## Chapter 10: Every Application Status Stage and Its Meaning

InternTrack supports an 8-stage lifecycle model:

1. **`SAVED` (Active):** You bookmarked this opportunity but have not submitted your application yet.
2. **`APPLIED` (Active):** You submitted your resume and application through the company portal.
3. **`SCREENING` (Active):** Recruiter screening, resume review, or initial online assessment received.
4. **`INTERVIEWING` (Active):** You are actively scheduling or completing technical or behavioral interview rounds.
5. **`OFFER` (Active):** You received a formal internship or job offer.
6. **`ACCEPTED` (Terminal / Closed):** You accepted the offer. Congratulations!
7. **`REJECTED` (Terminal / Closed):** The company decided not to move forward.
8. **`WITHDRAWN` (Terminal / Closed):** You chose to withdraw your application.

---

## Chapter 11: Companies & Opportunities Catalog: Complete Supported Workflows

### 11.1 Viewing Opportunities
1. Navigate to **Opportunities** in the sidebar.
2. You will see cards for each available job posting, including the company name, role title, location type (`REMOTE`, `HYBRID`, or `ONSITE`), city, and stipend.

### 11.2 Creating a Custom Opportunity
1. Click **"+ New Opportunity"** in the top right.
2. Fill in:
   - **Company:** Select an existing company or type a new company name.
   - **Job Title:** e.g., *"Backend Software Engineer Intern"*.
   - **Location Type:** Select `REMOTE`, `HYBRID`, or `ONSITE`.
   - **City:** e.g., *"San Francisco, CA"*.
   - **Stipend:** Optional hourly or monthly compensation amount.
   - **Required & Preferred Skills:** Select technical skills expected for the role.
3. Click **"Create Opportunity"**.

### 11.3 Applying Directly from an Opportunity
On any opportunity card, click the **"Apply"** button. This automatically opens the Application Modal with the opportunity pre-selected, ready to save to your tracker.

---

## Chapter 12: Candidate Skills: Adding Skills and Setting Proficiency

1. Navigate to **Skills Matrix** in the sidebar.
2. You will see technical skills grouped into 6 categories:
   - **Languages:** Java, Python, TypeScript, C++, Go, etc.
   - **Frameworks:** Spring Boot, React, Node.js, Express, etc.
   - **Databases:** MySQL, PostgreSQL, MongoDB, Redis, etc.
   - **Tools:** Git, Docker, Maven, Postman, Linux, Jira, etc.
   - **Cloud:** AWS, Azure, Google Cloud Platform, Kubernetes, etc.
   - **Methodologies:** REST APIs, Microservices, CI/CD, Agile, TDD, etc.
3. To add a skill to your profile:
   - Click **"+ Add Skill"**.
   - Select the skill from the master taxonomy of 58 industry skills.
   - Choose your proficiency level: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, or `EXPERT`.
   - Optionally enter years of experience.
   - Click **"Add to Profile"**.
4. To remove a skill, click the small `x` icon on the skill badge.

---

## Chapter 13: Deterministic Skill-Gap Analysis: How Match Scores Work

### 13.1 How to Check Your Skill Match for a Job
1. On the **Opportunities** page, locate any job posting.
2. Click the **"Skill Match"** button on the opportunity card.
3. A **Skill Match Drawer** slides out from the right side of the screen.

### 13.2 Understanding the Skill Match Breakdown
The drawer gives you an exact, objective breakdown:
- **Match Score Percentage:** Calculated using an industry-standard formula:
  $$\text{Score} = \left(0.75 \times \frac{\text{Matched Required Skills}}{\text{Total Required Skills}}\right) + \left(0.25 \times \frac{\text{Matched Preferred Skills}}{\text{Total Preferred Skills}}\right)$$
- **Matched Skills (Green Badges):** Skills you have on your profile that the job requests.
- **Missing Skills (Red Badges):** Skills requested by the company that are not on your profile. These are your exact study topics!
- **Zero-Skill Result:** If an employer did not specify any skills, InternTrack displays: *"No Skills Specified"* with a clear explanatory note rather than a misleading 0%.

---

## Chapter 14: Interview Preparation Hub: Where to Enter Study Notes

InternTrack provides a dedicated preparation space for every interview:

1. Navigate to **Interviews** in the sidebar.
2. To schedule an interview, click **"+ Schedule Interview"**:
   - **Application:** Link it to your target company.
   - **Round Number:** e.g., Round 1, Round 2, Round 3.
   - **Round Title:** e.g., *"Technical Coding Assessment"* or *"System Design Interview"*.
   - **Round Type:** Choose `TECHNICAL_OA`, `BEHAVIORAL`, `SYSTEM_DESIGN`, or `FINAL_ROUND`.
   - **Date & Time:** Set the scheduled interview date and time.
   - **Interviewer Names:** Optional names of your interviewers.
3. Click **"Save Interview"**.
4. On the interview card, click **"Prep Notes & Outcome"**.
5. The **Interview Prep Drawer** opens on the right side.
6. Under **"Preparation Notes"**, type your study topics, algorithms to review, questions to ask the interviewer, and company research notes.
7. Click **"Save Prep Notes"**. Your notes are saved to the database and will be there whenever you return.

---

## Chapter 15: Post-Interview Reflections: Recording Feedback and Outcomes

Immediately after your interview call finishes:
1. Open **Interviews** and click **"Prep Notes & Outcome"** on that round's card.
2. Scroll to the **"Outcome & Reflection"** section at the bottom of the drawer.
3. In the **"Reflection Notes"** box, type how the interview went:
   - What questions were asked?
   - What problems did you solve well?
   - What mistakes or knowledge gaps did you encounter?
4. When the company sends you feedback, select the **Outcome**:
   - `PENDING` (Waiting for results)
   - `PASSED` (Moving forward to next round)
   - `FAILED` (Rejected after this round)
   - `CANCELLED`
5. Click **"Submit Outcome"**.

---

## Chapter 16: Reminders & Deadlines: Managing Tasks and Filtering

### 16.1 Creating a Reminder
1. Navigate to **Reminders** in the sidebar.
2. Click **"+ New Reminder"**.
3. Fill in:
   - **Title:** e.g., *"Complete HackerRank Coding Challenge for Datadog"*.
   - **Reminder Type:** Choose `OA_DEADLINE`, `APPLICATION_DEADLINE`, `FOLLOW_UP`, `OFFER_EXPIRY`, or `CUSTOM`.
   - **Due Date & Time:** When the task must be completed.
   - **Linked Application:** Optional link to a specific application.
4. Click **"Create Reminder"**.

### 16.2 Completing a Reminder
On any reminder card, click the checkbox on the left. The task is optimistically crossed out and marked complete. If you unchecked it by mistake, click it again to reopen it.

### 16.3 Filtering Reminders
Use the filter tabs at the top of the Reminders page:
- **`ALL`:** View all tasks.
- **`UPCOMING`:** Tasks due in the future that are not yet completed.
- **`OVERDUE`:** Past-due tasks requiring immediate attention (highlighted in red).
- **`COMPLETED`:** Archive of tasks you have finished.

---

## Chapter 17: Documents Library: Registering Resume and Portfolio Links

InternTrack acts as a secure external link registry for your application documents:

1. Navigate to **Documents** in the sidebar.
2. Click **"+ Add Document"**.
3. Fill in:
   - **Title:** e.g., *"Alex Rivera — Software Engineer Resume (2026)"*.
   - **Document Type:** Choose `RESUME`, `COVER_LETTER`, `TRANSCRIPT`, or `PORTFOLIO`.
   - **Storage URL:** Paste your external link (e.g., Google Drive link, GitHub portfolio URL, or Dropbox link).
   - **Version Tag:** e.g., `v2.1-backend` or `v1.0-general`.
4. Click **"Save Document"**.

> [!WARNING]
> **Security Protocol Rule:** For your protection, InternTrack strictly requires valid web addresses starting with `https://` (or `http://`). Unsafe browser schemes such as `javascript:` or `file:` are automatically blocked.

---

## Chapter 18: Profile & Career Preferences: Updating Your Target Settings

1. Navigate to **Settings** in the sidebar.
2. You will see two sections:
   - **Account Information:** Displays your full name, email address, user ID, and registration date.
   - **Candidate Preferences Form:** Set your job search targets:
     - **Target Job Title:** e.g., *"Backend Engineer Intern"*.
     - **Target Location:** e.g., *"San Francisco, CA"* or *"Remote"*.
     - **Minimum Salary ($):** e.g., `$45.00/hr`.
     - **Maximum Salary ($):** e.g., `$65.00/hr`.
     - **Preferred Work Mode:** Select `REMOTE`, `HYBRID`, `ONSITE`, or `ANY`.
3. Click **"Save Preferences"**.
4. A green toast notification confirms: *"Preferences updated successfully"*.

---

## Chapter 19: Global Search, Keyboard Shortcuts, and Accessibility

InternTrack is fully accessible and optimized for fast keyboard navigation:

### 19.1 Global Shortcuts
- **`/` or `Ctrl+K`:** Focuses the Global Search bar from anywhere on the website.
- **`n` (when not typing in an input field):** Instantly opens the **New Application** modal.
- **`Escape`:** Dismisses any open modal or side drawer and restores focus to your previous position.

### 19.2 Table Keyboard Navigation (Roving Tabindex)
When in the Applications Table view:
- Press **`j` or `ArrowDown`** to move to the next row.
- Press **`k` or `ArrowUp`** to move to the previous row.
- Press **`Enter`** to activate the focused row.

---

## Chapter 20: Empty States, Loading Indicators, Offline Modes, and Errors

- **Empty States:** When you first register or have no items in a list, InternTrack displays friendly guidance cards with a clear call-to-action button (e.g., *"No applications yet. Click '+ New Application' to get started"*).
- **Loading Skeletons:** While data is loading, smooth skeleton placeholder bars appear rather than jarring blank screens.
- **Offline Banner:** If your computer loses internet or server connection, a bright warning banner appears at the top: *"You are currently offline. Actions requiring server synchronization will be retried when connection is restored."*
- **Error Alerts:** If an operation fails (e.g., duplicate email or missing required field), a descriptive red alert banner explains what needs correction.

---

## Chapter 21: How to Confirm That Your Changes Are Saved

1. **Toast Notifications:** Successful saves display a green pop-up message in the bottom-right corner (e.g., *"Application saved successfully"*).
2. **Instant UI Refresh:** When you update an application status or complete a reminder, the cards and dashboard numbers refresh automatically.
3. **Page Reload Test:** You can press `F5` or `Ctrl+R` in your browser at any time. All your applications, notes, reminders, and skills will be present exactly as you left them, retrieved directly from the MySQL database.

---

## Chapter 22: Frequently Asked Beginner Questions (FAQ)

### Q1: Is my data shared with other people on the website?
**No.** InternTrack enforces complete tenant data isolation. Even if another user registers on the same computer, their account cannot view your applications, interviews, documents, or reminders.

### Q2: Can I use InternTrack completely offline?
The application runs locally on your computer with MySQL. As long as your local Java application and MySQL database are running, you do not need an active internet connection to manage your applications.

### Q3: What happens if I forget which resume version I used?
Go to **Applications**, open your job record, and check the linked document or personal notes where you recorded your resume version tag.

### Q4: Why did my skill match score say "No Skills Specified"?
If an opportunity listing does not list any technical requirements (e.g., a general career-fair posting), InternTrack reports *"No Skills Specified"* so you know the employer has not listed concrete criteria.

### Q5: Can I drag and drop cards between Kanban columns?
Yes! You can either drag cards across columns or use the status dropdown menu on each card to transition between stages.

---

## Chapter 23: Complete Step-by-Step Walkthrough for a First-Time User

Follow this 5-minute walkthrough to experience the entire product:

1. **Step 1 — Register:** Open `http://localhost:8080`, click **"Create Account"**, enter your name, email, and password (`Password123!`), and click **"Create Account"**.
2. **Step 2 — Set Your Skills:** Click **Skills Matrix** in the sidebar. Click **"+ Add Skill"**, select `Java`, set proficiency to `ADVANCED`, and click **"Add to Profile"**. Repeat for `React` and `MySQL`.
3. **Step 3 — Find an Opportunity:** Click **Opportunities** in the sidebar. Locate a job posting and click **"Skill Match"**. Review your green matched skills and red missing skills in the drawer.
4. **Step 4 — Track the Application:** Click **"Apply"** on that opportunity card. Set your rating to 5 stars, add a note, and click **"Save Application"**.
5. **Step 5 — Move It on the Kanban Board:** Go to **Applications**. On your new card in the `SAVED` column, click the status dropdown and change it to `APPLIED`. Notice it moves to the `APPLIED` column.
6. **Step 6 — Schedule an Interview:** Go to **Interviews**, click **"+ Schedule Interview"**, pick your application, select `TECHNICAL_OA`, set tomorrow's date, and save.
7. **Step 7 — Write Prep Notes:** On the interview card, click **"Prep Notes & Outcome"**. Type your study topics under "Preparation Notes" and click **"Save Prep Notes"**.
8. **Step 8 — Set a Reminder:** Go to **Reminders**, click **"+ New Reminder"**, title it *"Review LeetCode dynamic programming"*, and save. Click the checkbox to mark it complete!
9. **Step 9 — Check Your Dashboard:** Click **Dashboard**. Observe that your Total Applications count is 1, Active Pipeline is 1, and your upcoming interview is listed.

---

## Chapter 24: Final Feature Verification Checklist

Use this checklist to confirm you have mastered all features of InternTrack:

- [ ] Registered a personal account and logged in
- [ ] Added at least 3 skills to your profile in the Skills Matrix
- [ ] Checked a Skill Match breakdown on an opportunity card
- [ ] Created an application and moved it through the Kanban pipeline
- [ ] Switched to the Table view and tested table sorting
- [ ] Scheduled an interview round and saved preparation notes
- [ ] Logged post-interview reflection notes and updated the outcome
- [ ] Created a deadline reminder and checked it off as completed
- [ ] Added a link to your online resume in the Documents Library
- [ ] Updated your target job preferences in Settings
- [ ] Pressed `/` or `Ctrl+K` to use the Global Search bar
- [ ] Logged out safely using the Top Command Bar
