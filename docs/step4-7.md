# Steps 4–7 — `user/`, `auth/`, 2FA + OAuth2, `publicapi/`

> Owner: **Risattou**
> ~40 files · `./mvnw verify` → **BUILD SUCCESS, 95 tests, 0 failures**
> Jra juj mrrat: l-DB kat-bqa `0/0/0/0` rows

```
Step 4  user/        entity, repository, 4 DTOs, service, controller
Step 5  auth/        JwtProperties, AuthConfig, TokenService, RefreshToken + repo,
                     RefreshTokenService, 4 DTOs, AuthService, AuthController
Step 6  auth/ + common/util
                     OAuthProvider, OAuthAccount + repo, OAuthAccountService,
                     OAuth2LoginSuccessHandler, TotpService, TwoFactorService,
                     4 DTOs, TwoFactorController, Base32, SecretEncryptor
Step 7  publicapi/   ApiKey + repo + service, 4 DTOs, ApiKeyController,
                     ApiKeyAuthFilter, RateLimitFilter, PublicApiSecurityConfig,
                     PublicApiController
common/util          SecureTokens  (mshared: refresh tokens + API keys)
```

---

## 0. L-ewwel haja: 4 bugs l-tests lqawhom

Qbel l-tafsir dyal l-code, had l-arb3a 7it hom l-dars l-7a9i9i dyal had l-khdma.
Kolhom **t-compilaw w banu khaddamin**, w l-tests hom li kchfuhom.

### Bug 1 — bulk update kay-douz **fo9** l-persistence context

```java
@Modifying                                          // ❌ kan hakka
@Query("update RefreshToken t set t.revokedAt = :at where t.user.id = :userId ...")
int revokeAllForUser(UUID userId, Instant at);
```

L-test `reusingARetiredRefreshTokenRevokesEverySessionOfThatUser` fshel b
`expected 401, was 200`.

**Ash kan kay-wqe3:** JPQL bulk update kay-khdem **direct 3la l-DB**, ma kay-douz-sh
3la l-persistence context. Ya3ni:

```
1. rotate(T1)  →  findByTokenHash(T1)   →  entity T1 dkhlet f l-session cache
2. reuse detected  →  revokeAllForUser()  →  UPDATE kay-mshi l-DB
3. rotate(T2)  →  findByTokenHash(T2)   →  Hibernate kay-rejje3 T2 mn l-CACHE
                                            b revokedAt = NULL  🔴
```

L-DB fiha `revoked_at` m3emmer, walakin l-Java kay9ra `null` → **l-token l-msrouq
baqi khaddem mn be3d l-reuse detection**. Ya3ni l-mécanisme kamel dyal theft
detection kan **décoration**.

**L-7al:**
```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
```

| Flag | Ash kaydir | Bla-h |
|---|---|---|
| `flushAutomatically` | kay-flushi l-changements l-pendantes qbel l-UPDATE | `stored.revoke(now)` li drna 9bel baqi f l-mémoire → l-UPDATE ma kay-shufha-sh, w l-flush li jay mn be3d yqder y-kteb l-value l-9dima fo9 l-natija |
| `clearAutomatically` | kay-khewwi l-session cache mn be3d | kol read f nafs l-transaction kay-ji mn l-cache l-stale |

🔑 **L-9a3ida:** kol `@Modifying` b `update`/`delete` **khasso** l-juj flags.
Qolha l-Retahri — l-`UPDATE users SET credits = credits - 1` dyalo nafs l-7ala.

### Bug 2 — `@Validated` kay-3ti 500 3iwad 400

Kteb `@Validated` 3la `PublicApiController` bach `@Max(100)` 3la `size` t-khdem.
L-test `pageSizeIsCapped` rejje3 **500**:

```
jakarta.validation.ConstraintViolationException: users.size: must be less than or equal to 100
	at org.springframework.validation.beanvalidation.MethodValidationInterceptor.invoke(...)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(...)
```

**Kayn juj chemins mkhalfin dyal l-validation f Spring, w kol wahd kay-rmi
exception mkhalfa:**

| Chemin | Exception |
|---|---|
| `@Validated` 3la l-class (proxy CGLIB + `MethodValidationInterceptor`) | `jakarta.validation.ConstraintViolationException` |
| Spring MVC natif (bla `@Validated`) | `HandlerMethodValidationException` |

L-handler dyali f step 2 kan kay-9bet ghir l-tani. **L-ewwel kan kay-s9ot f
l-fallback → 500.**

**L-7al, juj 7wayj:**
1. Hiyydt `@Validated` mn l-controller → chemin wa7ed l-l-web params
2. **Zedt handler l-`ConstraintViolationException`** f `GlobalExceptionHandler`

L-tani mohim l-l-équipe: ila Retahri wla Nisrine sta3mlu `@Validated` f service
(w hnak howa l-outil s7i7), kol violation kanet ghadi t-welli **500**.

### Bug 3 — `OrderBy` f l-isem kay-ghalleb l-`Sort`

```java
Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);   // ❌
```

L-test `theDirectoryFiltersSortsAndPages` tleb `sort=username&direction=asc` w
rejje3 `gamma` f l-ewwel 3iwad `alpha`.

**Ash kan kay-wqe3:** melli derived query method fih `OrderBy`, dak l-ordre
kay-rbe7 3la l-`Sort` dyal l-`Pageable`. Ya3ni l-param `sort` dyal l-client
kan **m-hmel f sokout**.

W hadi machi détail: module **Advanced search** (1 pt) kat-9iyyem 3la
*"filters, sorting and pagination"*. L-sorting kan khasser w l-build akhdar.

**L-7al:** `userRepository.findAll(pageable)` — l-ordre kayji ghir mn l-`Pageable`.

### Bug 4 — `./mvnw test` kan kay-jri **31 test mn 95**

Ba3d ma ktebt l-ITs, l-output:
```
Tests run: 31, Failures: 0, Errors: 0   →   BUILD SUCCESS
```

**Surefire by default kay-9bet `*Test`, `Test*`, `*Tests`, `*TestCase`.**
`*IT` howa l-convention dyal **Failsafe**, machi Surefire → l-11 classe `*IT`
dyali t-skippaw **f sokout**. Build akhdar li ma kay3ni walo l-64 test l-okhrin.

**L-7al** f `pom.xml`:
```xml
<includes>
    <include>**/*Test.java</include>
    <include>**/*Tests.java</include>
    <include>**/*IT.java</include>
</includes>
```

🔑 **Dars:** melli tzid tests, **shuf l-3dad**. "BUILD SUCCESS" bo7do ma kay-wri-sh
belli l-tests jraw.

### Zid: juj startup checks 9betu bugs dyali qbel l-production

```
IllegalStateException: app.security.encryption-key must decode to 32 bytes
                       for AES-256, got 31
```
L-dev key li ktebt (`"dev-only-insecure-key-32-bytes!"`) **31 char**. L-check f
l-constructeur wa99ef l-boot. Bla-h, l-ewwel user li y-dir enrolment dyal 2FA
kan ghadi y-shuf 500.

Nafs l-haja f `TokenService`: secret < 32 byte → `MACSigner` kay-rmi f l-ewwel
token. Daba kay-refdi y-boot.

🔑 **L-9a3ida:** validé l-config f l-**constructeur**, machi f l-ewwel usage.
Configuration ghalta khassha t-men3 l-démarrage, machi t-welli 500 f production.

---

## 1. Step 4 — `user/`

### `User.java` — 3lash ma kayn-sh setters

```java
@Entity
@Table(name = "users")
public class User {
    protected User() { }                                    // JPA kaytellebha

    private User(String username, String email, String displayName, String passwordHash) { ... }

    public static User createLocal(...)        { ... }
    public static User createFromProvider(...) { ... }

    public void updateProfile(...)        { ... }
    public void enableTwoFactor(String encryptedSecret) { ... }
    public void disableTwoFactor()        { ... }
    public void stageTwoFactorSecret(...) { ... }
    public void changeRole(Role role)     { ... }
    public void markEmailVerified()       { ... }
    public void touchLastSeen(Instant at) { ... }
}
```

**Juj factories 3iwad constructeur wa7ed:**

