# Skill Swap: project context

> Read this first. It is the single source of truth for the team and for AI tools.
> If something here is wrong or outdated, fix it in a PR.

## 1. What we are building

**Skill Swap** is a web app for ft_transcendence (42) where people exchange knowledge instead of money.

1. User signs up and lists skills they **teach** and skills they **want to learn**.
2. The app **matches** them with a compatible user (my teach list vs their learn list).
3. Partners **chat in real time**, do a session, and **rate** each other.
4. Sessions move **credits** and give **XP, badges, streaks**, and a **leaderboard** rank.

### Credit rules
- New account starts with **5 credits** (given once).
- Learning a lesson costs the learner **1 credit**; the teacher earns **1 credit**.
- Credits are part of Gamification (rewards), not a separate module.

## 2. Stack

| Part | Tech |
|---|---|
| Frontend | Next.js (React) + *()* |
| Backend | Spring Boot 4.2.0-M2 (milestone) on Java 21, Maven |
| Database | PostgreSQL + Spring Data JPA + Flyway migrations |
| Real-time | WebSocket / STOMP |
| Auth | Spring Security, JWT, OAuth2 (Google + 42 intra), TOTP 2FA |
| Docs / limits | springdoc-openapi 3.1.1, Bucket4j (Spring starter 0.14.0) |
| Infra | Docker Compose, HTTPS (wss for sockets), `.env` |

## 3. Modules we claim (20 pts, need 14 + max 5 bonus)

| Module | Pts |
|---|---|
| Frontend framework | 1 |
| Backend framework | 1 |
| ORM | 1 |
| Real-time (WebSocket) | 2 |
| User interaction (chat, profile, friends) | 2 |
| Standard user management | 2 |
| OAuth 2.0 | 1 |
| 2FA | 1 |
| Gamification (XP, badges, leaderboard, credits) | 1 |
| Advanced search (filters, sort, pagination) | 1 |
| Public API (API key, rate limit, docs, 5+ endpoints) | 2 |
| **Base total** | **15** |
| Advanced permissions (roles, admin) | 2 |
| Multiple languages (3) | 1 |
| SSR (Next.js) | 1 |
| Custom design system (10+ components) | 1 |
| **Bonus total** | **5** |

**Stretch (only if ahead after day 40):** file upload, RTL, ELK.
**Cut:** microservices, WCAG 2.1 AA, real-time collaboration, complete notifications, user analytics.

A broken module is worth 0 points. Demo-test every claimed module.

## 4. Team and ownership

| Who | Owns |
|---|---|
| Frontend (1) | Next.js shell, design system, layout, i18n setup, landing + leaderboard pages |
| Backend 1 | `auth`, `user`, `publicapi` (login, OAuth, 2FA, roles, API key) |
| Backend 2 | `skill`, `matching`, `session`, `credit`, `gamification` |
| Backend 3 | `realtime`, `chat`, `friend`, `notification` |
| Devops (1) | Docker Compose, HTTPS, `.env`, CI, seed data, Privacy/ToS, README |

Backend devs also build the frontend pages for their own features on top of the shell.

## 5. Backend layout

Project root: `backend/` (Maven, `groupId` `com.backend`, artifact `backend`).
Main class: `com.trendenden.backend.BackendApplication`. All new packages go under `com.trendenden.backend`, or component scanning will not find them.

Package-by-feature under `backend/src/main/java/com/trendenden/backend/` (mirror in `src/test/java/...`):

```
common/        config, security, exception, util
auth/ user/ skill/ matching/ session/ credit/
gamification/ chat/ friend/ notification/ publicapi/
realtime/      config, handler, security
```

Each feature has `controller/ service/ repository/ entity/ dto/`. Empty leaf folders hold a `.gitkeep` until real code lands. The test tree has one folder per feature.

Stay inside your own feature folder. Shared files (`pom.xml`, `SecurityConfig`, WebSocket config, `application.properties`) change only through a small PR.

## 6. Current backend state

Checked with `./mvnw verify`: **passing** (`BackendApplicationTests.contextLoads`).

**Dependencies in `backend/pom.xml`:**
- `flyway-database-postgresql`: already present, version from the Boot BOM (12.11.0).
- `springdoc-openapi-starter-webmvc-ui` 3.1.1 (`springdoc-openapi.version`). Supports Boot 4.
- `bucket4j-spring-boot-starter` 0.14.0 (`bucket4j-spring-boot-starter.version`). Pulls `bucket4j_jdk17-core` 8.15.0. Currently disabled, see below.
- `com.eatthepath:java-otp` 1.0.0 (`java-otp.version`). TOTP library. Replaces the abandoned `dev.samstevens:totp` (last release 2020). It is a plain OTP generator (`TimeBasedOneTimePasswordGenerator`), so enrollment, QR codes and verification are ours to write.

