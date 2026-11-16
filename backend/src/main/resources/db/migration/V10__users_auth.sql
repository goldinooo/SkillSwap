-- V10: users, OAuth links, refresh tokens, API keys.
-- Owner: Risattou (version range V10-V19).
--
-- Follows the team DBML design, with the deltas recorded in
-- docs/V10__users_auth.md section 2. Two of them matter enough to repeat here:
--
--   * TIMESTAMPTZ instead of timestamp. "DEFAULT now()" on a plain timestamp
--     column silently stores the SERVER's local wall-clock time, not UTC, which
--     contradicts the design's own "store in UTC" note the moment anyone runs the
--     container with TZ set. See the doc for the reproduction.
--   * users.role and the api_keys table are not in the DBML. They back two
--     claimed modules (Advanced permissions, 2 pts; Public API, 2 pts). If the
--     team cut those modules on purpose, delete the marked blocks below.
--
-- This file runs before every other migration: every other table has a foreign
-- key into users(id).

-- ============================================================================
-- users
-- ============================================================================
CREATE TABLE users (
    -- gen_random_uuid() is built into PostgreSQL 13+, so no pgcrypto extension.
    -- Keep the default even though the entity will generate the id in Java: it
    -- keeps hand-written INSERTs (seed data, admin fixes) from failing.
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    first_name          VARCHAR(50),
    last_name           VARCHAR(50),

    -- Stored lowercase only, which makes UNIQUE effectively case-insensitive:
    -- "Risattou" cannot take a second account once "risattou" exists. The name
    -- users actually see is display_name, so nothing is lost by normalising.
    username            VARCHAR(50)  NOT NULL UNIQUE CHECK (username = lower(username)),
    email               VARCHAR(255) NOT NULL UNIQUE CHECK (email = lower(email)),

    -- NOT NULL per the design. Email signup has no display name to copy, so
    -- UserService must default it to the username.
    display_name        VARCHAR(255) NOT NULL,

    -- NULL for OAuth-only accounts, which never pick a password.
    -- 255 fits BCrypt (60) and leaves room for Argon2 (~100).
    password_hash       VARCHAR(255),

    bio                 TEXT,
    avatar_url          VARCHAR(255),

    -- IANA zone name, e.g. "Africa/Casablanca". Used to render class times in
    -- the viewer's own zone; the stored instants stay UTC.
    timezone            VARCHAR(50),

    email_verified      BOOLEAN      NOT NULL DEFAULT FALSE,

    two_factor_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Ciphertext, never the raw TOTP secret. AES-GCM output plus the IV, base64
    -- encoded, is far longer than the 20-byte secret it protects.
    two_factor_secret   VARCHAR(255),

    -- NOT IN THE DBML. Backs the Advanced permissions module (2 bonus pts) and
    -- the USER/ADMIN split that TASKS.md puts in common/security. Drop this line
    -- only if that module was cut.open 
    role                VARCHAR(20)  NOT NULL DEFAULT 'USER'
                                     CHECK (role IN ('USER', 'ADMIN')),

    last_seen           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 2FA cannot be on without a secret to verify codes against. Without this, a
    -- bug that clears two_factor_secret locks the account out permanently: login
    -- demands a code and there is nothing left to check it against.
    CONSTRAINT chk_users_2fa_secret_present
        CHECK (two_factor_enabled = FALSE OR two_factor_secret IS NOT NULL)
);

-- ============================================================================
-- oauth: links a users row to a Google or 42 identity.
-- ============================================================================
CREATE TABLE oauth (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- The DBML leaves provider unconstrained. A typo ('google' vs 'GOOGLE')
    -- would create a second, invisible account for the same person, so the
    -- allowed values are pinned here. FORTYTWO, not 42: a Java enum constant
    -- cannot start with a digit, and the two must match exactly.
    provider         VARCHAR(32)  NOT NULL CHECK (provider IN ('GOOGLE', 'FORTYTWO')),

    -- The provider's own id ("sub" for Google, "id" for 42). Never the email:
    -- emails change, provider ids do not.
    provider_user_id VARCHAR(128) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- One identity maps to one account. This is the lookup OAuth login performs.
    CONSTRAINT uq_oauth_provider_identity UNIQUE (provider, provider_user_id),

    -- NOT IN THE DBML. A user links each provider at most once; without it one
    -- user can hold three GOOGLE rows and "unlink my Google account" has no
    -- single row to remove. It also indexes user_id, which is why the design's
    -- separate (user_id) index is not repeated below.
    CONSTRAINT uq_oauth_user_provider     UNIQUE (user_id, provider)
);

-- ============================================================================
-- refresh_tokens: long-lived credential that mints new short-lived access JWTs.
-- ============================================================================
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- SHA-256 of the raw token, hex encoded. Hashed for the same reason as a
    -- password: a database leak must not hand out usable sessions.
    token_hash  VARCHAR(255) NOT NULL UNIQUE,

    expires_at  TIMESTAMPTZ  NOT NULL,

    -- NOT IN THE DBML. Without it the only way a refresh token dies is expiry,
    -- so "log out" and "log out everywhere" cannot actually invalidate anything,
    -- and a stolen token stays valid for its full lifetime. Rotation also needs
    -- it: issuing a replacement must retire the token it replaces.
    revoked_at  TIMESTAMPTZ,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Supports "revoke every token for this user" on logout-everywhere and on
-- password change.
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- ============================================================================
-- api_keys: Public API credentials.
-- NOT IN THE DBML. Backs the Public API module (2 pts): "API key create and
-- revoke (store only the hash)" in TASKS.md. Drop this block only if cut.
-- ============================================================================
CREATE TABLE api_keys (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(60)  NOT NULL,

    -- SHA-256 of the raw key, hex encoded: always exactly 64 characters.
    -- VARCHAR, not CHAR(64): CHAR pads with spaces, which breaks the equality
    -- comparison JPA sends. The raw key is returned once at creation and never
    -- stored, so a database leak cannot be replayed against the API.
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Soft revoke, so an audit can still see the key existed. The API-key filter
    -- must therefore test "revoked_at IS NULL", not row absence.
    revoked_at  TIMESTAMPTZ,

    CONSTRAINT uq_api_keys_user_name UNIQUE (user_id, name)
);