| | `createLocal` | `createFromProvider` |
|---|---|---|
| `passwordHash` | BCrypt hash | **`null`** |
| `emailVerified` | `false` | **`true`** |

L-tani howa 3lash V10 kay-khelli `password_hash` nullable. W `emailVerified = true`
7it l-provider déjà verifa l-email.

**3lash méthodes b asma' 3iwad setters:** l-entity kat-7afed 3la l-CHECK constraints
dyal V10 **mn dakhel Java**:

```java
public void enableTwoFactor(String encryptedSecret) {
    if (encryptedSecret == null || encryptedSecret.isBlank()) {
        throw new IllegalArgumentException("Cannot enable 2FA without a secret");
    }
    this.twoFactorSecret = encryptedSecret;
    this.twoFactorEnabled = true;          // l-juj m3a b3dhom, 3omrhom ma yt-far9u
}

public void disableTwoFactor() {
    this.twoFactorEnabled = false;         // l-flag L-EWWEL
    this.twoFactorSecret = null;
}
```

B `setTwoFactorEnabled(true)` + `setTwoFactorSecret(null)` f juj star mkhalfin,
chi wahd nhar y-nsa l-tani w kay-tle3:
```
ERROR: violates check constraint "chk_users_2fa_secret_present"
```
Wla aqra7: account **locked out l-abad** (l-login kaytelleb code, ma kayn secret).

L-test `disablingNeedsThePasswordAgain` kay-verifi belli mn be3d l-disable
**l-juj** `twoFactorEnabled == false` w `twoFactorSecret == null`.

**Ash ma dert-sh w 3lash:**

| Ma dert-sh | 3lash |
|---|---|
| Lombok `@Data` | kay-génére `equals`/`hashCode` 3la **kol** l-fields. Entity f `Set` li t-modifa mn be3d kat-dya3 |
| `toString()` généré | kay-tbe3 **`passwordHash`** w `twoFactorSecret` f l-logs |
| `equals`/`hashCode` | l-default (identity) kafi hna. Wa7ed 3la l-`id` kay-kherbe9 melli l-entity mazal ma t-sauvegarda (`id == null`) |

### `@GeneratedValue(strategy = GenerationType.UUID)`

L-id kay-t-génére **f Java**, qbel l-`INSERT`. 3lash machi l-`DEFAULT gen_random_uuid()`
dyal V10:

```java
User user = User.createLocal(...);
// l-id déjà 3ammer hna, bla round-trip l-DB
userRepository.save(user);
return UserResponse.from(user);     // kay-khdem
```

B l-DB-generated, khassek `saveAndFlush` + refresh bach t-3ref l-id. W melli
t-7taj trbot rows akhorin f nafs l-transaction, l-id khasso ykun m3ruf 9bel.

L-`DEFAULT` f V10 **khellito** b7al safety net l-SQL manuel (seed data, fixes).

### `@CreationTimestamp` + `@UpdateTimestamp`

```java
@CreationTimestamp
@Column(name = "created_at", nullable = false, updatable = false)
private Instant createdAt;

@UpdateTimestamp
@Column(name = "updated_at", nullable = false)
private Instant updatedAt;
```

V10 3ndo `DEFAULT now()` f l-juj, walakin dak l-default kay-khdem **ghir f
l-`INSERT`**. Bla `@UpdateTimestamp`, `updated_at` kaybqa 9dim l-abad.

`updatable = false` 3la `createdAt`: ma kayn tari9a chi `UPDATE` y-bedlo.

### ⚠️ `display_name NOT NULL` — l-trap dyal V10

```java
String displayName = hasText(request.displayName())
        ? request.displayName().trim()
        : username;
```

L-DBML kay-tellebha `NOT NULL`, walakin l-signup b email kaytelleb ghir
username + email + password. Bla had l-fallback, **kol registration = 500**.

L-test `displayNameFallsBackToTheUsernameWhenOmitted` kay-verifi.

### ⚠️ `toLowerCase(Locale.ROOT)` — l-trap dyal l-locale

```java
private String normalise(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
}
```

V10 3ndo `CHECK (email = lower(email))`, 3lahekka l-service khasso y-normalizi.
**Walakin `Locale.ROOT` machi ikhtiyari:**

`toLowerCase()` bla paramètre kay-sta3mel l-**default locale** dyal l-JVM.
F locale Turkish:
```java
"I".toLowerCase()              // → "ı"  (bla n9ta!)
"I".toLowerCase(Locale.ROOT)   // → "i"
```

Ya3ni server b `-Duser.language=tr` kay-khzen username mkhalef 3la nafs l-input
f machine khra. W l-login mn be3d ma kay-lqa-sh l-user. Bug mashhour w s3ib
t-debugih.

L-test `usernameAndEmailAreStoredLowercase` kay-verifi l-normalisation,
w `anEmailThatDiffersOnlyInCaseIsTheSameEmail` kay-verifi l-natija dyalha.

### `RegisterRequest` — juj détails

```java
@NotBlank
@Size(min = 3, max = 50)
@Pattern(regexp = "^[A-Za-z0-9._-]+$")       // ← kay-9bel MAJUSCULES
String username,

@NotBlank
@Size(min = 8, max = 72)                      // ← 72 machi raqm 3afwi
String password,
```

**L-pattern kay-9bel majuscules b 9asd.** L-value **l-mkhezzna** khassha tkun
lowercase, machi li kteb l-user. Ila l-pattern kan `^[a-z0-9._-]+$`, `"Risattou"`
kan ghadi y-trefdi b 400 3iwad y-t-normalizi.

**3lash `max = 72`:** **BCrypt kay-hashi ghir l-ewwel 72 byte** w kay-hmel l-ba9i
**f sokout**. Ya3ni:
```
password dyal 100 char  →  BCrypt kay-shuf 72 ghir
l-user kay-dhenn 3ndo password 9wi  →  l-28 byte l-okhrin ma kay-zidu walo
```
B `max = 72`, l-limite wadha 3iwad makhfiya.

L-test `aPasswordLongerThanBcryptCanHashIsRejected` kay-verifi.

### Juj projections: `UserResponse` vs `PublicUserResponse`

| | `UserResponse` (`/me`) | `PublicUserResponse` (l-akhorin) |
|---|---|---|
| `email` | ✅ | ❌ |
| `role` | ✅ | ❌ |
| `emailVerified`, `twoFactorEnabled` | ✅ | ❌ |
| `passwordHash`, `twoFactorSecret` | ❌ **3omrhom** | ❌ |

**3lash juj records w machi wa7ed b flag:** nhar li chi wahd yzid field l-l-view
dyal l-mall (mathal `lastLoginIp`), b record wa7ed kay-tnashar l-**kolshi**
automatiquement. B juj, khasso y-zido f l-tani b 9asd.

L-`role` machi public 7it **shkun admin machi information 3amma**.

L-test `registersAUserAndNeverEchoesCredentialMaterial` kay-verifi
`$.passwordHash`, `$.password`, `$.twoFactorSecret` kolhom `doesNotExist()`.

### L-race dyal l-registration — 3lash juj couches

```java
if (userRepository.existsByUsername(username)) {
    throw new ConflictException("USERNAME_TAKEN", ...);
}
```

Had l-check **machi** l-garantie. Juj requests f nafs l-waqt:
```
t=0  A: existsByUsername("x") → false
t=1  B: existsByUsername("x") → false      ← l-juj shafo "free"
t=2  A: INSERT → ✅
t=3  B: INSERT → 🔴 violates users_username_key
```

L-garantie l-7a9i9iyya hiya l-**`UNIQUE`** f V10. W l-handler dyal step 2
(`DataIntegrityViolationException` → 409 `CONSTRAINT_VIOLATION`) kay-khewwel
dak l-fashal l-response ndifa.

Ya3ni: l-check kay-3ti **message mzian** f l-7ala l-3adiyya, w l-contrainte
kat-protegi f l-7ala dyal l-course. **L-juj lazmin.**

### Validation dyal l-timezone

```java
try { ZoneId.of(request.timezone().trim()); }
catch (DateTimeException ex) {
    throw new BadRequestException("INVALID_TIMEZONE", "Not a known IANA time zone id.");
}
```