**Temporary settings in `backend/src/main/resources/application.properties`:**
- `spring.autoconfigure.exclude` turns off DataSource, JDBC, Hibernate/JPA and Flyway auto-configuration. No PostgreSQL is needed to start the app yet.
- `bucket4j.enabled=false`. The Bucket4j starter refuses to start without a cache configured, and rate limiting is not wired yet.

**Known warnings:**
- Maven prints a 401 for `flyway-community-db-support` (GitHub Packages). It is a warning, not a failure. It can be ignored, but it will need a fix if the build ever requires that repository.
- The Bucket4j starter logs `CacheAutoConfiguration not present`. Harmless for now.

## 7. Database schema (draft, not yet in Flyway)

This is a draft derived from the rules in sections 1 and 6, not a finished design. Backend devs confirm or change it before it goes into a migration. It uses PostgreSQL syntax.

Proposed Flyway file split by owner (see section 9 for version ranges):
- **Backend 1** (V10-V19): `V10__users_auth.sql`, containing `users`, `oauth_accounts`, `api_keys`.
- **Backend 2** (V20-V29): `V20__skills_matching.sql` (skills, teach/learn lists, matches, sessions, ratings), `V21__credits_gamification.sql` (ledger, badges), each as its own file.
- **Backend 3** (V30-V39): `V30__friends_chat_notifications.sql` (friendships, conversations, messages, notifications).

```sql
-- V10: auth and users (Backend 1)
CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    username              VARCHAR(30)  NOT NULL UNIQUE,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(100),                 -- NULL for OAuth-only accounts; never store plain text
    display_name          VARCHAR(60),
    bio                   TEXT,
    avatar_url            VARCHAR(500),
    role                  VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    credits               INT          NOT NULL DEFAULT 0 CHECK (credits >= 0),
    signup_bonus_granted  BOOLEAN      NOT NULL DEFAULT FALSE, -- set TRUE in the same transaction that writes the SIGNUP_BONUS ledger row (5 credits); anti-farming
    totp_secret           VARCHAR(100),                 -- encrypt at rest
    totp_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE oauth_accounts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider         VARCHAR(20) NOT NULL CHECK (provider IN ('GOOGLE', 'FORTYTWO')),
    provider_user_id VARCHAR(255) NOT NULL,
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE api_keys (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(60)  NOT NULL,
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,              -- store a hash, never the raw key
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revoked_at  TIMESTAMPTZ
);

-- V20: skills and matching (Backend 2)
CREATE TABLE skills (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(80) NOT NULL UNIQUE,
    category  VARCHAR(40)
);

CREATE TABLE user_teach_skills (
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id  BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE user_learn_skills (
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id  BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE matches (
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    learner_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id    BIGINT      NOT NULL REFERENCES skills(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'SUGGESTED' CHECK (status IN ('SUGGESTED', 'ACCEPTED', 'DECLINED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (teacher_id <> learner_id)
);
CREATE INDEX idx_matches_teacher ON matches (teacher_id);
CREATE INDEX idx_matches_learner ON matches (learner_id);

CREATE TABLE sessions (
    id            BIGSERIAL PRIMARY KEY,
    match_id      BIGINT      NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    teacher_id    BIGINT      NOT NULL REFERENCES users(id),
    learner_id    BIGINT      NOT NULL REFERENCES users(id),
    skill_id      BIGINT      NOT NULL REFERENCES skills(id),
    status        VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
                  CHECK (status IN ('REQUESTED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    scheduled_at  TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (teacher_id <> learner_id)
);
CREATE INDEX idx_sessions_match ON sessions (match_id);

CREATE TABLE ratings (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT      NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    rater_id    BIGINT      NOT NULL REFERENCES users(id),
    ratee_id    BIGINT      NOT NULL REFERENCES users(id),
    stars       SMALLINT    NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment     VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, rater_id)                          -- one rating per person per session
);

-- V21: credits and gamification (Backend 2)
-- Append-only. Balance = SUM(amount) per user, must equal users.credits.
CREATE TABLE credit_ledger (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    amount      INT         NOT NULL CHECK (amount <> 0),
    reason      VARCHAR(30) NOT NULL CHECK (reason IN ('SIGNUP_BONUS', 'LESSON_SPENT', 'LESSON_EARNED', 'REFUND')),
    session_id  BIGINT      REFERENCES sessions(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- A session can pay each side only once: one LESSON_SPENT, one LESSON_EARNED, at most one REFUND.
CREATE UNIQUE INDEX uq_ledger_session_reason ON credit_ledger (session_id, reason) WHERE session_id IS NOT NULL;
CREATE INDEX idx_ledger_user ON credit_ledger (user_id);

CREATE TABLE badges (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(40)  NOT NULL UNIQUE,
    name         VARCHAR(80)  NOT NULL,
    description  VARCHAR(255)
);

CREATE TABLE user_badges (
    user_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id  BIGINT      NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, badge_id)
);

-- V30: friends, chat, notifications (Backend 3)
CREATE TABLE friendships (
    id            BIGSERIAL PRIMARY KEY,
    requester_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (requester_id, addressee_id),
    CHECK (requester_id <> addressee_id)
);

CREATE TABLE conversations (
    id          BIGSERIAL PRIMARY KEY,
    user_a_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id),
    CHECK (user_a_id < user_b_id)                           -- one row per pair, ordered
);

CREATE TABLE messages (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id        BIGINT      NOT NULL REFERENCES users(id),
    content          TEXT        NOT NULL CHECK (length(content) BETWEEN 1 AND 2000),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at          TIMESTAMPTZ
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(40)  NOT NULL,
    payload     JSONB,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications (user_id, read_at);
```

