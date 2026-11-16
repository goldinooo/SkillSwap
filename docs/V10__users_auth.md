# V10 — `users`, `oauth`, `refresh_tokens`, `api_keys`

> Owner: **Risattou** (Flyway range V10–V19)
> File: `backend/src/main/resources/db/migration/V10__users_auth.sql`
> Base: l-DBML dyal l-équipe (2026-11-06) — machi l-draft f `project_context.md` section 7
> Status: applikat w mverifya 3la PostgreSQL 17 ✅

---

## 1. Ash kaykhzen had l-file

| Table | Dour |
|---|---|
| `users` | l-compte: identité, password, 2FA, profil |
| `oauth` | rbat m3a Google / 42 |
| `refresh_tokens` | sessions li kat-tjedded, w li kat-9der t-révoqué |
| `api_keys` | credentials dyal l-Public API |

Kay-jri **l-ewwel**: kol table f V20/V21/V30 fiha FK l-`users(id)`.

---

## 2. 🔴 Ash **naqes** f l-DBML — points f khater

Had l-section hiya l-ehemm f had l-document. L-design jdid w ndif, walakin **naqsa fih
tables li 3lihom points claimed f `project_context.md` section 3.**

### F part dyali (zedthom f V10, m3a comment `NOT IN THE DBML`)

| Naqes | Module | Points | 3lash bla-h = 0 |
|---|---|---|---|
| `users.role` | Advanced permissions (roles, admin) | **2** (bonus) | Ma kayn hta blasa t-khzen "had l-user admin". `TASKS.md` kaygol part dyali: *"USER/ADMIN roles"* |
| `api_keys` table | Public API (API key, rate limit, docs, 5+ endpoints) | **2** | `TASKS.md`: *"API key create and revoke (store only the hash)"*. Bla table, l-module kamel ma yqdar-sh y-tbna |

**Zedthom bjuj**, kol wahd m3a comment `-- NOT IN THE DBML` f l-SQL. Ila l-équipe
9arrerat **b 9asd** t-9te3 had l-juj modules → mse7 dik l-blocks. Walakin dik l-7ala
kat-nqes **4 points** mn l-15 dyal l-base + 5 bonus.

### Machi part dyali — walakin goulhom l-l-équipe

| Naqes | Module | Points | Mall-ha |
|---|---|---|---|
| `credits`, `credit_ledger`, XP, badges, streaks | Gamification (XP, badges, leaderboard, credits) | **1** | Retahri |
| `friendships` | User interaction (chat, profile, **friends**) | **2** | Nisrine |

`README.md` w section 1 dyal `project_context.md` kol-hom kay-dkru l-credits
(*"New account starts with 5 credits"*, *"Learning a lesson costs the learner 1 credit"*).
L-design l-jdid ma 3ndo hta column dyal credits. W `TASKS.md` 3ndo package kamel
`credit/` l-Retahri b test dyal concurrence.

**Nafs l-haja l-`friendships`**: l-module claimed kaygol "chat, profile, friends",
w `TASKS.md` 3ndo package `friend/` l-Nisrine (request, accept, decline, block,
list friends, online status). L-design 3ndo `messages` w `notifications` ghir.

### Juj 7wayj zaydin ymken y-9tle3 3lihom su'al

- **`users.email_verified`** — kayn f l-design, walakin **ma kayn hta table dyal
  verification token**. Ya3ni ma kayn tari9a t-wellih `TRUE`. Ya khassna table
  (`email_verification_tokens`), ya n-mse7u l-column — daba howa column mayt.
  Hadi part dyali (`auth/`), 3lahekka 3aref beha.
- **`messages` bla `conversations`** — l-design 3ndo `sender_id` + `receiver_id`
  direct. Kay-khdem, walakin *"warri liya l-conversations dyali"* kaywelli query
  s3iba (`GROUP BY least(sender,receiver), greatest(sender,receiver)`). L-draft
  l-9dim kan 3ndo table `conversations`. Part dyal Nisrine — ghir goulha-lha.

---

## 3. Ash bdelt mn l-DBML, w l-proof