Machi `@Pattern` — l-base dyal l-zones dyal l-JDK hiya l-marje3 l-wa7ed 3la ash
howa zone s7i7. B regex, `"Africa/Casablanka"` (b typo) kan ghadi y-doz, w
kay-fshel ghir melli l-frontend y-jarreb y-renderi waqt bih.

---

## 2. Step 5 — `auth/` JWT + refresh tokens

### 3lash kaynin refresh tokens — l-fikra l-asasiyya

🔑 **JWT ma yqdar-sh y-trévoqué.** L-server kay-verifi l-signature w safi —
**ma kay-lawwej-sh f l-DB**. Natija:

| L-7ala | Wash l-JWT kay-wqef? |
|---|---|
| L-user dar logout | ❌ baqi valide |
| L-user bdel l-password (msrouq) | ❌ baqi valide |
| L-admin banna l-user | ❌ baqi valide |
| L-role tbedel | ❌ baqi b l-role l-9dim |

Ya3ni **b JWT bo7do, "logout" kay-welli décoration**: l-frontend kay-mse7 l-token
mn l-storage, walakin ila chi wahd 9bto 9bel, kaybqa khaddem.

**L-7al l-standard = juj tokens:**

| | Access token (JWT) | Refresh token |
|---|---|---|
| Fin | `Authorization` header, kol request | f l-DB (SHA-256) |
| Mudda | **15 min** | **30 yum** |
| Verification | signature ghir | **lookup f l-DB** |
| Révocable? | **la** | **ah** |

L-mantiq: l-access token **9sir** 7it ma t9der-sh t-wa99fo — 15 min dyal darar
f l-a9sa. L-révocation kat-skon f l-refresh token, li **kay-t9ra mn l-DB** f kol
tajdid, w dak l-wa9t t9der tgoul "la".

L-test `logoutMakesTheRefreshTokenUnusable` howa l-proof: hadi l-haja li JWT
bo7do **ma yqdar-sh** y-dirha.

### `TokenService` — l-verification star b star

```java
private Optional<JWTClaimsSet> claims(String token) {
    SignedJWT jwt = SignedJWT.parse(token);

    // 1. L-signature L-EWWEL
    if (!jwt.verify(new MACVerifier(secret))) return Optional.empty();

    JWTClaimsSet set = jwt.getJWTClaimsSet();

    // 2. L-issuer
    if (!properties.issuer().equals(set.getIssuer())) return Optional.empty();

    // 3. L-expiration
    Date expiry = set.getExpirationTime();
    if (expiry == null || expiry.toInstant().isBefore(Instant.now())) {
        return Optional.empty();
    }
    return Optional.of(set);
}
```

**L-ordre mohim.** L-signature l-ewwel: **hta haja dakhel l-token ma kat-ttheq
qbel-ha**. Ila 9rina l-claims qbel ma n-verifiw, kan ghadi n-khdemu b data li
l-attaquant kteb-ha.

**`expiry == null` = invalide, machi "bla limite".** Had l-far9 khatir: token
bla `exp` kan ghadi ykun valide **l-abad**. L-choix l-amen howa l-refus.

**L-issuer:** kay-men3 token mn system akhor (mathal environnement dyal staging)
mn l-isti3mal f production — hta ila l-secret t-partagea b 3alat.

L-test `aTokenSignedWithAnotherKeyIsRejected` kay-verifi l-signature,
w `aGarbageTokenIsRejectedWithoutAnError` kay-verifi belli l-refus kay-dir
**401 ndif**, machi 500.

### 🔑 L-claim `scp` — l-bypass dyal l-2FA li kan momkin

Hadi **a7san décision tqniyya f step 5**. L-mushkil:

```
1. L-user kay-dekhel l-password s7i7
2. 3ndo 2FA → l-server khasso y-3ti "challenge token" mo'aqqat
3. L-user kay-sedd l-code TOTP + l-challenge → kay-khod l-tokens
```

L-challenge token **m-signé b nafs l-mefta7** dyal l-access token. Ya3ni:

```
🔴 Bla hta haja: l-attaquant 3ndo l-password → kay-khod l-challenge →
   kay-sta3mlo b7al "Authorization: Bearer <challenge>" →
   l-JWT filter kay-verifi l-signature → VALIDE →
   DKHEL BLA L-2FA
```

L-2FA kamel kay-welli bla ma3na: l-password bo7do kafi.

**L-7al — claim `scp` f l-challenge, w refus f `parse`:**

```java
public IssuedToken issueTwoFactorChallenge(User user) {
    ... .claim(SCOPE_CLAIM, SCOPE_TWO_FACTOR)      // scp = "2fa"
}

@Override
public Optional<AuthenticatedUser> parse(String token) {
    return claims(token)
            .filter(set -> set.getClaim(SCOPE_CLAIM) == null)   // ← l-star l-mohimma
            ...
}
```

`parse` (li l-JWT filter kay-3eyyet) kay-refdi **ay** token fih `scp`.
W `parseTwoFactorChallenge` kay-9bel **ghir** li fih `scp == "2fa"`.

Juj tokens, nafs l-mefta7, **juj isti3malat mkhalfin tmaman**.

L-test:
```java
@Test
void theChallengeTokenCannotBeUsedAsAnAccessToken() { ... }
```
→ 401 `UNAUTHENTICATED`. Bla dak l-`.filter(...)`, had l-test kan ghadi y-fshel
b **200**.

### `completeTwoFactorLogin` — check zaid

```java
if (!user.isTwoFactorEnabled()) {
    throw new UnauthorizedException("TWO_FACTOR_NOT_ENABLED", ...);
}
```

Kay-protegi mn had l-scénario: l-challenge t-3ta, mn be3d l-user tfa l-2FA
f tab akhor, w l-challenge baqi valide 5 min. Bla had l-check, l-flow kay-kemmel
3la account li 3ndo `twoFactorSecret == null` → NPE wla 7ala gharība.

### `RefreshTokenService` — rotation w theft detection

```java
@Transactional
public Rotation rotate(String rawToken) {
    RefreshToken stored = repository.findByTokenHash(sha256Hex(rawToken))
            .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", ...));

    if (stored.isRevoked()) {
        int revoked = repository.revokeAllForUser(stored.getUser().getId(), now);
        log.warn("Refresh token reuse detected for user {}; revoked {} live token(s)", ...);
        throw new UnauthorizedException("REFRESH_TOKEN_REUSED", ...);
    }

    if (stored.isExpired(now)) { throw ... }

    stored.revoke(now);
    return new Rotation(stored.getUser(), issue(stored.getUser()));
}
```

**Ash kaydir l-rotation:** kol refresh token **sal7 l-isti3mal wa7ed ghir**.
Melli t-sta3mel, kay-trévoqué w kay-t3awed b wa7ed jdid.

**3lash hadi kat-khelli l-srqa t-tban:**

```
L-client l-7a9i9i: T1 → T2 → T3 → ...
L-attaquant srq T1 w sta3mlo mn be3d:
    → T1 déjà révoqué  →  JUJ COPIES KAYNIN
```

Ma kayn tari9a t3ref shkun fihom l-7a9i9i. 3lahekka l-9arar: **révoqui kolshi**
w khelli l-juj y-rje3u y-dekhlu b l-password. Hadchi:
- kay-9te3 l-attaquant
- kay-3lem l-user l-7a9i9i (t-dekhel 3awtani bla sabab = ishara)
- kay-dir `log.warn` l-l-monitoring

L-test:
```java
void reusingARetiredRefreshTokenRevokesEverySessionOfThatUser()
```
Kay-verifi l-3 7wayj: l-token l-msrouq 401 `REFRESH_TOKEN_REUSED`, l-token
dyal l-client l-7a9i9i **7ta howa 401**, w
`findByUserIdAndRevokedAtIsNull(...)` **khawya**.

⚠️ **Had l-test howa li kchef Bug 1.** Bla l-`clearAutomatically`, l-marhala
l-tanya (l-token dyal l-client) kanet kat-rejje3 **200** — ya3ni l-"revoke all"
ma kan kay-dir walo.

