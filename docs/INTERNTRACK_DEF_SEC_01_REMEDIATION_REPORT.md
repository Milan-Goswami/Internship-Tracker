# InternTrack — DEF-SEC-01 Security Remediation & Final Regression Gate Report

> **Document Status:** Authoritative Security Remediation & Verification Record  
> **Date:** October 12, 2026  
> **Workspace Root:** `D:\InternTrack`  
> **Target Protected Database:** `interntrack_db` (0 operations, strictly untouched)  
> **Target Isolated QA Database:** `interntrack_live_reconcile`  
> **Final Verdict:** **PASS — DEF-SEC-01 fixed and all mandatory security/regression gates passed.**

---

## 1. Executive Summary

This report documents the root-cause analysis, production fix, automated regression testing, and live verification for defect **DEF-SEC-01** (Refresh Token Family Revocation Rollback Under Declarative `@Transactional`).

Prior to remediation, an audit probe observed that presenting an already-revoked refresh token caused Spring Declarative Transaction Management to roll back the family revocation write because `InvalidTokenException` was an unchecked `RuntimeException`. Consequently, concurrent active sessions for the victim user remained usable in persistent storage.

Following a narrowly scoped production fix in [RefreshTokenService.java](file:///D:/InternTrack/backend/src/main/java/com/interntrack/api/service/RefreshTokenService.java), new integration tests in [RefreshTokenFamilyRevocationIntegrationTest.java](file:///D:/InternTrack/backend/src/test/java/com/interntrack/api/service/RefreshTokenFamilyRevocationIntegrationTest.java), and a hardened live QA harness in [scratch/run_live_api_qa.js](file:///D:/InternTrack/scratch/run_live_api_qa.js):

- **Targeted Refresh Token Tests:** **14 / 14 PASSED** (100%)
- **Backend Maven Test Suite (H2):** **508 / 508 PASSED** (100% — 0 failures, 0 errors, 0 skipped, 2m 54s)
- **Frontend Vitest Suite:** **68 / 68 PASSED** (100% — 34.5s)
- **Frontend Production Build:** **EXIT CODE 0** (Vite Rollup bundle generated in 9.97s)
- **Playwright Chromium E2E Suite:** **13 / 13 PASSED** (100% — 47.7s)
- **Live REST API Core Assertions:** **41 / 41 PASSED** (100%)
- **Mandatory Live Security Gates:** **3 / 3 PASSED** (100%)
- **Standalone Fat JAR Smoke Test:** **5 / 5 Routes Verified** (100%)
- **Protected Database Safety:** `interntrack_db` had **0 queries, 0 connections, 0 migrations, 0 modifications**.

---

## 2. DEF-SEC-01 Root Cause Analysis

### 2.1 The Vulnerability Scenario
The intended security contract for refresh token rotation specifies:
1. When a candidate logs in across multiple devices (e.g. Session 1 on Desktop, Session 2 on Mobile), distinct refresh tokens (RT1, RT2) are issued.
2. In normal rotation, Session 1 presents RT1 and receives a new access token and RT1-new. RT1 is marked `revoked: true`.
3. If an adversary attempts to replay the already-revoked RT1, the platform detects token reuse (a replay breach indicator).
4. The security policy mandates that all active refresh tokens for the victim user must be immediately invalidated (`revokeAllUserTokens(userId)`), and the replay attempt must be rejected with HTTP 401 Unauthorized.

### 2.2 Why the Breach Revocation Rolled Back
In the original implementation of `RefreshTokenService.java`:
```java
@Service
@Transactional // <-- Class-level declarative transaction
public class RefreshTokenService {
    ...
    public RefreshToken rotateRefreshToken(String rawToken) {
        ...
        if (token.isRevoked()) {
            revokeAllUserTokens(token.getUser().getId()); // Line 80
            throw new InvalidTokenException("Refresh token reuse detected"); // Line 81
        }
        ...
    }
}
```
1. `InvalidTokenException` is defined as:
   ```java
   public class InvalidTokenException extends RuntimeException { ... }
   ```
2. In Spring Framework, declarative transactions (`@Transactional`) automatically **roll back** on any unchecked exception (`RuntimeException` or `Error`) unless explicitly instructed otherwise.
3. When line 80 executed `revokeAllUserTokens`, it marked active tokens as revoked in JPA memory (`saveAll(activeTokens)`).
4. But when line 81 immediately threw `InvalidTokenException`, the Spring transactional interceptor intercepted the exception on method exit and triggered a transaction **ROLLBACK**.
5. The `UPDATE refresh_tokens SET revoked = 1` statements were discarded before reaching MySQL commit.
6. As a result, Session 2's refresh token (RT2) and the newly rotated RT1-new remained active (`revoked = 0`) in the database. When Session 2 subsequently called `/api/v1/auth/refresh`, the server accepted the token and returned HTTP 200 OK.

### 2.3 Spring AOP Proxy Self-Invocation Trap
An alternative candidate fix considered using `@Transactional(propagation = Propagation.REQUIRES_NEW)` on `revokeAllUserTokens`. However:
- In Spring AOP, `@Transactional` boundaries are created by dynamic CGLIB proxies wrapping bean instances.
- Calling `this.revokeAllUserTokens(...)` from within `rotateRefreshToken(...)` is an internal self-invocation. Internal method calls bypass the Spring proxy and therefore **do not activate a new transaction boundary** without injecting a self-proxy.
- Relying on `REQUIRES_NEW` on an internal method without proxy interception would have silently failed to isolate the transaction.

---

## 3. The Minimal Robust Production Fix

The correct and narrowest production fix was applied directly to `RefreshTokenService.java`:

### 3.1 Code Modifications
```diff
--- a/backend/src/main/java/com/interntrack/api/service/RefreshTokenService.java
+++ b/backend/src/main/java/com/interntrack/api/service/RefreshTokenService.java
@@ -71,6 +71,7 @@ public class RefreshTokenService {
         return refreshTokenRepository.save(token);
     }
 
+    @Transactional(noRollbackFor = InvalidTokenException.class)
     public RefreshToken rotateRefreshToken(String rawToken) {
         String tokenHash = hashToken(rawToken);
         RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
@@ -107,7 +108,7 @@ public class RefreshTokenService {
         for (RefreshToken token : activeTokens) {
             token.setRevoked(true);
         }
-        refreshTokenRepository.saveAll(activeTokens);
+        refreshTokenRepository.saveAllAndFlush(activeTokens);
     }
```

### 3.2 Rationale
1. **`@Transactional(noRollbackFor = InvalidTokenException.class)`:**
   - Overrides class-level default rollback rules specifically for `rotateRefreshToken`.
   - Intercepts the call at the Spring AOP proxy boundary when invoked from `AuthController`.
   - When `InvalidTokenException` is thrown, Spring's transaction manager commits all pending database mutations (`COMMIT`) before propagating the exception up the stack.
   - Any other unexpected system failure (e.g. database disconnect, `DataAccessException`, `NullPointerException`) still triggers an immediate transaction rollback, preserving ACID integrity.
2. **`saveAllAndFlush(activeTokens)`:**
   - Immediately flushes SQL update statements to the database session, ensuring the dirty state is synchronized before transaction completion.
3. **Preserved HTTP Contract:**
   - `InvalidTokenException` propagates untouched to `GlobalExceptionHandler`, which formats and returns the exact intended HTTP 401 Unauthorized response to the client.

---

## 4. Automated Regression Suites & Evidence

### 4.1 Backend Integration Regression Test
A dedicated integration test suite was created in [RefreshTokenFamilyRevocationIntegrationTest.java](file:///D:/InternTrack/backend/src/test/java/com/interntrack/api/service/RefreshTokenFamilyRevocationIntegrationTest.java) running against isolated in-memory H2:

| Scenario | Objective | Observed Result | Status |
| :--- | :--- | :--- | :--- |
| **Scenario A** | Normal token rotation (RT1 -> RT1-new) revokes old token and issues fresh one. | RT1 marked revoked, new token valid. | **PASS** |
| **Scenario B** | Replaying revoked RT1 revokes concurrent session RT2 and RT1-new in persistent DB state; unrelated User B unaffected. | RT2 and RT1-new persistently marked `revoked: true`. Unrelated User B remains active (`revoked: false`). | **PASS** |
| **Scenario C** | Re-authentication after family revocation permits issuing fresh, usable tokens. | User A re-logs in, fresh RT3 rotates successfully. | **PASS** |
| **Scenario D** | Attempting rotation with unknown/malformed token throws 401 without affecting existing active tokens. | Throws `InvalidTokenException`, active tokens untouched. | **PASS** |

### 4.2 Full Verification Suite Results

```
====================================================================================================
SUITE                               COMMAND                       EXIT   COUNT   PASS  FAIL  TIME
====================================================================================================
1. Targeted Service/Integration     mvn test -Dtest=RefreshToken* 0      14      14    0     1m 05s
2. Full Backend Maven Suite         .\mvnw.cmd test               0      508     508   0     2m 54s
3. Frontend Vitest Suite            npm.cmd test                  0      68      68    0     34.54s
4. Frontend Production Build        npm.cmd run build             0      N/A     OK    0     9.97s
5. Standalone Fat JAR Packaging     .\mvnw.cmd package            0      N/A     OK    0     11.81s
6. Playwright Chromium E2E          npx.cmd playwright test       0      13      13    0     47.70s
7. Live REST API Functional Tests   node scratch/run_live_api_qa  0      41      41    0     7.20s
8. Live Mandatory Security Gates    node scratch/run_live_api_qa  0      3       3     0     (included)
9. Standalone Fat JAR Smoke Test    HTTP GET verification         0      5       5     0     <1.0s
====================================================================================================
```

### 4.3 Mandatory Live Security Gate Detail

Executed against the live standalone Fat JAR on port 8080 connected to isolated MySQL database `interntrack_live_reconcile`:

```
================================================================
Core Functional Assertions: 41 PASSED / 0 FAILED out of 41 total.
Mandatory Security Gates:   3 PASSED / 0 FAILED out of 3 total.
 - [PASS] SEC-GATE-01: Multi-Session Family Revocation: Session 2 Token Invalidation | HTTP 401, Session 2 token is revoked in database and rejected with 401
 - [PASS] SEC-GATE-02: Token Family Revocation: Rotated Session 1 Token Invalidation | HTTP 401, Rotated token is revoked in database and rejected with 401
 - [PASS] SEC-GATE-03: Unrelated User Isolation: User B Token Unaffected by User A Revocation | HTTP 200, Unrelated User B refreshed successfully
================================================================
SUCCESS: All core functional assertions and mandatory security gates PASSED cleanly.
```

### 4.4 MySQL Persisted State Evidence

Direct query to `interntrack_live_reconcile` immediately following the live test run:
```sql
SELECT id, user_id, revoked, expires_at FROM refresh_tokens ORDER BY id DESC LIMIT 5;
```

**Output:**
```
id    user_id    revoked    expires_at
209   18         0          2026-10-19 10:18:50  <-- User B: Token remained ACTIVE (revoked=0)
208   18         1          2026-10-19 10:18:50  <-- User B: Initial token (rotated normally)
207   17         0          2026-10-19 10:18:48  <-- User A: Fresh token after intentional re-login
206   17         1          2026-10-19 10:18:47  <-- User A: Session 1 rotated token (PERSISTENTLY REVOKED)
205   17         1          2026-10-19 10:18:47  <-- User A: Session 2 token (PERSISTENTLY REVOKED)
```
This confirms that upon RT1 reuse detection, both Token 205 (Session 2) and Token 206 (Session 1 rotated) were persistently committed to MySQL with `revoked = 1`, while unrelated User B (Token 209) remained completely active.

---

## 5. Database Safety Verification

The production database is:
```
interntrack_db
```
Safety guarantees enforced throughout the audit and remediation:
1. **0 queries, 0 connections, 0 migrations, 0 writes, and 0 drops** executed against `interntrack_db`.
2. Unit and integration tests ran strictly against in-memory H2 (`jdbc:h2:mem:testdb;MODE=MySQL`).
3. Live regression verification ran strictly against verified isolated database `interntrack_live_reconcile`.
4. Pre-flight verification in `scratch/run_live_api_qa.js` verified the isolated database target and aborted immediately if pointing to `interntrack_db` or an unknown target.
5. All database operations passed credentials via child process environment variable `MYSQL_PWD` rather than command-line arguments, preventing credential exposure in logs or process tables.

---

## 6. Changed Files Audit

The following files were modified or created to remediate DEF-SEC-01 and verify the regression gates:

| File Path | Nature of Change | Purpose |
| :--- | :--- | :--- |
| `backend/src/main/java/com/interntrack/api/service/RefreshTokenService.java` | Modified (Production Code) | Added `@Transactional(noRollbackFor = InvalidTokenException.class)` and `saveAllAndFlush`. |
| `backend/src/test/java/com/interntrack/api/service/RefreshTokenServiceTest.java` | Modified (Unit Test) | Updated Mockito verification from `saveAll` to `saveAllAndFlush`. |
| `backend/src/test/java/com/interntrack/api/service/RefreshTokenFamilyRevocationIntegrationTest.java` | Created (Integration Test) | Added 4 comprehensive family revocation regression scenarios. |
| `scratch/run_live_api_qa.js` | Created / Updated (Harness) | Added mandatory security gates (`SEC-GATE-01/02/03`), DB safety check, entity cleanup. |
| `scratch/run_server.ps1` | Created (Harness) | Server launch script configuring isolated QA DB and environment credentials safely. |
| `README.md` | Updated (Documentation) | Corrected backend test count to 508; qualified local performance benchmarks. |
| `IMPLEMENTATION_ROADMAP.md` | Updated (Documentation) | Added Phase 15 tracking DEF-SEC-01 remediation and regression gate completion. |
| `SYSTEM_ARCHITECTURE.md` | Updated (Documentation) | Added Section 7 documenting refresh token rotation and transactional family revocation. |
| `docs/INTERNTRACK_QA_TEST_SUMMARY.md` | Updated (Documentation) | Added Section 5 summarizing DEF-SEC-01 remediation. |
| `docs/INTERNTRACK_DEF_SEC_01_REMEDIATION_REPORT.md` | Created (Documentation) | This authoritative remediation and verification deliverable. |

---

## 7. Git Publishing Gate & Staged Diff Analysis

### 7.1 Git Baseline Status
```powershell
git status --short
```
- Current branch: `master`
- No files were committed, pushed, reset, stashed, cleaned, or discarded during this task.
- Zero unstaged files were prematurely staged (`git add .` strictly avoided).

### 7.2 Staged vs Working-Tree Differences in `backend/pom.xml`
Re-inspection of `backend/pom.xml`:

1. **`git diff --cached -- backend/pom.xml` (Staged in Index):**
   - Upgraded Spring Boot parent to `4.1.1` and Java to `21`.
   - Added core security foundations: `spring-boot-starter-security`, `spring-security-crypto`, and `jjwt` (0.12.6).
   - Added initial OWASP dependency check plugin configuration.
2. **`git diff -- backend/pom.xml` (Unstaged in Working Tree):**
   - Added `bucket4j-core` (8.10.1) for IP and account rate limiting.
   - Added `h2` database dependency scoped to `test` for isolated fast in-memory execution.
   - Added `maven-resources-plugin` execution (`copy-frontend-dist`) copying `frontend/dist` into `BOOT-INF/classes/static` during packaging.
   - Added `maven-surefire-plugin` configuration directing surefire to run against `jdbc:h2:mem:testdb`.
   - Added `jacoco-maven-plugin` configuration for test code coverage.

**Explanation:** The staged index reflects the earlier security foundation setup, while the unstaged working tree contains the subsequent packaging, rate-limiting, and testing isolation enhancements. Both sets of changes are valid and required. When ready for final GitHub publishing, the complete working tree should be reviewed and deliberately staged together.

---

## 8. Remaining Architectural Notes & Production Considerations

1. **In-Memory Rate Limiting:** Bucket4j buckets are held in JVM memory (`ConcurrentHashMap`), which is optimal and zero-overhead for single-node deployments. If horizontally scaling across multiple container instances, a shared Redis cache or reverse-proxy rate limiter should be used.
2. **Cookie Security Header:** In local development, `COOKIE_SECURE=false` permits refresh token exchange over HTTP. In production behind HTTPS reverse proxies, `COOKIE_SECURE=true` should be set in environment variables.

---

## 9. Final Verdict

### **PASS**
**DEF-SEC-01 is completely remediated, robustly verified against database state, and all mandatory automated security gates have passed with zero exit codes.**

- Replaying a revoked refresh token reliably invalidates all concurrent sessions for that user.
- The revocation is persistently committed in MySQL before HTTP 401 Unauthorized is returned.
- Unrelated users' sessions are completely unaffected.
- Normal rotation, login, and application workflows continue to operate with 100% test pass rates.

### Single Safest Next Action for Project Owner:
Review the working-tree differences using `git status --short` and `git diff`, confirm the documentation updates, and perform a deliberate staging (`git add <files>`) when preparing your first public commit or portfolio demo push.