| Ash | L-DBML | Dert | 3lash |
|---|---|---|---|
| Kol `timestamp` | `timestamp` | **`TIMESTAMPTZ`** | chouf l-proof t7t |
| `username`, `email` | `unique` | + `CHECK (= lower(...))` | `UNIQUE` case-sensitive → comptes mkerrerin |
| `two_factor_*` | bla lien | + `chk_users_2fa_secret_present` | kay-men3 lockout permanent |
| `oauth.provider` | `varchar(32)` bla contrainte | + `CHECK IN ('GOOGLE','FORTYTWO')` | typo `google` = compte tani makhfi l-nafs l-insan |
| `oauth` | `(provider, provider_user_id)` unique | + `UNIQUE (user_id, provider)` | user wa7ed ma yqdar-sh yrbot Google 3 mrrat |
| `oauth` index `(user_id)` | mtlub | **ma zedto-sh** | l-`UNIQUE (user_id, provider)` déjà kay-3tih |
| `refresh_tokens` | `expires_at` ghir | + `revoked_at` | bla-h, "logout" ma kay-7iyyed walo |

### 🔬 L-proof dyal `TIMESTAMPTZ` — jarrebto 3la l-DB

L-DBML kaygol `timestamp`, w f `classes.scheduled_at` kteb note: *"store in UTC"*.
L-mushkil: **`DEFAULT now()` 3la column `timestamp` ma kay-khzen-sh UTC** — kay-khzen
l-wall clock dyal l-**server**.

Jarrebt had-shi 3la l-Postgres dyalna:

```sql
SET TIME ZONE 'Asia/Tokyo';
CREATE TEMP TABLE t (plain timestamp DEFAULT now(), aware timestamptz DEFAULT now());
INSERT INTO t DEFAULT VALUES;
SELECT plain, aware FROM t;
```
```
      plain timestamp       |          timestamptz
----------------------------+-------------------------------
 2026-11-09 00:37:12.363508 | 2026-11-09 00:37:12.363508+09
```

Daba n-rje3 l-session l-UTC w n9ra **nafs l-row**:

```sql
SET TIME ZONE 'UTC';
SELECT plain, aware FROM t;
```
```
  nafs l-row, session UTC   |       timestamptz f UTC
----------------------------+-------------------------------
 2026-11-09 00:37:12.363508 | 2026-11-08 15:37:12.363508+00
```

**Shuf l-far9:**
- `plain` b9a kay9ra `00:37` — **ghalet b 9 swaye3**, w ma kayn **hta haja** f l-value
  katgoul-lek f ashmen timezone t-khzen. L-information t-dya3et l-abad.
- `aware` rje3 `15:37+00` — nafs l-instant, m3rud s7i7.

**3lash hadi khatira f part dyali b l-khusus:** `refresh_tokens.expires_at`.
Ila l-container t-lanca b `TZ=Asia/Tokyo` w l-comparaison t-dart f UTC, l-token
kay-bqa valide **9 swaye3 zyada** 3la l-mudda l-maqsouda. Token msrou9 3ndo 9 swaye3
zaydin. W `last_seen` kay-ghlet f l-presence dyal Nisrine.

**`users.timezone` kayn f l-design** — ya3ni l-équipe 3arfa belli l-users f zones
mkhalfin. `TIMESTAMPTZ` howa l-type li kay-dir dak l-converti wahdo. B `timestamp`,
khassek t-wtheq f **kol** developer belli ghadi y-converti l-UTC b l-yedd, f kol
`INSERT`, l-abad.

> **Tawsiya l-l-équipe:** bedlu `timestamp` → `timestamptz` f **kol** l-design,
> machi ghir f V10. Ila ma dertu-sh, l-tables dyali `timestamptz` w dyalhom
> `timestamp`, w kol JOIN 3la l-waqt kaykhtaj cast.

---

## 4. `users` — column b column