### `findByTokenHash` bla filtre — b 9asd

```java
/**
 * Looks up by hash alone, including revoked and expired rows.
 */
Optional<RefreshToken> findByTokenHash(String tokenHash);
```

Hna **l-3aks** dyal `api_keys`. L-caller **khasso y-shuf** l-row l-révoqué bach
y-9bet l-reuse. Ila 7ettina `AndRevokedAtIsNull`, l-token l-msrouq kan ghadi
y-rejje3 ghir `INVALID_REFRESH_TOKEN` — w **l-attaque ma t-tban-sh**.

Far9 dqiq walakin asasi: f `api_keys` l-filtre **lazem** (l-key l-révoqué khasso
y-wqef), f `refresh_tokens` l-filtre **kay-khewwi** l-mécanisme.

### `AuthService` — l-protection mn account enumeration

```java
private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOa0y9pGH0YVYQxWTCRQ8t1sI1Nz9kQ4u";

if (found.isEmpty()) {
    passwordEncoder.matches(request.password(), DUMMY_HASH);   // ← ash ma3na had l-star?
    throw invalidCredentials();
}
```

**L-mushkil l-ewwel: l-message.** Ila rejje3na `EMAIL_NOT_FOUND` l-email machi
msejjel w `WRONG_PASSWORD` l-password ghalet, l-attaquant kay-jarreb 10000 email
b password khawi, w kolla jawab kaygoul-lo "had l-email msejjel f SkillSwap".
Mn be3d: credential stuffing, phishing m-wejjeh.

3lahekka **l-3 7alat b nafs l-code**: `INVALID_CREDENTIALS`.

**L-mushkil l-tani: l-waqt.** Hta b nafs l-message:
```
email ma kayn      →  0 BCrypt  →  ~1 ms
password ghalet    →  1 BCrypt  →  ~100 ms
```
L-far9 **100×**. L-attaquant kay-9is l-waqt w kay3ref. Hadi **timing attack**.

L-`DUMMY_HASH` kay-sawi l-waqt: f l-7ala dyal email ma kayn, kay-dir BCrypt
3la hash dyal value ma 3raf-ha hta wahd — nafs l-cout, nafs l-waqt.

**Nafs l-haja l-account dyal OAuth** (`passwordHash == null`):
```java
if (user.getPasswordHash() == null) {
    passwordEncoder.matches(request.password(), DUMMY_HASH);
    throw invalidCredentials();
}
```
Juj asbab: (a) nafs l-waqt, (b) `passwordEncoder.matches(raw, null)` kay-rmi NPE.

**L-test:**
```java
void unknownEmailWrongPasswordAndProviderOnlyAccountAllAnswerIdentically() {
    String unknown       = failedLogin("nobody@example.com", PASSWORD);
    String wrongPassword = failedLogin("realuser@example.com", "not-the-password");
    String providerOnly  = failedLogin("oauthonly@example.com", PASSWORD);

    assertThat(unknown).isEqualTo(wrongPassword).isEqualTo(providerOnly);
}
```
L-`failedLogin` kay-mse7 l-`timestamp` (li kaytbedel) w kay-9abel **l-response
kamel**. Ila chi wahd nhar bdel wa7ed mn l-3 messages, had l-test kay-7mer.

### `SecureTokens` — 3lash SHA-256 w machi BCrypt

```java
public static String randomToken() {
    byte[] bytes = new byte[32];        // 256 bit
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}

public static String sha256Hex(String value) { ... }   // 64 char hex
```

Su'al kayji f l-eval. **L-jawab f juj nuqat:**

| | Password dyal bnadem | Refresh token / API key |
|---|---|---|
| Entropie | ~30 bit | **256 bit** |
| Dictionary attack | **momkin** | mustahil |
| Khasso key stretching? | **ah** (BCrypt) | la |
| Imta kay-t-verifa | f l-login ghir | **f kol request** |
| Cout dyal BCrypt | ~100ms, maqbul | **kay-9tel l-performance** |

Ya3ni l-juj mkhalfin f l-**juj** l-mi7warin. BCrypt f l-API key = bati bla fa'ida;
SHA-256 f l-password = bla 7imaya.

**`Base64.getUrlEncoder().withoutPadding()`**: l-token kay-mshi f header, f URL
(l-redirect dyal OAuth), w f JSON. L-`+` w l-`/` dyal Base64 l-3adi kaykhtaju
escaping f URL, w l-`=` kay-kherbe9 l-parsing.

L-test `tokensAreUrlSafeBase64WithoutPadding` + `tokensDoNotRepeat` (2000 token).

---

## 3. Step 6 — 2FA + OAuth2

### `SecretEncryptor` — 3lash encryption w machi hashing

🔑 **L-secret dyal TOTP ma yqdar-sh y-thashi.** L-verification dyal code = **i3adat
l-7isab** dyalo, w hadchi kaykhtaj l-bytes l-asliyyin. B hash, ma 3ndek tari9a.

Ya3ni l-7imaya l-wa7da momkina hiya **encryption b mefta7 machi f l-DB**:

```
L-DB t-dumpat + secret plaintext  →  l-attaquant kay-génére l-codes dyal KOL user
                                   →  l-2FA kamel = 0
L-DB t-dumpat + secret chiffré    →  3ndo ghir ciphertext, l-mefta7 f env var
```

```java
private static final int IV_BYTES = 12;      // 96 bit, l-taille li GCM m-specifia 3liha
private static final int TAG_BITS = 128;
private static final String TRANSFORMATION = "AES/GCM/NoPadding";
```

**3lash GCM w machi CBC:**

| | CBC | **GCM** |
|---|---|---|
| Encryption | ✅ | ✅ |
| **Authentication** | ❌ | ✅ |
| Ciphertext m-modifi | kay-déchiffri l-**garbage**, w kay-t-sta3mel b7al data | **kay-fshel** |

B CBC, l-attaquant yqder y-bedel bytes w l-app kat-khdem b natija kherba.
B GCM, l-tag kay-9bet l-modification.

L-test:
```java
void tamperedCiphertextIsRejectedRatherThanDecryptedToGarbage() {
    byte[] raw = Base64.getDecoder().decode(encryptor.encrypt("JBSWY3DPEHPK3PXP"));
    raw[raw.length - 1] ^= 0x01;              // bit wa7ed
    assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(...);
}
```

### L-IV — l-mushkil l-wa7ed l-khatir f GCM

```java
byte[] iv = new byte[IV_BYTES];
RANDOM.nextBytes(iv);                      // ⚠️ JDID f KOL encryption
```

🔴 **I3adat l-isti3mal dyal IV b nafs l-mefta7 kat-hedd l-garanties dyal GCM
tmaman** — machi "kat-d3ef-hom", kat-hedd-hom. L-attaquant yqder y-sta7sel 3la
l-authentication key.

3lahekka l-IV **random f kol mrra**, w kay-t-khzen m3a l-ciphertext (l-IV machi
secret, ghir khasso ykun **machi mkerrer**):

```java
byte[] combined = new byte[iv.length + ciphertext.length];
// iv || ciphertext || tag   →   base64
```

L-test:
```java
void encryptingTheSameValueTwiceGivesDifferentCiphertext() {
    assertThat(encryptor.encrypt("same-secret"))
        .isNotEqualTo(encryptor.encrypt("same-secret"));
}
```
Zid 3la l-correction: ciphertexts m-tsawyin kan ghadi y-goulu l-ay wahd kay9ra
l-table **shkun fihom kay-share secret**.

### `Base32` — 3lash ktebtha b yeddi

**Ma kayn Base32 la f l-JDK** (ghir Base64) **la f l-classpath** (`commons-codec`
machi mawjud — verifit b `dependency:tree`). W authenticator apps **kay-tellbu**
Base32 l-l-param `secret` f l-URI `otpauth://`.

**3lash Base32 w machi Base64 f l-TOTP:**
```
Base64: A-Z a-z 0-9 + /     ← case-sensitive, fih I/l/1 w O/0
Base32: A-Z 2-7             ← case-insensitive, BLA 0 1 8
```
L-`0`, `1`, `8` m-7iyydin b 9asd 7it kay-tshabhu l-`O`, `I`, `B`. L-fikra:
l-user khasso y-9der y-kteb l-secret **b yeddo** melli l-QR ma kay-t-scanna-sh.

