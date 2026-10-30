# Backend task split

> Backend only. Derived from `project_context.md` (sections 4, 5, 7, 9, 10).
> Ownership here replaces the backend rows of the table in section 4 of `project_context.md`.
> Shared files (`pom.xml`, `application.properties`, `SecurityConfig`) change only through a small PR.

## Ownership

| Person | Packages | Flyway range | Why |
|---|---|---|---|
| **Retahri** | `credit/`, `session/`, `matching/`, `skill/`, `gamification/` | V20–V29 | Concurrency-sensitive credit logic, session state machine, search and matching queries |
| **Risattou** | `common/` (security, exception), `auth/`, `user/`, `publicapi/` | V10–V19 | Security stack: password hashing, JWT, OAuth2, TOTP, API keys, rate limiting |
| **Nisrine** | `realtime/`, `chat/`, `friend/`, `notification/` | V30–V39 | WebSocket/STOMP, plus the CRUD-heavier social features |

Risattou and Nisrine carry roughly equal load. Retahri has fewer packages but the hardest ones.

## Retahri (hard)

1. **Migrations V20 and V21**: skills, teach/learn lists, matches, sessions, ratings, credit ledger, badges. Include the unique index on `(session_id, reason)`.
2. **`credit/`**: atomic spend with `UPDATE users SET credits = credits - 1 WHERE id = ? AND credits >= 1`. Write the ledger row and the balance change in one transaction. Signup bonus once per account. Add a concurrency test with many parallel requests.
3. **`session/`**: state machine REQUESTED → CONFIRMED → COMPLETED / CANCELLED. Check balance on request, transfer on learner confirmation, refund on cancel. Ratings: 1–5 stars, one per rater per session.
4. **`matching/`**: suggestions from "my teach list vs their learn list". Advanced search with filters, sorting and pagination (a claimed module).
5. **`skill/`**: skill catalog and teach/learn list endpoints. Small, but matching depends on it, so do it first.
6. **`gamification/`**: XP, badges, streaks, paginated leaderboard. Credits are reported through this module, not a separate one.

## Risattou (security)

1. **Migration V10**: `users`, `oauth_accounts`, `api_keys`. **Do this first**, since other tables reference `users`.
2. **`common/`**: `SecurityConfig`, JWT filter, one global exception handler with a single error response shape, USER/ADMIN roles. Retahri and Nisrine depend on this, so land it early.
3. **`user/`**: register, profile (display name, bio, avatar), salted password hashing, input validation.
4. **`auth/`**: login, JWT issue and validation, Google and 42 OAuth2 linked through `oauth_accounts`.
5. **`auth/` 2FA**: TOTP enrollment and verification on top of `java-otp`. Store `totp_secret` encrypted.
6. **`publicapi/`**: API key create and revoke (store only the hash), API-key filter, Bucket4j rate limiting (includes the cache config that `application.properties` currently disables), 5+ endpoints, springdoc docs.

## Nisrine (realtime and social)

1. **Migration V30**: `friendships`, `conversations`, `messages`, `notifications`.
2. **`realtime/config`**: STOMP endpoint over wss, message broker.
3. **`realtime/security`**: authenticate the WebSocket CONNECT with the JWT from Risattou's `auth/`.
4. **`chat/`** and **`realtime/handler`**: one conversation per pair (ordered ids), persist messages (1–2000 chars), send and receive over STOMP, history pagination, read receipts.
5. **`friend/`**: request, accept, decline, block, list friends, online status over the socket.
6. **`notification/`**: keep it minimal (the subject cuts "complete notifications"). Store events, list and mark read, push over the socket. Session and friend events come from the other two.
7. **Tests**: WebSocket integration tests against Postgres.

## Dependencies

Ids: `R` = Retahri, `RS` = Risattou, `N` = Nisrine, `D` = devops (from `project_context.md`).

### Shared prerequisites (before any feature PR)

| Task | Owner | Why it blocks others |
|---|---|---|
| Remove the `spring.autoconfigure.exclude` block and set datasource from env (doc section 10, steps 1–3) | D (or RS as first PR) | No JPA, Flyway or repository can run until this lands |
| Postgres in Docker Compose with health check (doc step 1) | D | Every integration test and every migration needs it |
| Fix `groupId` mismatch (doc step 9) | D | Shared `pom.xml`, one small PR, do it before the others branch off |

### Per-task dependencies