```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

**UUID 3iwad `BIGSERIAL`** (l-draft l-9dim kan `BIGSERIAL`). L-avantages:

| | `BIGSERIAL` | `UUID` |
|---|---|---|
| `/api/users/5` | kay-kchef belli kayn ~5 users | ma kay-kchef walo |
| Enumeration | `/users/1`, `/users/2`... kolshi | mustahil |
| T-génériha f Java qbel l-INSERT | la (khassek round-trip) | ah |
| Taille | 8 bytes | 16 bytes |
| Ordre f l-index | séquentiel (locality mziana) | **random** (page splits) |
| F l-logs | `42` | `f9324a6b-8bda-...` |

**L-khtiyar s7i7 hna** 7it kay-claimi **Public API** — l-ids kay-tban-hom bra,
w `BIGSERIAL` kay-kchef l-7ejm dyal l-platform w kay-khelli l-scraping sahel.

**`gen_random_uuid()`** — built-in f PostgreSQL **13+** (7na 3la 17), 3lahekka
**ma khassek-sh** `CREATE EXTENSION pgcrypto`. Lcode l-9dim online kaygol-lek
tzid pgcrypto — hadak l-PG 12 w ash qbel.

**3lash khellit l-`DEFAULT` hta ila l-entity ghadi y-génére l-id f Java:**
seed data w SQL manuel dyal l-admin kay-khdmu bla ma t-3eyyer l-UUID b l-yedd.

> **Note l-step 4:** f l-entity sta3mel
> `@GeneratedValue(strategy = GenerationType.UUID)` — Hibernate kay-génére l-id
> **qbel** l-INSERT, 3lahekka t3erfo w t9der t-bni l-response wla trbot rows
> akhorin f nafs l-transaction bla round-trip l-DB.
>
> ⚠️ **L-random UUID v4** kay-dkhel f blasa random f l-B-tree → page splits w index
> akbar mn `BIGSERIAL`. F l-7ejm dyal had l-project (alaf dyal rows) **ma kay-bayn-sh**.
> Ila kan 3ndek malayin dyal rows f l-sec, kayn UUIDv7 (time-ordered). Hadi jawab
> mzian ila sewlok "wash UUID machi bati?".

```sql
first_name  VARCHAR(50)
last_name   VARCHAR(50)
display_name VARCHAR(255) NOT NULL
```

⚠️ **`display_name` `NOT NULL`** — jarrebto w kay-fshel b:
```
ERROR: null value in column "display_name" violates not-null constraint
```

Ya3ni f **`UserService.register`**, ila l-user ma 3ta-sh display name (registration
b email ghir kat-telleb username + email + password), **khassek t3emmero nta**:
```java
String displayName = request.displayName() != null ? request.displayName() : request.username();
```
Bla hadi, kol signup kay-fshel b 500. **Had l-7aja khassek t-tfekkerha f step 4.**

F l-OAuth, l-provider kay-3ti l-isem (`name` f Google, `displayname` f 42) → mnin t3emmero.

```sql
username VARCHAR(50) NOT NULL UNIQUE CHECK (username = lower(username))
email    VARCHAR(255) NOT NULL UNIQUE CHECK (email = lower(email))
```

**L-`CHECK` zedto, machi f l-DBML.** `UNIQUE` f PostgreSQL **case-sensitive**:

| Bla l-CHECK | |
|---|---|
| User A kay-registri `risattou@gmail.com` | ✅ |
| User B kay-registri `Risattou@gmail.com` | ✅ **juj comptes l-nafs l-email** |
| User B kay-dir login b `risattou@gmail.com` | 🔴 shkun kay-dekhel? |

Jarrebto:
```
INSERT ... 'Test@X.com'  →  ERROR: violates check constraint "users_email_check"
INSERT ... username 'TEST' →  ERROR: violates check constraint "users_username_check"
```

**Ash khassek tdir f `UserService`:** `.toLowerCase(Locale.ROOT)` qbel save.
⚠️ **`Locale.ROOT` machi `toLowerCase()` bla paramètre** — f locale Turkish,
`"I".toLowerCase()` kay-3ti `"ı"` (bla n9ta) machi `"i"`. Bug 7a9i9i w mashhour.

L-isem li kay-shuf l-user howa `display_name` (ymken ykun `RiSaTtOu`), 3lahekka
ma khserna hta haja.

```sql
password_hash VARCHAR(255)
```

**Nullable** — user li dkhel b Google 3omro ma 3ndo password. Ila kanet `NOT NULL`,
l-OAuth registration kay-fshel.

255 kay-wsse3 l-BCrypt (60) w l-Argon2 (~110). F PostgreSQL `VARCHAR(255)` w
`VARCHAR(60)` **nafs l-stockage** (`varlena`) — ma kayn waste.

🔑 **Fen l-salt?** **Dakhel l-`password_hash`.** BCrypt kay-génére l-salt wahdo w
kay-khezzno f l-output: `$2a$10$<22-char salt><31-char hash>`. 3lahekka
**ma kayn column `salt`** — w hada su'al kayji f l-eval.

```sql
timezone VARCHAR(50)
```
Isem IANA (`Africa/Casablanca`), machi offset (`+01:00`). L-offset kay-tbedel m3a
l-DST; l-isem la. Kay-khdem bach t-werri l-waqt dyal l-classes f l-zone dyal
l-viewer, w l-instants kaybqaw UTC f l-DB.

```sql
email_verified BOOLEAN NOT NULL DEFAULT FALSE
```
Chouf section 2: **ma kayn-sh** table dyal verification token, 3lahekka daba
ma kayn tari9a t-wellih `TRUE`.

```sql
two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE
two_factor_secret  VARCHAR(255)
```

**Hna kay-tkhzen ciphertext, machi l-secret l-khawi.** L-DBML kaygol
*"store encrypted"* — w hadi machi décoration.

Lmaqas: secret TOTP = 20 bytes = 32 char f Base32. B3d AES-GCM:
`IV (12) + ciphertext (20) + tag (16)` = 48 bytes → Base64 = **64 char**.
Zid prefix dyal key-version (l-rotation) → 255 confortable.

🔒 **3lash:** ila l-DB t-dumpat w l-secret plaintext, l-attaquant kay-génére
l-codes dyal **kol** user w l-2FA kamel kaywelli bla ma3na. L-key dyal l-encryption
kayji mn **env var** (`TOTP_ENCRYPTION_KEY`), machi mn l-DB — wella khzenti
l-mefta7 7da l-bab.

```sql
role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN'))
```

**Zedto — machi f l-DBML.** Chouf section 2. Module **Advanced permissions**, 2 bonus pts.

**`VARCHAR` + `CHECK` 3iwad enum natif dyal PostgreSQL:** l-DBML kay-sta3mel
`enum` (`user_skill_role`, `skill_level`, `class_status`) → f PostgreSQL
`CREATE TYPE ... AS ENUM`. Hadchi kay-khdem, walakin:

| | `VARCHAR` + `CHECK` | `CREATE TYPE ... AS ENUM` |
|---|---|---|
| Zid value jdida | `ALTER TABLE ... DROP/ADD CONSTRAINT` | `ALTER TYPE ... ADD VALUE` |
| F transaction? | ✅ | ⚠️ `ADD VALUE` ma yqdar-sh f transaction f ba3d l-versions — ya3ni **l-migration Flyway kat-fshel** |
| 7iyyed value | sahel | **mustahil** bla ma t-saweb type jdid |
| JPA | `@Enumerated(STRING)` | kaykhtaj converter wla `@JdbcTypeCode` |

3lahekka f l-tables dyali `VARCHAR` + `CHECK`. **Hadi 7ta tawsiya l-Retahri**
3la l-3 enums dyalo — `class_status` ghadi y-7taj values jdid (`rescheduled`?)
w b `CREATE TYPE` dak l-migration kat-wel3.

Jarrebto:
```
INSERT ... role 'SUPERADMIN'  →  ERROR: violates check constraint "users_role_check"
```

F l-entity: **`@Enumerated(EnumType.STRING)`**.
⚠️ **3omrek ma tsta3mel `EnumType.ORDINAL`** — kay-khzen 0/1/2, w ila bdelt
l-ordre dyal l-enum f Java, kol l-data f l-DB kat-welli ghalta bla ma hta
7ad y-3ref.

```sql
last_seen   TIMESTAMPTZ
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
```

Chouf section 3 3la l-type. **`last_seen` nullable** — user jdid 3omro ma dkhel.

⚠️ **`updated_at` ma kayn hta haja kay-updatiha automatiquement.** `DEFAULT now()`
kay-khdem ghir f l-`INSERT`. L-DBML kteb note: *"refresh via ORM or trigger"*.
L-khtiyar:

| | Avantage | Inconvénient |
|---|---|---|
| **`@UpdateTimestamp` f l-entity** *(n9ter3)* | bsita, kolshi f Java, sahla t-explikiha | `UPDATE` mn SQL direct kay-khelliha 9dima |
| Trigger f PostgreSQL | kay-9bet kol `UPDATE` hta mn psql | side-effect makhfi |

```sql
CONSTRAINT chk_users_2fa_secret_present
    CHECK (two_factor_enabled = FALSE OR two_factor_secret IS NOT NULL)