Open questions for the team before this becomes migrations:
- Matching: is a `matches` row created by the system (suggestions) or by a user request? The status values assume suggestions.
- Session flow: the status names above assume request, confirm, complete, cancel. Check against the flow in section 1.
- Whether `users.credits` stays as a cached balance or is computed from the ledger. Section 8 (credit safety) requires them to match, so the cache must be updated in the same transaction as each ledger row.

## 8. Rules the code must follow

**Project rules (from the subject, failing = rejection):**
- One command starts everything (Docker Compose).
- All traffic to the backend over **HTTPS** (sockets over wss).
- Secrets only in `.env` (git-ignored) + a committed `.env.example`.
- Passwords hashed and salted. Inputs validated in **frontend and backend**.
- **Zero** warnings/errors in the browser console. Works in latest Chrome.
- Real **Privacy Policy** and **Terms of Service** pages linked in the footer.
- Many users at once: no race conditions, no data corruption.
- Commits from every member with clear messages.

**Credit safety (our riskiest part):**
- Spend atomically: `UPDATE users SET credits = credits - 1 WHERE id = ? AND credits >= 1`. Zero rows updated = not enough credits.
- Append-only **ledger** table (user, +/-amount, reason, session id). Balance must equal the ledger sum.
- Unique constraint on (session id, transfer type) so a session can never pay twice.
- Check balance on session request, transfer on learner confirmation, refund on cancel.
- Anti-farming: signup bonus once per account, daily cap on repeat sessions between the same two users.

## 9. Team conventions

- `main` is protected. Work on feature branches (`feature/<area>-<what>`) and open PRs, with at least one review.
- Flyway migrations: each backend dev uses their own version range (dev 1: V10-V19, dev 2: V20-V29, dev 3: V30-V39) to avoid clashes.
- Use DTOs in controllers, never expose entities.
- Backend errors use one standard error response from the global exception handler.
- Everything configurable comes from environment variables.

## 10. Next steps once PostgreSQL runs in Docker

Do these in order. Each one is a PR.

1. **Docker Compose** (devops): a `postgres` service with a named volume, health check, and credentials from `.env`. Commit `.env.example`, not `.env`.
2. **Datasource config**: set `spring.datasource.url`, `username` and `password` from environment variables, not literal values.
3. **Remove the temporary exclusions** from `application.properties` (the `spring.autoconfigure.exclude` block). Flyway and JPA then start again.
4. **Migrations**: add the Flyway files from section 7 in the owner's version range. Set `spring.jpa.hibernate.ddl-auto=validate` so Hibernate checks the schema and does not create tables itself.
5. **Bucket4j**: remove `bucket4j.enabled=false`, configure a cache (for example JCache or Caffeine), and wire a rate-limit filter to the Public API (`publicapi`). The starter's startup check will fail without a cache.
6. **Integration test**: `BackendApplicationTests` should run against Postgres. Use Testcontainers or the Compose database. Do not rely on a local install.
7. **HTTPS**: terminate TLS (reverse proxy or Spring SSL) and check that sockets use `wss`.
8. **TOTP**: write the enrollment and verification code on top of `java-otp`. Store `totp_secret` encrypted.
9. **Fix the groupId** (`com.backend` in `pom.xml`) so it matches the package (`com.trendenden`), or the other way round. Do this in one small PR, since it touches shared config.
10. **springdoc**: check that `/swagger-ui` and `/v3/api-docs` load once the app runs.

## 11. Timeline (50 days)

| Days | Goal |
|---|---|
| 1-5 | Spring Boot spike (decision gate: switch to NestJS if it fails), repo, Docker, HTTPS, DB schema |
| 6-20 | Auth, profiles, skills, matching, search, WebSocket, chat, friends |
| 21-35 | Sessions, credits, ratings, gamification, OAuth, 2FA, public API, permissions |
| 38 | **Feature freeze.** Then only i18n, SSR, polish, bug fixing |
| 44-50 | README, demo-test every module, evaluation prep |

## 12. Evaluation reminders

- README first line (italic): `This project has been created as part of the 42 curriculum by <login1>, <login2>, ...`
- README sections: Description, Instructions, Resources (including how AI was used), team roles, project management, tech stack, DB schema, features list, modules with justification, individual contributions.
- Everyone must be able to explain **all** the code and their own contribution. Never paste AI code you cannot explain.