**Verifit 3la l-vectors dyal RFC 4648 section 10:**
```java
assertThat(encode("f"))      .isEqualTo("MY");
assertThat(encode("fo"))     .isEqualTo("MZXQ");
assertThat(encode("foo"))    .isEqualTo("MZXW6");
assertThat(encode("foob"))   .isEqualTo("MZXW6YQ");
assertThat(encode("fooba"))  .isEqualTo("MZXW6YTB");
assertThat(encode("foobar")) .isEqualTo("MZXW6YTBOI");
```

🔑 **3lash l-vectors w machi ghir round-trip:** `encode(decode(x)) == x` kay-doz
**hta ila l-alphabet wla l-ordre dyal l-bits ghalet** — ghadi ykun ghalet
b tari9a cohérente. L-vectors dyal l-RFC hom l-wa7din li kay-prouviw belli
l-output dyali howa Base32 l-7a9i9i li l-apps kay-fehmu.

### `TotpService` — SHA-1 machi ikhtiyar 7orr

```java
this.generator = new TimeBasedOneTimePasswordGenerator(
        Duration.ofSeconds(30), 6,
        TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
```

Su'al mo7tamal: *"3lash SHA-1? machi mkesser?"*

**Jawab:** Google Authenticator, Authy, Microsoft Authenticator — kolhom
kay-ftardu **SHA-1, 30 seconde, 6 arqam**. Hadchi ma kayn f l-URI `otpauth://`
tari9a wadha t-forci ghir had-shi (l-param `algorithm` kay-t-hmel mn barcha apps).

SHA-256 ghadi ykun a9wa **f l-nadariyya** w **ghir qabil l-isti3mal f l-wa9i3**:
l-user kay-scanni l-QR, l-app kat-sta3mel SHA-1, l-codes ma kay-tt-fa9u 3omrhom.

Zid: l-hajmat 3la SHA-1 hom **collision attacks**. HMAC-SHA1 ma kay-t'ather-sh
bihom (kaykhtaj preimage), w l-output 6 arqam ghir — l-nuqta l-d3ifa hiya
l-brute force (10⁶), machi l-hash.

### L-drift window

```java
private static final int ALLOWED_DRIFT_STEPS = 1;

for (int step = -1; step <= 1; step++) {
    Instant at = now.plus(generator.getTimeStep().multipliedBy(step));
    if (generator.generateOneTimePassword(key, at) == expected) return true;
}
```

**Bla tolérance:** l-clock dyal l-telephone m-zewwed b 5 seconde → l-user
**ma yqdar-sh ydekhel 3omro**. W l-user li kay-kteb l-code f l-lahda li kay-tbedel
→ fashal.

**L-cout:** l-espace dyal l-recherche kay-tle3 mn 10⁶ l-3×10⁶ — ya3ni l-attaquant
3ndo 3 mrrat ktar dyal chance f kol tajriba. Walakin:
- l-code kay-tbedel kol 30 seconde
- l-endpoint khasso ykun rate-limited (⚠️ **mazal** — chouf section 6)

Ya3ni l-mkasab (users ma kay-tbloqaw-sh) akbar bzzaf mn l-khasara.

L-tests:
```java
void acceptsOneStepOfClockDriftInBothDirections()     // ±30s  ✅
void rejectsACodeFromWellOutsideTheWindow()            // -300s ❌
```
L-tani mohim: l-window **m7dud**. Bla-h, code 9dim kan ghadi ybqa sal7.

### L-QR code — URI machi PNG

```java
public String buildOtpAuthUri(String base32Secret, String accountName) {
    return "otpauth://totp/" + label
            + "?secret=" + base32Secret
            + "&issuer=" + encode(issuer)
            + "&algorithm=SHA1&digits=6&period=30";
}
```

**Ma dert-sh l-QR f l-server.** Juj asbab:
1. Kaykhtaj dependency imaging (`zxing`) f `pom.xml` **l-shared**
2. L-secret kay-welli f **image** — w l-images kay-tcachiw (CDN, browser cache,
   proxy). L-URI f JSON ma kay-tcachi-sh

L-frontend kay-renderi l-QR mn l-URI. L-secret 3omro ma kay-khrej mn l-response
dyal l-API.

L-test kay-verifi kol param (ila `period` wla `digits` ghalet, l-app ma t-scanni-sh):
```java
assertThat(uri).startsWith("otpauth://totp/SkillSwap:")
        .contains("secret=" + secret).contains("algorithm=SHA1")
        .contains("digits=6").contains("period=30");
assertThat(uri).contains("risattou%40example.com");    // l-@ m-encodé
```

### `TwoFactorService` — l-enrolment f **juj** marhala

```java
public TotpEnrollResponse startEnrolment(UUID userId) {
    String secret = totpService.generateSecret();
    user.stageTwoFactorSecret(totpService.encrypt(secret));   // ← l-flag BAQI OFF
    return new TotpEnrollResponse(secret, totpService.buildOtpAuthUri(...));
}

public void confirmEnrolment(UUID userId, TotpConfirmRequest request) {
    if (!totpService.verifyPlain(request.secret(), request.code())) {
        throw new BadRequestException("INVALID_TOTP_CODE", ...);
    }
    user.enableTwoFactor(totpService.encrypt(request.secret()));   // ← daba ON
    refreshTokenService.revokeAllForUser(userId);
}
```

🔑 **3lash juj appels w machi wa7ed:**

Ila l-`enroll` khddem l-2FA direct:
```
L-user kay-clicki "Enable 2FA"  →  l-QR kay-tban  →  l-scan FSHEL (telephone
mcha, app ghalta, l-user sedd l-page)  →  2FA m-khddem  →  l-login jay kaytelleb
code  →  MA KAYN TARI9A y-générih  →  ACCOUNT MQFOUL
```
W l-7al kay-khtaj **SQL manuel** mn l-admin.

B juj marhala: l-enrolment m-khella kay-khelli l-account **b exactement b7al ma kan**.

**`revokeAllForUser` mn be3d l-confirm** — 7aja dqiqa walakin mohimma:
l-sessions l-9dima t-sawbu **bla** second factor. Ila khellinahom:
```
L-attaquant 3ndo session msrouqa  →  l-user kay-khddem l-2FA bach y-7mi rasso
→  l-session dyal l-attaquant BAQYA KHADDAMA
```
L-2FA kan ghadi y-7mi **ghir l-logins l-jdid**.

L-test:
```java
void aValidCodeCompletesEnrolmentAndEndsOtherSessions() {
    refreshTokenService.issue(user);                    // session dyal qbel
    ... confirm ...
    assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
            .isEmpty();                                 // ← mcha
}
```

### Disable — 3lash l-password 3awtani

```java
if (user.getPasswordHash() == null
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
    throw new UnauthorizedException("INVALID_CREDENTIALS", "Password is incorrect.");
}
```

L-access token bo7do **ma kafi-sh** hna. L-mantiq: ila token t-srek, **l-7arka
l-ewwela** dyal l-attaquant hiya t-tfi l-2FA (bach y-9der ydekhel mn be3d
b l-password ila 9bto, wla bach y-khelli l-access).

B check dyal l-password, l-attaquant li 3ndo **ghir** l-token ma yqdar-sh.

L-test `disablingNeedsThePasswordAgain` kay-jarreb l-juj: password ghalet → 401
w l-2FA **baqi on**; password s7i7 → 204 w l-juj `twoFactorEnabled == false`
w `twoFactorSecret == null`.

### `OAuthAccountService` — 3 7alat, w l-wa7da l-khatira

```java
// 1. L-identité déjà m-rbota
if (existingLink.isPresent()) return existingLink.get().getUser();

// 2. L-email 3ndo account → RBOT
if (byEmail.isPresent()) {
    oauthAccountRepository.save(new OAuthAccount(user, provider, providerUserId));
    user.markEmailVerified();
    return user;
}

// 3. Sayeb account jdid
```