```

**Zedto.** Kay-men3 état invalide: 2FA on + secret NULL.

Bla hadi: ila chi bug msa7 l-secret w khella l-flag `TRUE`, l-user
**ma yqdar-sh ydkhel 3omro** — l-login kaytelleb code, w ma kayn secret y-verifi bih.
Account locked out l-abad, w khassek SQL manuel bach t-réparih.

Jarrebto:
```
INSERT ... two_factor_enabled TRUE  →  ERROR: violates "chk_users_2fa_secret_present"
```

---

## 5. `oauth`

```sql
provider VARCHAR(32) NOT NULL CHECK (provider IN ('GOOGLE','FORTYTWO'))
```

**L-`CHECK` zedto.** L-DBML kay-khelli l-`provider` 7orr. Typo (`google` 3iwad
`GOOGLE`) kay-saweb **identité tanya makhfiya l-nafs l-insan** — l-lookup
`findByProviderAndProviderUserId('GOOGLE', ...)` ma kaylqa walo, w l-user kay-tsawb-lih
compte jdid. Jarrebto:
```
INSERT ... 'google'  →  ERROR: violates check constraint "oauth_provider_check"
```

**`FORTYTWO` machi `42`** — enum f Java ma yqdar-sh ybda b raqm (`enum { 42 }` =
erreur de compilation). L-value f l-DB khassha t-matchi l-enum `OAuthProvider` **bzzaf**.

```sql
provider_user_id VARCHAR(128) NOT NULL
```
`sub` 3and Google, `id` 3and 42.

🔑 **3omrek ma tsta3mel l-email b7al identifiant.** L-user kay-bdel l-email f Google;
l-`sub` 3omro ma kaytbedel. Ila rbatti 3la l-email, l-user kay-bdel l-email w
kay-rja3 b7al shakhs jdid → compte mkerrer w kay-tle3 l-data dyalo.

```sql
CONSTRAINT uq_oauth_provider_identity UNIQUE (provider, provider_user_id)
CONSTRAINT uq_oauth_user_provider     UNIQUE (user_id, provider)
```

L-ewwel f l-DBML: identité wehda = compte wa7ed. **Hada howa l-lookup dyal l-login.**

**L-tani zedto ana.** User kay-rbot kol provider **mrra wehda**. Bla-h, user wa7ed
yqder ykun 3ndo 3 rows `GOOGLE` — w "unlink my Google" ma y3ref ash y-7iyyed.
Jarrebto:
```
INSERT GOOGLE mrra tanya  →  ERROR: violates "uq_oauth_user_provider"
```

> 📌 **Su'al mo7tamal f l-eval:** l-DBML kaytelleb index 3la `(user_id)`.
> **Ma zedto-sh.** 3lash: l-`UNIQUE (user_id, provider)` kay-saweb index b
> `user_id` f l-**ewwel column**, w PostgreSQL kaysta3mel index composite
> l-queries 3la l-préfixe dyalo. Index zaid = batta f kol `INSERT`, espace,
> w `VACUUM` zyada — 3la walo.
>
> Ila sewlok *"fen l-index 3la user_id?"* → **3ndi wa7ed, dakhel l-UNIQUE.**

---

## 6. `refresh_tokens` — l-table l-jdida f part dyali

Hadi **ma kanet-sh** f l-draft l-9dim. Kat-bdel l-flow dyal l-`auth/` (step 5), 3lahekka
khassek t-fhem 3lash kayna.

### 3lash kaynin refresh tokens

🔑 **JWT ma yqdar-sh y-trévoqué.** JWT self-contained: l-server kay-verifi
l-signature w safi — **ma kay-lawwej-sh f l-DB**. Ya3ni:

- User kay-dir logout → l-JWT baqi valide
- Password msrouq w l-user bdlo → l-JWT l-9dim baqi valide
- Admin kay-banni user → l-JWT dyalo baqi valide

L-7al l-standard = **juj tokens**:

| | Access token (JWT) | Refresh token |
|---|---|---|
| Fin | f l-header dyal kol request | f l-DB (hashé) |
| Mudda | **9sira** (15 min) | **twila** (30 yum) |
| Verification | signature ghir, bla DB | lookup f l-DB |
| T-révoqué? | la | **ah** |

L-fikra: l-access token 9sir 7it ma t9der-sh t-wa99fo — 15 min dyal darar
f l-a9sa. L-révocation kat-skon f l-refresh token, li **kay-t9ra mn l-DB** f kol
tajdid — w dak l-wa9t t9der tgoul "la".

### L-columns

```sql
token_hash VARCHAR(255) NOT NULL UNIQUE
```
**Hashé, machi l-token l-khawi** — nafs l-mantiq dyal l-password. Ila l-DB t-dumpat,
l-attaquant ma yqdar-sh y-sta3mel l-tokens.

SHA-256 f hex = 64 char. L-DBML kaygol 255 — khellit-ha (kat-wsse3 hash akhor ila bdelt).

🔑 **3lash SHA-256 w machi BCrypt b7al l-password?** L-token `SecureRandom` 32 bytes
= 256 bit d'entropie → **ma kayn dictionary attack 3liha**, 3lahekka ma khassha-sh
key-stretching bati. Password dyal bnadem fih ~30 bit → BCrypt khassha bach t-batti
l-brute force. Zid: l-refresh token kay-t-verifa f kol tajdid; BCrypt (~100ms) kay-9tel
l-performance. **Nafs l-jawab dyal `api_keys.key_hash`.**

```sql
expires_at TIMESTAMPTZ NOT NULL
```
Chouf section 3: b `timestamp` 3adi, token f server b TZ mkhalfa kay-3ish
swaye3 zaydin. Hna l-type ghalet = **faille**, machi ghir bug dyal affichage.

```sql
revoked_at TIMESTAMPTZ
```
**Zedto — machi f l-DBML.** Bla-h, l-wa7ida tari9a token kaymout hiya l-expiration:

- **"Logout"** ma kay-7iyyed walo — l-token baqi khaddem 30 yum
- **"Logout mn kol l-appareils"** mustahil
- Token **msrouq** kaybqa valide l-mudda kamla, w ma 3ndek hta tari9a t-wa99fo
- **Rotation** mustahila: melli t-3ti token jdid, khassek t-retiri l-9dim

⚠️ **Ya3ni f step 5 l-query dyalk khasso ykun:**
```java
findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash, Instant.now())
```
**machi** `findByTokenHash`. Ila nsiti `AndRevokedAtIsNull`, l-tokens l-révoqués
kaybqaw khaddamin — w l-logout kay-welli décoration.

```sql
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
```
Hada **zedto b 9asd** (bkhilaf l-`oauth`): hna **ma kayn** contrainte UNIQUE
3la `user_id`, w khassna l-index l-:
- "révoqui kol l-tokens dyal had l-user" (logout partout, badl l-password)
- cleanup dyal l-tokens expirés

### 💡 Rotation — ash khassek tdir f step 5

L-pattern l-9awi:
1. `POST /api/auth/refresh` b token `T1`
2. L-server kay-verifi `T1`, kay-3ti access jdid **w refresh jdid `T2`**
3. Kay-setti `revoked_at` 3la `T1`
4. **Ila `T1` t-sta3mel 3awtani** → ya3ni t-srek → **révoqui kol l-tokens
   dyal dak l-user**

Had l-"reuse detection" kay-9bet l-srqa dyal token bla ma l-user y-7ess.
`revoked_at` howa li kay-khelli had-shi momkin.

---

## 7. `api_keys`

**Zedt had l-table — machi f l-DBML.** Chouf section 2: module **Public API**, 2 pts.

```sql
key_hash VARCHAR(64) NOT NULL UNIQUE
```

L-flow:
1. `POST /api/keys` → `SecureRandom` 32 bytes
2. Kay-rejje3-ha l-user **mrra wehda**
3. Kay-khzen ghir `SHA-256(raw)` f hex = **64 char bzzaf**
4. Kol request b `X-API-Key` → hashi, lawwej

**3lash `VARCHAR(64)` w machi `CHAR(64)`:** f PostgreSQL `CHAR(n)` kay-remplir
b espaces l-ghaya `n`. Melli JPA kay-9abel `"abc..."` m3a `"abc...   "`,
l-equality kay-fshel b tari9a **makhfiya bzzaf**. Had l-bug s3ib t-debugih.

```sql
revoked_at TIMESTAMPTZ
```
**Soft revoke** — l-row kaybqa l-l-audit ("shkun kan 3ndo access w imta t-7iyyed").

⚠️ L-filter khasso ykun `findByKeyHashAndRevokedAtIsNull`, **machi** `findByKeyHash`.
Ila nsiti, l-keys l-révoqués kaybqaw khaddamin — **faille de sécurité**.

```sql
CONSTRAINT uq_api_keys_user_name UNIQUE (user_id, name)
```
Noms distincts f l-UI (`"CI pipeline"` 3iwad UUID), w kay-3ti l-index 3la
`user_id` l-`GET /api/keys`. Jarrebto:
```
INSERT juj keys b isem 'ci'  →  ERROR: violates "uq_api_keys_user_name"
```

---

## 8. Ash mverifi fe3lan

```
Migrating schema "public" to version "10 - users auth"
Successfully applied 1 migration    →    BUILD SUCCESS, Tests run: 1
```

| Jarrebt | Natija |
|---|---|
| `email = 'Test@X.com'` | ❌ `users_email_check` |
| `username = 'TEST'` | ❌ `users_username_check` |
| `display_name` naqes | ❌ `not-null constraint` |
| `two_factor_enabled = TRUE` bla secret | ❌ `chk_users_2fa_secret_present` |
| `role = 'SUPERADMIN'` | ❌ `users_role_check` |
| `provider = 'google'` (lowercase) | ❌ `oauth_provider_check` |
| Google mrra tanya l-nafs l-user | ❌ `uq_oauth_user_provider` |
| Juj api_keys b isem `ci` | ❌ `uq_api_keys_user_name` |
| User s7i7 | ✅ id = `f9324a6b-8bda-4edb-b570-fe6368a9e778` |
| `DELETE` l-user | ✅ CASCADE mse7 l-oauth + refresh_token + api_key → 0/0/0 |

### Kifash t-testiha b rask

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd ~/Documents/GitHub/SkillSwap
docker compose up -d
cd backend && ./mvnw test
```