| Task | Blocked by | Provides to |
|---|---|---|
| **RS** V10 migration (`users`, `oauth_accounts`, `api_keys`) | Shared prerequisites | Every FK in V20, V21, V30; every user lookup |
| **RS** `common/` security filter and `CurrentUser` accessor | RS V10 (for the user lookup) | R (all endpoints), N (WebSocket principal) |
| **RS** `common/` exception handler and error shape | none | R, N (all endpoints) |
| **RS** `user/` entity and repository | RS V10 | R `skill/`, R `session/`, R `credit/`, N `friend/`, N `chat/` |
| **RS** `auth/` JWT issue and parse (`TokenService`) | RS `user/`, RS `common/` security | N `realtime/security` |
| **RS** `auth/` OAuth2, 2FA | RS `auth/` JWT | none |
| **RS** `publicapi/` API keys and rate limit | RS V10, RS `common/` filter, Bucket4j cache config (shared `application.properties`) | none |
| **R** V20 and V21 migrations | RS V10 | R `skill/`, `matching/`, `session/`, `credit/`, `gamification/` |
| **R** `skill/` | R V20, RS `user/` entity | R `matching/` |
| **R** `credit/` (`spend`, `earn`, `refund`) | R V21, RS `user/` entity, RS `common/` exception | R `session/` |
| **R** `session/` (request, confirm, cancel, rate) | R `credit/`, R `matching/` (match rows), RS `common/` | R `gamification/`; N `notification/` (events) |
| **R** `matching/` search | R `skill/`, R V20 | none |
| **R** `gamification/` (XP, badges, leaderboard) | R `session/` events, R V21 | none |
| **N** V30 migration | RS V10 | N `chat/`, `friend/`, `notification/` |
| **N** `realtime/config` (STOMP endpoint, broker) | none (can start day 1) | N `chat/`, `realtime/handler` |
| **N** `realtime/security` | RS `auth/` JWT parse, N `realtime/config` | N `chat/`, `friend/` (presence) |
| **N** `chat/` | N V30, N `realtime/config`, N `realtime/security`, RS `user/` | none |
| **N** `friend/` | N V30, RS `user/`, RS `common/` | N `notification/` (friend events) |
| **N** `notification/` | N V30, R `session/` events, N `friend/` events | none |

### Critical path

`RS V10` → `RS common/ security + exception` → `RS user/` → `RS auth/ JWT` → `R V20/V21` → `R skill/` → `R credit/` → `R session/` → `R gamification/`

Retahri's chain is the longest. Risattou's V10 and `common/` gate almost everything, so they are the first thing to merge.

### Can start day 1, no blockers

- **N** `realtime/config`, using a stub principal until RS `auth/` lands.
- **N** V30 migration file, merged after RS V10.
- **RS** `common/` exception handler.
- **RS** `publicapi/` controller and docs, using stubbed keys until the table exists.
- **R** `matching/` query design and `skill/` entity drafts, merged after RS `user/`.
- **R** `credit/` concurrency test harness, against the V21 table once it merges.

### Interfaces to agree on before coding

These are proposals. Each owner confirms them in a short PR or call before the consumer starts.

- **`CurrentUser` (RS, `common/security`)**: `Long currentUserId()`. R and N call this instead of reading the security context directly.
- **Token parsing (RS, `auth/`)**: `Optional<AuthenticatedUser> parse(String token)`. N's WebSocket CONNECT handler calls it. N can stub it in tests until RS merges.
- **Error shape (RS, `common/exception`)**: one `ErrorResponse` class and one base exception. Everyone throws the base exception.
- **Credit operations (R, `credit/`)**: `spend(userId, sessionId)`, `earn(userId, sessionId)`, `refund(userId, sessionId)`. Only R's `session/` calls them.
- **Session events (R publishes, N and R consume)**: `SessionRequested`, `SessionConfirmed`, `SessionCancelled`, `SessionCompleted`, `SessionRated`, each carrying `sessionId`, `teacherId`, `learnerId`. N's `notification/` listens. R's `gamification/` listens to `SessionCompleted`.
- **Friend events (N publishes, N consumes)**: `FriendRequested`, `FriendAccepted`. N's `notification/` listens.

Agree on these names before the first consumer is written. Otherwise N's `notification/` and R's `session/` will each guess the other's names and break at merge.

### Ownership rules

- Each person owns only their own folders and their own test folders under `src/test/java/com/trendenden/backend/`.
- Shared files (`pom.xml`, `application.properties`, `SecurityConfig` if it is not under `common/`) change only through a small PR that names the other two as reviewers.