**L-7ala 2 hiya l-mohimma.** Bla-ha:
```
L-user kay-registri b risattou@gmail.com + password
Mn be3d kay-clicki "Sign in with Google" (nafs l-email)
→  ACCOUNT TANI, l-historique kamel m-dya3
```

**W hiya 7ta l-khatira.** L-rbot 3la l-email **amen ghir 7it l-provider
déjà verifa l-email**. Ila ttheqna f email machi m-verifi:
```
🔴 L-attaquant kay-saweb account f provider b l-email dyal l-victime (bla
   verification) → kay-dekhel → l-system kay-rbto l-l-account l-7a9i9i →
   DKHEL F ACCOUNT MACHI DYALO
```
Google w 42 kay-verifiw l-email, 3lahekka hna safi. **Walakin had l-fard
khasso ykun maktub** — w howa maktub f l-javadoc.

### Génération dyal l-username

Lا Google la 42 kay-3tiw haja kat-wafe9 l-9awa3id dyalna (`^[a-z0-9._-]{3,50}$`):

```java
String base = sanitise(email.split("@")[0]);
if (base.length() < 3) base = sanitise(displayName);
if (base.length() < 3) base = "user";
base = base.substring(0, Math.min(base.length(), 50 - 5));   // blasa l-suffix

if (!userRepository.existsByUsername(base)) return base;
for (int suffix = 1; suffix < 10_000; suffix++) { ... }
throw new IllegalStateException("Could not derive a free username from " + base);
```

L-`throw` f l-ekhher: 10000 collision 3la nafs l-base machi scénario 7a9i9i.
Walakin **l-fashal b sot** a7san mn rejj3an value li ghadi t-kesser
l-`UNIQUE` f V10 b tari9a gharība.

### `OAuth2LoginSuccessHandler` — 3lash **ghir** l-refresh token f l-URL

```java
response.sendRedirect(UriComponentsBuilder.fromUriString(frontendRedirectUri)
        .queryParam("refreshToken", refreshToken)
        .build().toUriString());
```

L-default dyal Spring Security kay-saweb session w kay-redirecti l-page.
7na stateless + frontend f origin akhor, 3lahekka khassna n-3ddiw token.

**L-URL kay-wsel l-blasat mkhalfin:**
- l-historique dyal l-browser
- l-`Referer` header dyal l-page li jay
- l-logs dyal l-proxy

3lahekka **ghir l-refresh token**, machi l-access token. L-mantiq: l-frontend
kay-sta3mlo **mrra wehda** f `/api/auth/refresh`, w **l-rotation kat-9tel-ha
f dik l-lahda**. Ya3ni l-value li b9at f l-historique **ma b9at sal7a**.

B l-access token, l-value f l-historique kat-bqa khaddma 15 min.

> **Ta7sin momkin l-l-mustaqbal:** code mo'aqqat (30 seconde, isti3mal wa7ed)
> 3iwad l-refresh token. Daba l-rotation kat-3ti nafs l-7imaya b code aqell.

### `oauth2Login` conditionnel

```java
ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
AuthenticationSuccessHandler successHandler = oauthSuccessHandlers.getIfAvailable();
if (registrations != null && successHandler != null) {
    http.oauth2Login(login -> login
            .clientRegistrationRepository(registrations)
            .successHandler(successHandler)
            .failureHandler(authErrorHandler::commence));
}
```

**Bla had l-condition, l-app ma t-bootiya-sh 3la ay machine bla Google/42
credentials** — w 7na ma 3ndna-sh (`.env.example` kay-khelli l-ids khawyin).

Spring kay-saweb `ClientRegistrationRepository` **ghir** melli ykun 3la l-a9all
registration wehda b `client-id`. `oauth2Login()` bla condition kaykhtaj dak
l-bean → `NoSuchBeanDefinitionException` → l-app kamel m-wa99ef.

**`.failureHandler(authErrorHandler::commence)`** — default-deny kay-tabe9 7ta hna:
login dyal provider li fshel **ma khasso-sh** y-s9ot f l-page HTML dyal Spring.

---

## 4. Step 7 — `publicapi/`

### Juj security chains — 3lash

| | Main API | Public API |
|---|---|---|
| Path | kolshi | `/api/v1/public/**` |
| Credential | `Authorization: Bearer <JWT>` | `X-API-Key` |
| Rate limited | la | **ah, per key** |
| Chain | `SecurityConfig` `@Order(2)` | `PublicApiSecurityConfig` `@Order(1)` |

**3lash juj chains w machi filter wa7ed f chain wehda:** l-séparation. B chain
wehda, ay credential kay-khdem f ay endpoint:
```
🔴 API key kay-wsel l-/api/account/api-keys  →  kay-saweb keys jdid  →
   leak wa7ed kay-welli DA'IM
🔴 JWT kay-wsel l-/api/v1/public/**  →  bla rate limit
```

L-tests:
```java
void aJwtDoesNotAuthenticateAgainstThePublicApi()   // 401
void anApiKeyDoesNotAuthenticateAgainstTheJwtApi()  // 401
```

### ⚠️ `@Order` khasso ykun 3la l-**@Bean method**

Had l-bug wa99ef l-app kamel:

```java
@Configuration
@Order(1)                        // ❌ hna ma kay-dir walo
public class PublicApiSecurityConfig {
    @Bean
    public SecurityFilterChain publicApiFilterChain(...) { ... }
}
```

```
UnreachableFilterChainException: A filter chain that matches any request
[... 'securityFilterChain' ... matching [any request]] has already been
configured, which means that this filter chain [... 'publicApiFilterChain' ...
matching [/api/v1/public/**]] will never get invoked.
```

Spring Security kay-rettib l-**beans** dyal `SecurityFilterChain`, machi
l-classes dyal l-configuration. B l-`@Order` 3la l-class, l-chain khda
l-`LOWEST_PRECEDENCE` → mcha mora l-catch-all → **Spring refda t-démarri**
(w hadi mziana: l-erreur wadha 3iwad chain m-hmel f sokout).

```java
@Bean
@Order(1)                        // ✅
public SecurityFilterChain publicApiFilterChain(...) { ... }
```

### L-ordre dyal l-filters f l-chain

```java
.addFilterBefore(new ApiKeyAuthFilter(apiKeyService),
        UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(new RateLimitFilter(objectMapper, requestsPerMinute),
        ApiKeyAuthFilter.class)
```

**L-key khasso y-t3arref 9bel ma l-limiter y-9arrer ashmen bucket y-7asseb.**
Bla had l-ordre, l-limiter ma y3ref mnin jay l-request.

L-communication binathom b **request attribute**:
```java
// ApiKeyAuthFilter
request.setAttribute(KEY_ID_ATTRIBUTE, apiKey.getId());

// RateLimitFilter
Object keyId = request.getAttribute(ApiKeyAuthFilter.KEY_ID_ATTRIBUTE);
if (keyId == null) { filterChain.doFilter(request, response); return; }
```

**L-`keyId == null` kay-douz bla metering** — mohim: request bla key khasso
y-khod **401** mn l-chain, machi **429** mn l-limiter. L-test:
```java
void anUnauthenticatedRequestIsRefusedRatherThanMetered()   // 401 machi 429
```

### `ApiKey` — l-prefix `ssk_`

```java
private static final String KEY_PREFIX = "ssk_";
String raw = KEY_PREFIX + SecureTokens.randomToken();
```

**Machi décoration.** Secret scanners (GitHub secret scanning, `trufflehog`,
`gitleaks`) kay-fettru 3la patterns m3rufin. Key b prefix wadeh:
- melli chi wahd y-pushih f repo 3am → kay-t-détecti → kay-t-révoqué
- melli y-tbe3 f log → kay-t-filtri

Bla prefix, l-key kay-ban b7al ay string base64 w kay-b9a ghadi.

L-test: `assertThat(body).contains("\"key\":\"ssk_")`.

### L-hash ghir, w soft revoke

```java
ApiKey saved = apiKeyRepository.save(new ApiKey(user, name, SecureTokens.sha256Hex(raw)));
return new ApiKeyCreatedResponse(saved.getId(), saved.getName(), raw, saved.getCreatedAt());
//                                                              ↑ mrra wehda
```