```bash
docker exec -it skillswap-postgres psql -U skillswap -d skillswap -c '\d users'
docker exec -it skillswap-postgres psql -U skillswap -d skillswap \
  -c 'SELECT version, description, success FROM flyway_schema_history;'
```

⚠️ **Bedlt had l-file mn be3d ma t-applika.** Flyway kay-khzen checksum — ila bdeltih
3awtani w l-DB déjà fiha V10, l-boot kay-fshel b `Migration checksum mismatch`.
L-7al f dev: `docker compose down -v && docker compose up -d`.
**F production wla mn be3d ma t-pushi: ma t-bedel-sh V10, sayeb `V11__...sql`.**

---

## 9. As'ila momkin f l-eval

**S: 3lash UUID w machi `BIGSERIAL`?**
J: 7it kay-claimi Public API w l-ids kay-tban-hom bra. B `BIGSERIAL`, `/users/5`
kay-kchef belli 3ndna ~5 users, w l-scraping kaywelli `for i in 1..N`. UUID
ma kay-kchef-sh l-7ejm w l-enumeration mustahila. L-cout: 16 bytes 3iwad 8,
w l-insert random f l-index. F l-7ejm dyalna ma kay-bayn-sh.

**S: 3lash `TIMESTAMPTZ` w l-design kaygol `timestamp`?**
J: `DEFAULT now()` 3la column `timestamp` kay-khzen l-wall clock dyal l-**server**,
machi UTC — w l-design rasso kteb "store in UTC". Jarrebtha: b session f Tokyo,
l-value t-khzenat `00:37`, w melli rje3t l-UTC b9at t9ra `00:37` — ghalta b 9 swaye3
w bla hta ishara. L-khatar l-kbir f `refresh_tokens.expires_at`: token kay-3ish
swaye3 zyada 3la l-maqsoud.