L-test `onlyTheHashIsPersisted` kay-verifi: `stored.getKeyHash()` 64 char
w **machi** l-value l-khawi.

```java
Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);
```

🔑 **`AndRevokedAtIsNull` hiya l-9alb.** L-revocation `TIMESTAMPTZ` machi `DELETE`
(l-row kaybqa l-l-audit). Ya3ni b `findByKeyHash` bo7do, **l-keys l-révoqués
kaybqaw khaddamin** = faille.

L-test li kay-9bet-ha:
```java
void aRevokedKeyStopsWorking() {
    ... 200 ...
    apiKeyService.revoke(user.getId(), stored.getId());
    ... 401 ...
    assertThat(apiKeyRepository.findById(stored.getId())).isPresent();  // l-row b9a
}
```

### `revoke` scoped 3la l-mall

```java
ApiKey key = apiKeyRepository.findByIdAndUserId(keyId, userId).orElseThrow(
        () -> new NotFoundException("API_KEY_NOT_FOUND", "No such API key."));
```

**`findByIdAndUserId`** machi `findById` + check mn be3d. L-far9: b `findById`,
kayn lahda fin 3ndek entity dyal shi wahd akhor f l-mémoire — w sahel t-nsa
l-check.

**W 404 machi 403** melli l-key dyal wahd akhor: b 403, l-attaquant kay-jarreb
ids w kay3ref ashmen wahdin **kaynin**. B 404 l-juj 7alat m-tsawyin.

L-test `anotherUsersKeyCannotBeRevoked` → 404, w l-key **baqi machi révoqué**.

### `RateLimitFilter` — 3lash machi l-starter

**`bucket4j-spring-boot-starter` 0.14.0 f `pom.xml`, w ma sta3melto-sh.** Juj asbab:
1. Kay-**refdi y-démarri bla cache provider** → dependency jdida (Caffeine/JCache)
   f `pom.xml` **l-shared** (PR + 2 reviewers)
2. L-0.14.0 mktub l-**Boot 3**, w 7na 3la Boot 4 milestone

L-core (`io.github.bucket4j`) déjà f l-classpath b7al dependency transitive dyal
l-starter, 3lahekka ktebt filter 3la l-API direct:

```java
Bucket bucket = buckets.computeIfAbsent(keyId, id -> Bucket.builder()
        .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
        .build());

ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
```

### Per-key machi per-IP

```java
private final Map<Object, Bucket> buckets = new ConcurrentHashMap<>();
Object keyId = request.getAttribute(ApiKeyAuthFilter.KEY_ID_ATTRIBUTE);
```

| | Per-IP | **Per-key** |
|---|---|---|
| Juj clients mora NAT wehda | kay-sta7lku 3la b3dhom 🔴 | mustaqill ✅ |
| Client wa7ed b IPs bezzaf | kay-shri quota zaid 🔴 | nafs l-quota ✅ |

L-test `eachKeyHasItsOwnBudget` kay-verifi: key A kay-sta7lek 3/3, key B
baqi kay-khdem.

### L-headers

```java
response.setHeader("X-RateLimit-Limit", ...);
response.setHeader("X-RateLimit-Remaining", ...);
// w 3la 429:
response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
```

**Kay-t-sddu 7ta 3la n-naja7**, machi ghir 3la l-refus: l-client ma yqdar-sh
y-pacei rasso ila ma shaf-sh sh7al b9a-lih **qbel** ma y-sali.

**`Retry-After` 3la 429** standard. Bla-h, l-client ma y3ref sh7al y-tssena
w 3adatan kay-3awed direct — w kay-khelli l-7ala a9ra7.

### ⚠️ L-7add: l-buckets f l-heap

```java
/**
 * <b>Limitation.</b> The buckets live in this instance's heap, so running two
 * instances doubles the effective limit.
 */
```

Juj instances = l-limite 3la **juj** (kol wahd 3ndo buckets dyalo). L-7al
l-7a9i9i = storage m-shared (Redis, Hazelcast) — w **hadak howa 3lash l-starter
kay-telleb cache**.

L-7ala dyalna: instance wehda f Docker Compose → safi. **Walakin maktub**,
machi makhfi.

### L-5 endpoints

| # | Endpoint | Ash kay-3ti |
|---|---|---|
| 1 | `GET /health` | status + version. **L-wa7ed bla key** — bach l-client y-tsheck l-reachability |
| 2 | `GET /users` | directory b `q`, `page`, `size`, `sort`, `direction` |
| 3 | `GET /users/{username}` | profil wa7ed |
| 4 | `GET /stats` | 3dad l-users (aggregate ghir) |
| 5 | `GET /me` | l-account dyal l-key — bach l-client y-3ref b7al shkun kay-khdem |

L-endpoint #2 howa l-module **Advanced search** (1 pt): *filters, sorting,
pagination*.

⚠️ **Kolhom kay9raw ghir l-tables dyal V10.** Skills, classes, leaderboard
kaykhtaju V20/V21 dyal Retahri.

### L-allow-list dyal l-`sort` — 7aja mohimma

```java
String sortProperty = switch (sort) {
    case "username"    -> "username";
    case "displayName" -> "displayName";
    default            -> "createdAt";
};
```

**Bla had l-switch**, l-string dyal l-client kay-wsel **direct l-JPA metamodel**:

```
GET /api/v1/public/users?sort=passwordHash&direction=asc
→  ORDER BY password_hash ASC
```

L-values ma kay-t-rejj3u-sh, **walakin l-ordre kay-kchef 3lihom**. B binary
search 3la l-ordre, l-attaquant kay-sta7sel 3la ma3lumat 3la l-hashes.
Nafs l-haja b `twoFactorSecret`.

L-test:
```java
void anUnknownSortFieldFallsBackInsteadOfReachingTheMetamodel() {
    ... .param("sort", "passwordHash") ...   →  200, fallback l-createdAt
}
```

### `PageResponse` 3iwad `Page` dyal Spring

```java
public record PageResponse<T>(List<T> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last) { }
```

Spring Data `Page` kay-serializa l-**structure dakhiliyya** dyalo (w Boot kay-dir
warning 3la had-shi). L-shape dyalo kay-tbedel bin l-versions → l-API contract
kay-kesser f upgrade.

B record dyalna, l-payload **contract stable**.

L-test kay-verifi kol field: `content.length()`, `page`, `size`, `first`, `last`.

---

## 5. L-tests — kifash m-bniyin

```
95 tests, 0 failures
├── 24  unit (bla Spring)
│   ├── Base32Test                      4   RFC 4648 vectors
│   ├── SecureTokensTest                5   digest, unicité, URL-safe
│   ├── SecretEncryptorTest             6   round trip, IV random, tampering
│   └── TotpServiceTest                 9   drift, encrypted secret, otpauth URI
└── 71  integration (DB + security chains)
    ├── GlobalExceptionHandlerTest      6   standalone, bla DB
    ├── SecurityRulesIT                10   l-chain, roles, default-deny
    ├── UserApiIT                      11   registration, normalisation, conflicts
    ├── AuthFlowIT                     16   login, refresh, rotation, 2FA bypass
    ├── TwoFactorManagementIT           6   enrolment, confirm, disable
    ├── PublicApiIT                    18   keys, chains, endpoints, sort
    ├── RateLimitIT                     3   429, per-key budget
    └── BackendApplicationTests         1   ddl-auto=validate
```

### `@IntegrationTest` — 3lash l-rollback kay-khdem

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public @interface IntegrationTest { }
```

🔑 **MockMvc kay-3eyyet l-controller 3la l-thread dyal l-test**, machi 3la
thread dyal server. Ya3ni l-transaction dyal l-test hiya **nafs-ha** li l-service
kay-dkhel fiha → l-rollback kay-mse7 kolshi.

Verifit:
```bash
./mvnw test   # 95 ✅
./mvnw test   # 95 ✅ (3awtani, nafs l-natija)
psql -c 'SELECT count(*) FROM users ...'   →  0 | 0 | 0 | 0
```

Bla `@Transactional`, l-test l-tani kan ghadi y-fshel b `USERNAME_TAKEN` mn
l-run l-ewwel. W l-ordre dyal l-tests kan ghadi y-3ewwel.

**`RateLimitIT` 3ndo context dyalo** (`@TestPropertySource` b `rpm=3`): l-quota
property dyal startup, w l-buckets kay-3ishu m3a l-filter.

### `ddl-auto=validate` b7al test

`BackendApplicationTests.contextLoads` test wehda, walakin **kat-verifi belli
l-entities kay-matchiw V10 column b column**. Ila ktebt `String credits` 3iwad
`Integer`, wla nsiti `@Column(name = "...")`:

```
Schema-validation: wrong column type encountered in column [credits]
```

L-app **ma t-bootiya-sh**. Ya3ni l-divergence bin l-Java w l-SQL kat-tban f
l-boot, machi f l-production.

---

## 6. Ash mazal — 7wayj khassek t3rfhom

| Haja | L-7ala |
|---|---|
| **Rate limit 3la `/api/auth/login` w `/api/auth/2fa`** | ❌ **ma kayn**. L-limiter kay-7mi ghir l-Public API. Brute force 3la l-password wla 3la l-code TOTP (10⁶, w b drift 3×10⁶) m-mfto7. **Hadi l-naq9a l-kbira l-ba9ya f part dyali** |
| `email_verified` | 🟡 l-OAuth kay-wellih `true`; l-signup b email ma 3ndo tari9a (ma kayn table dyal token) |
| OAuth2 end-to-end | 🟡 l-code m-ktub w m-wsol, walakin **ma t-jarreb-sh** b Google/42 7a9i9iyyin (ma 3ndna-sh credentials) |
| L-5 endpoints | 🟡 kay9raw ghir V10. Skills/leaderboard kaykhtaju V20/V21 |
| Buckets distribués | 🟡 in-memory; instance wehda safi |
| HTTPS / wss | ❌ step 7 f `project_context.md` section 10, machi part dyali |

---

## 7. As'ila momkin f l-eval

**S: 3lash juj tokens w machi JWT wa7ed twil?**
J: JWT ma yqdar-sh y-trévoqué — l-server kay-verifi l-signature w ma kay-lawwej-sh
f l-DB. Logout, badl l-password, ban l-user: hta wa7ed ma kay-wa99fo. 3lahekka
l-access 9sir (15 min = l-window l-a9sa dyal darar) w l-révocation kat-skon f
l-refresh token, li kay-t9ra mn l-DB f kol tajdid.

**S: Ash hiya rotation, w 3lash?**
J: kol refresh token sal7 l-isti3mal wa7ed. Melli t-sta3mel, kay-trévoqué w
kay-t3awed. Ila wa7ed m-révoqué t-presenta 3awtani, ya3ni **juj copies kaynin**
— ya3ni srqa. Ma kayn tari9a t3ref shkun l-7a9i9i, 3lahekka kay-t-révoqué kolshi.

**S: Kifash l-challenge dyal l-2FA ma yqdar-sh y-bypassi l-2FA?**
J: m-signé b nafs l-mefta7, 3lahekka l-signature bo7dha ma kat-farre9-sh.
L-far9 f claim `scp = "2fa"`: `TokenParser.parse` (li l-filter kay-3eyyet)
kay-refdi ay token fih `scp`, w `parseTwoFactorChallenge` kay-9bel ghir li fih.
L-test `theChallengeTokenCannotBeUsedAsAnAccessToken` kay-verifi.

**S: 3lash l-secret dyal TOTP m-chiffré w l-password m-hashé?**
J: l-verification dyal code TOTP = i3adat l-7isab, kaykhtaj l-bytes l-asliyyin.
B hash ma 3ndek tari9a. L-password ma khassha-sh t-rje3: l-comparaison dyal
l-hashes kafya. L-mefta7 dyal l-encryption f env var, machi f l-DB.

**S: 3lash SHA-256 l-l-tokens w BCrypt l-l-passwords?**
J: juj asbab. L-entropie: token = 256 bit mn `SecureRandom`, ma kayn dictionary
attack; password dyal bnadem ~30 bit, khasso key stretching. L-performance:
l-token kay-t-verifa f **kol request**, BCrypt (~100ms) kay-9tel l-latence.

**S: 3lash DUMMY_HASH f l-login?**
J: timing attack. Hta b nafs l-message, email ma kayn = 0 BCrypt (~1ms) w
password ghalet = 1 BCrypt (~100ms). Far9 100×, kafi bach l-attaquant y3ref
ashmen emails msejjlin. L-DUMMY_HASH kay-sawi l-waqt.

**S: 3lash l-enrolment dyal l-2FA f juj appels?**
J: ila l-ewwel appel khddem l-2FA, w l-scan dyal l-QR fshel, l-user
**ma yqdar-sh ydekhel 3omro** — l-login kaytelleb code w ma kayn tari9a
y-générih. B juj, l-enrolment m-khella kay-khelli l-account b7al ma kan.

**S: 3lash `revokeAllForUser` mn be3d ma t-khddem l-2FA?**
J: l-sessions l-9dima t-sawbu bla second factor. Bla l-revocation, l-2FA kay-7mi
ghir l-logins l-jdid — w session msrouqa mn qbel kat-bqa khaddama.

**S: 3lash juj security chains?**
J: séparation. B chain wehda, API key kan ghadi y-wsel l-`/api/account/api-keys`
w y-saweb keys jdid — leak wa7ed kay-welli da'im. W JWT kan ghadi y-wsel
l-Public API bla rate limit.

**S: 3lash allow-list 3la l-param `sort`?**
J: l-string kay-wsel l-JPA metamodel. `sort=passwordHash` kay-3ti
`ORDER BY password_hash`. L-values ma kay-t-rejj3u-sh, walakin **l-ordre kay-kchef
3lihom** — b binary search 3la l-ordre kat-sta7sel 3la ma3lumat 3la l-hashes.

**S: 3lash ma sta3melt-sh `bucket4j-spring-boot-starter`?**
J: kay-refdi y-démarri bla cache provider, ya3ni dependency jdida f `pom.xml`
l-shared, w l-0.14.0 mktub l-Boot 3. L-core API déjà f l-classpath b7al
transitive dependency dyalo.

**S: Sh7al mn test, w ashmen 7aja kay-verifiw?**
J: 95. 24 unit (crypto, Base32 3la l-vectors dyal l-RFC, TOTP drift) w 71
integration 3la l-DB l-7a9i9iyya w l-chains l-7a9i9iyyin. L-ehemm fihom hom
li kay-verifiw propriétés dyal l-sécurité: account enumeration, bypass dyal
l-2FA, theft detection, keys m-révoqués, sort injection.

---

## 8. L-7wayj li khassek tgoulhom l-l-équipe

1. 🔴 **`@Modifying` khasso `(flushAutomatically = true, clearAutomatically = true)`.**
   Qolha l-Retahri: l-`UPDATE users SET credits = credits - 1` dyalo nafs l-7ala,
   w bla hom l-read li jay f nafs l-transaction kay-rejje3 balance stale.
2. 🔴 **`@Validated` f service kay-rmi `ConstraintViolationException`**, machi
   `HandlerMethodValidationException`. Zedt handler f `GlobalExceptionHandler`,
   walakin khassom y3rfu l-far9.
3. 🔴 **`OrderBy` f isem dyal query method kay-ghalleb l-`Sort` dyal l-`Pageable`.**
   Retahri 3ndo `matching/` b sorting — had l-trap ghadi y-lqah.
4. ⚠️ **`./mvnw test` kaykhtaj `docker compose up -d`.**
5. ⚠️ **Surefire includes** bdeltha f `pom.xml` bach `*IT` t-jri. Ila smmaw
   l-tests dyalhom `*IT`, daba kay-khdem.
6. **L-contrats**: `UUID currentUserId()`, `TokenParser.parse`, `AuthenticatedUser`,
   `Role` — kolhom landed f `common/security`. `Role` **machi** f `user/entity`.
7. **`User.changeRole(Role)`** zedtha l-module dyal l-roles. L-endpoint li
   kay-3eyyet-ha khasso ykun `hasRole("ADMIN")`, wella kol wahd kay-promoti rasso.