**S: Fen l-salt dyal l-password?**
J: dakhel `password_hash`. BCrypt kay-génére l-salt wahdo w kay-khezzno f l-output
(`$2a$10$<salt><hash>`), 3lahekka ma kayn column `salt`.

**S: 3lash SHA-256 l-l-tokens w BCrypt l-l-passwords?**
J: l-token `SecureRandom` 32 bytes = 256 bit d'entropie, ma kayn 3lih dictionary
attack. Password dyal bnadem ~30 bit, khasso key-stretching bati. Zid: l-token
kay-t-verifa f kol request — BCrypt kay-9tel l-performance.

**S: 3lash 3ndek `refresh_tokens` w JWT bjuj?**
J: JWT ma yqdar-sh y-trévoqué — self-contained, l-server ma kay-lawwej-sh f l-DB.
Logout, badl l-password, ban l-user: kolhom ma kay-wa99fu-sh l-JWT. 3lahekka
l-access token 9sir (15 min) w l-révocation kat-skon f l-refresh token, li
**kay-t9ra mn l-DB** f kol tajdid.

**S: 3lash `VARCHAR` + `CHECK` w machi enum natif dyal PostgreSQL?**
J: `ALTER TYPE ... ADD VALUE` ma yqdar-sh y-khdem f transaction f ba3d l-versions,
ya3ni l-migration Flyway kat-fshel. W 7iyyed value mn enum mustahil bla ma t-saweb
type jdid. B `VARCHAR` + `CHECK`, `DROP CONSTRAINT` + `ADD CONSTRAINT` f migration
3adiyya.

**S: 3lash ma 3ndek-sh index 3la `oauth.user_id` w l-design kaytelbo?**
J: 3ndi wa7ed — dakhel l-`UNIQUE (user_id, provider)`. PostgreSQL kaysta3mel
index composite l-queries 3la l-préfixe. Index zaid = batta f l-INSERT 3la walo.

---

## 10. Ash jay

| Step | Status |
|---|---|
| 1. V10 | ✅ salat (had l-file) |
| 2. `common/exception/` | ✅ salat — `docs/step2-common-exception.md` |
| 3. `common/security/` | 🟢 jay |
| 4. `user/` | ⚠️ 3qel 3la `display_name NOT NULL` + `Locale.ROOT` |
| 5. `auth/` JWT + **refresh tokens** | l-flow tbedel, chouf section 6 |
| 6. OAuth2 + 2FA | `two_factor_*` (machi `totp_*`) |
| 7. `publicapi/` | kaykhtaj `api_keys` — chouf section 2 |

**W l-7wayj li khassek tgoulhom l-l-équipe daba:**
1. `role` w `api_keys` zedthom — ila t-9te3u b 9asd, goulia (**4 points**)
2. Credits w friendships naqsin kamlin — **3 points**, part dyal Retahri w Nisrine
3. `timestamp` → `timestamptz` f kol l-design
4. `email_verified` ma 3ndo tari9a y-welli TRUE
5. L-enums: `VARCHAR` + `CHECK` 3iwad `CREATE TYPE` (chouf section 4)
