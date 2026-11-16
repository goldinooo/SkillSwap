# Step 3 — `common/security/` + `common/config/`

> Owner: **Risattou**
> 9 files · compiled ✅ · 9 tests passed ✅
> `backend/src/main/java/com/trendenden/backend/common/{security,config}/`

Hadi l-blasa l-wa7ida f l-app li kat-9arrer **shkun yqder y-wsel l-ashmen endpoint**.
Retahri w Nisrine kay-bniw 3la l-contrats dyalha, 3lahekka khassha tkun s7i7a daba.

---

## 0. L-flow dyal request

```
GET /api/classes   +   Authorization: Bearer eyJhbGc...
          │
          ▼
┌─────────────────────────────────────────────────────┐
│  CorsConfig          preflight / origin check       │
├─────────────────────────────────────────────────────┤
│  JwtAuthenticationFilter                            │
│    header → TokenParser.parse(token)                │
│    ✅ → SecurityContext = AuthenticatedUser         │
│    ❌ → kay-khelli l-context khawi, kay-kemmel      │
├─────────────────────────────────────────────────────┤
│  authorizeHttpRequests                              │
│    permitAll? hasRole? authenticated?               │
│                                                     │
│    ❌ bla identité  → RestAuthErrorHandler → 401    │
│    ❌ bla 7a9      → RestAuthErrorHandler → 403    │
└─────────────────────────────────────────────────────┘
          │ ✅
          ▼
    DispatcherServlet → Controller
          │                 CurrentUser.currentUserId()
          ▼
    GlobalExceptionHandler (step 2)
```

**L-nuqta l-mohimma:** l-401 w l-403 kay-tsawbu **f l-filter chain**, qbel
l-DispatcherServlet. 3lahekka `@RestControllerAdvice` ma kay-shufhom-sh, w
`RestAuthErrorHandler` howa li kaykteb l-body. Chouf section 5.

---

## 1. L-9 files

| File | Dour |
|---|---|
| `security/Role.java` | enum `USER` / `ADMIN` |
| `security/AuthenticatedUser.java` | l-principal: `UUID id`, `username`, `role` |
| `security/TokenParser.java` | interface — l-seam m3a `auth/` |
| `security/CurrentUser.java` | `UUID currentUserId()` — l-contrat m3a l-équipe |
| `security/JwtAuthenticationFilter.java` | kay9ra `Authorization: Bearer` |
| `security/RestAuthErrorHandler.java` | 401 + 403 b nafs l-`ErrorResponse` |
| `security/SecurityConfig.java` | l-chain, l-rules, `PasswordEncoder` |
| `config/CorsConfig.java` | l-frontend Next.js f origin akhor |
| `config/OpenApiConfig.java` | Swagger "Authorize" button |

---

## 2. ⚠️ L-contrat tbedel: `Long` → `UUID`

`TASKS.md` kaygol `Long currentUserId()`. **Daba wella `UUID currentUserId()`**,
7it V10 wellat UUID primary keys.

**Goul had-shi l-Retahri w Nisrine daba.** Ila bdaw ykitbu `Long userId` f
l-services dyalhom, kol signature ghadi t-tbedel mn be3d.

| Contrat | Signature l-jdida |
|---|---|
| `CurrentUser` | `UUID currentUserId()` |
| `TokenParser` | `Optional<AuthenticatedUser> parse(String token)` |
| `AuthenticatedUser` | `record(UUID id, String username, Role role)` |

---

## 3. 🔴 Juj mushkil 7a9i9iyyin f Boot 4 — lqithom hna

Had l-juj ma kat-lqahom-sh f hta tutorial, 7it Boot 4 jdid. **3qel 3lihom**,
ghadi t-3awdhom f step 5 w 6.

### (a) Boot 4 wella 3la **Jackson 3**

`RestAuthErrorHandler` kaykhtaj `ObjectMapper`. Ktebt:
```java
import com.fasterxml.jackson.databind.ObjectMapper;   // ❌
```
**Kay-compili bla hta warning**, w mn be3d l-app kat-fshel f l-démarrage:
```
No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available
```

L-sabab, mn `dependency:tree`:
```
spring-boot-starter-jackson
  \- tools.jackson.core:jackson-databind:3.1.6          ← Jackson 3, hada li Boot kay-configuri
     +- com.fasterxml.jackson.core:jackson-annotations:2.21
springdoc-openapi-starter-webmvc-ui
  \- com.fasterxml.jackson.core:jackson-databind:2.21.6  ← Jackson 2, jay ghir transitivement
```

Jackson **2** baqi f l-classpath (springdoc kay-jibo), 3lahekka l-class kayna w
l-import kay-compili. Walakin **Boot kay-registri bean dyal Jackson 3**:
`tools.jackson.databind.ObjectMapper`.

| | Jackson 2 | **Jackson 3 (Boot 4)** |
|---|---|---|
| Mapper | `com.fasterxml.jackson.databind.ObjectMapper` | **`tools.jackson.databind.ObjectMapper`** |
| Annotations | `com.fasterxml.jackson.annotation.*` | **nafs l-haja** ✅ |
| `writeValue` kay-rmi | `IOException` (checked) | `JacksonException` (**unchecked**) |

🔑 **3lahekka f `ErrorResponse.java` l-import `com.fasterxml.jackson.annotation.JsonInclude`
howa s7i7** — Jackson 3 baqi kay9ra l-annotations mn dak l-package. Ghir l-**mapper**
li tbedel. Had l-khalt sahel bzzaf.

**L-9a3ida:** ila l-import kaybda b `com.fasterxml.jackson.databind` → **ghalet**.
`com.fasterxml.jackson.annotation` → s7i7.

### (b) `AccessDeniedHandler` machi fin kan-tsenna

```java
import org.springframework.security.web.AccessDeniedHandler;        // ❌
import org.springframework.security.web.access.AccessDeniedHandler; // ✅
```
(`AuthenticationEntryPoint` **howa** f `...security.web`. L-juj mkhalfin.)

### Kifash t-lqa l-jawab wa7dek

Had l-tari9a khassek t3refha — Boot 4.2.0-M2 **milestone**, l-docs online b9aw mora:

```bash
# fen kayn had l-class?
JAR=~/.m2/repository/org/springframework/security/spring-security-web/7.2.0-M2/spring-security-web-7.2.0-M2.jar
unzip -l "$JAR" | grep AccessDeniedHandler

# ashmen méthodes 3ndha fe3lan?
javap -cp "$JAR" org.springframework.security.web.access.AccessDeniedHandler

# shkun kay-jib had l-library?
./mvnw dependency:tree | grep -i jackson
```

**L-7a9i9a kat-ji mn l-jar, machi mn l-blog.**

---

## 4. `SecurityConfig` — l-rule b l-rule

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
```

**`@EnableMethodSecurity`** kay-khelli `@PreAuthorize("hasRole('ADMIN')")` ykhdem
3la l-méthodes. **Had l-annotation hiya li kat-khelli handler 8 f step 2
(`AccessDeniedException`) y3ni shi haja** — bla-ha, dak l-handler 3omro ma kay-t-3eyyet.

### `cors(...)`
Chouf section 7.

### `csrf(AbstractHttpConfigurer::disable)`

🔑 **3lash msmo7 n-désactiviha hna** — w had l-su'al kayji f l-eval:

CSRF kay-protegi mn l-7ala fin l-browser kay-7ett l-credentials **wahdo**:
cookie dyal session. Site khayb kay-7ett form makhfi:
```html
<form action="https://skillswap.com/api/users/me" method="post">
```
L-browser kay-sedd l-cookie m3ah automatiquement → l-request kay-nje7.

**Hna l-identité kat-ji mn `Authorization: Bearer`** — w form mn site akhor
**ma yqdar-sh** y-setti dak l-header. Ya3ni l-token rasso proof dyal niyya.

⚠️ **Walakin:** ila l-refresh token (V10 3ndo table) mcha f **cookie** mn be3d,
**CSRF khasso yrja3** l-dak l-endpoint. Ktebto f l-comment f l-code bach ma t-tnsa.

### `sessionManagement(STATELESS)`

Bla hadi, Spring kay-saweb `HttpSession` f kol request. Juj mushkil:
1. L-backend kay-wqef ykun **scalable horizontalement** — l-session f l-mémoire
   dyal instance wa7ed
2. Session = cookie = w dak l-wa9t **CSRF kay-welli lazem**

### `authorizeHttpRequests` — l-ordre kay-3ewwel

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/users").permitAll()
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

⚠️ **L-ewwel match kay-rbe7.** Ila 7ettit `.anyRequest().authenticated()`
f l-loul, kolshi kay-welli protégé w l-login rasso mustahil.

| Rule | 3lash |
|---|---|
| `/api/auth/**` | bla-ha hta wahd ma yqdar y-7essel 3la token — l-login rasso protégé |
| `/oauth2/**`, `/login/oauth2/**` | l-redirects dyal Spring Security OAuth2 (step 6) |
| **`POST` 3la `/api/users` ghir** | registration. **Machi** `/api/users/**` — dakshi kay-ftah l-profils dyal kolshi. Scoped l-l-méthode w l-path b exactement |
| springdoc paths | bla-hom, `/swagger-ui` kay-rejje3 401 w l-docs bla ma3na |
| **`/actuator/health` ghir** | ⚠️ `/actuator/**` kay-kchef `/actuator/env`, `/configprops`, `/beans` — **l-secrets w l-config kamla**. Hadi faille mashhura |
| `/api/admin/**` | module Advanced permissions (2 pts) |
| `.anyRequest().authenticated()` | **default deny**. Endpoint jdid protégé wahdo hta ila chi wahd fte7 lih b 9asd |

### `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`

L-filter dyalna khasso yjri **qbel** l-authentication filters dyal Spring, bach
l-`SecurityContext` ykun 3amer melli l-authorization kat-9arrer.

### `httpBasic(disable)` + `formLogin(disable)`

L-security starter kay-sheddhom **wahdo**. Bla had l-juj star:
- `formLogin` kay-redirecti l-clients dyal l-API l-**page HTML** dyal login
- `httpBasic` kay-fte7 **dialog dyal l-browser** 3la 401

L-juj ma 3ndhom-sh blasa f API dyal tokens, w kay-kherbqu l-frontend.

### `PasswordEncoder`

```java
return new BCryptPasswordEncoder();
```
BCrypt kay-génére salt random f kol hash w kay-khezzno **dakhel** l-output:
`$2a$10$<22-char salt><31-char hash>`.

🔑 **3lahekka V10 ma 3ndha-sh column `salt`** — su'al mutawaqqa3 f l-eval.

L-work factor khellito 10 (l-default). Rafdo = l-hashing **w kol login** kay-wellu
bati b 9asd; décision dyal tuning, machi default li katrfe3 3la 9ed.

---

## 5. 🔑 `RestAuthErrorHandler` — 3lash kayn

**Had l-class kay-sedd l-7ofra li shre7na f step 2 section 5.**

L-filters dyal Spring Security kay-jriw **qbel** l-DispatcherServlet, w
`@RestControllerAdvice` kay-shuf ghir ash kay-wsel l-DispatcherServlet. Ya3ni
l-401 w l-403 — **l-erreurs li l-clients kay-shufuhom ktar mn kolshi** — ma
kay-wslu-sh l-`GlobalExceptionHandler`.

Bla had l-class, Spring kay-rejje3 l-body dyalo (wla body khawi), w l-wa3d dyal
"shape wehda" kay-therres f dak l-blasa bzzaf.

**Class wehda kat-implementi l-juj interfaces:**
```java
implements AuthenticationEntryPoint, AccessDeniedHandler
```
Bach l-juj bodies ma yqadro-sh y-tfar9u. Juj classes = nhar wa7ed shi wahd kay-bdel
wa7da w kay-nsa l-okhra.

**L-output l-7a9i9i mn l-test:**
```json
{"timestamp":"2026-11-08T15:51:32.451266Z","status":401,
 "code":"UNAUTHENTICATED","message":"Authentication is required.","path":"/api/probe/me"}

{"timestamp":"2026-11-08T15:51:32.439450Z","status":403,
 "code":"FORBIDDEN","message":"You are not allowed to perform this action.","path":"/api/admin/probe"}
```
**Nafs l-shape bzzaf** b7al l-404 w l-409 dyal step 2. `fieldErrors` ghab
(`@JsonInclude(NON_NULL)`).

### Far9 dqiq: `AccessDeniedException` jay mn juj blasat

| Mnin | Kay-wsel l-...? |
|---|---|
| `.requestMatchers("/api/admin/**").hasRole("ADMIN")` (chain) | `RestAuthErrorHandler.handle` |
| `@PreAuthorize("hasRole('ADMIN')")` (méthode) | `GlobalExceptionHandler` handler 8 |

**L-juj khassom ykunu mawjudin.** Machi redondance — juj chemins mkhalfin
l-nafs l-exception.

---

## 6. `JwtAuthenticationFilter` + `TokenParser` — l-inversion

### L-mushkil

L-filter f `common/security` kaykhtaj y-verifi token. L-code dyal l-JWT ghadi ykun
f `auth/service/TokenService` (step 5). Ila `common` importa `auth`:

```
common ──► auth      ❌ l-dependency f l-direction l-ghalta
```

`common` howa l-base layer — kolshi kay-importi mnu. Ila howa kay-importi mn
feature package, l-architecture t-9elbet.

### L-7al: l-interface f l-asfel

```java
// common/security/TokenParser.java
public interface TokenParser {
    Optional<AuthenticatedUser> parse(String token);
}
```
```
common/security/TokenParser  ◄──── auth/service/TokenService implements
```

`common` kay-3ref l-**interface** ghir. `auth` kay-3ti l-implementation.
Hadi **Dependency Inversion** — w jawab mzian f l-eval.

### Kifash l-app kat-bootiya daba, w `auth/` mazal

```java
TokenParser tokenParser = tokenParsers.getIfAvailable(() -> token -> Optional.empty());
```

`ObjectProvider<TokenParser>` kay-khelli l-bean **optionnel**. Daba ma kayn hta
implementation → kol token ma kay-t-verifa-sh → kol request anonyme → endpoints
protégés kay-rejj3u 401.

🔑 **W hada s7i7**: backend li ma yqdar-sh y-verifi tokens **khasso** y-rejje3 401.
Melli step 5 yzid bean dyal `TokenService`, kay-khdem wahdo — **bla ma tbdel
hta star hna**.

Jarrebto: f l-test, stub dyal `TokenParser` (lambda f `@TestConfiguration`)
dar l-flow kamel y-khdem.

### L-filter — juj décisions mohimmin

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

⚠️ **Machi `@Component`** — w hadi trap 7a9i9iyya. Spring Boot kay-registri
**ay bean dyal type `Filter`** 3la **kol** request automatiquement. Ila kan
`@Component` **w** zedto f l-security chain → **kay-jri juj mrrat**.
3lahekka `SecurityConfig` kay-saybo b `new`.

```java
tokenParser.parse(token).ifPresent(user -> { ... });
filterChain.doFilter(request, response);
```

⚠️ **Token khayb ma kay-rmi-sh exception.** Kay-khelli l-context khawi w kay-kemmel.
3lash:
1. Endpoint **public** khasso ykemmel ykhdem melli l-browser baqi 3ndo token 9dim
   (jarrebto: `staleTokenOnAPublicEndpointStillWorks` ✅)
2. L-401 dyal endpoint protégé howa khdemt `RestAuthErrorHandler`, li kay-3ti
   l-shape l-mettafe9 3liha

Ila l-filter rma exception, kan ghadi y-tle3 500 (wla shape mkhalfa) 3iwad 401.

---

## 7. `AuthenticatedUser` w l-trap dyal `ROLE_`

```java
public Collection<? extends GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
}
```

🔴 **L-prefix `ROLE_` machi décoration**, w hada l-sabab l-ewwel li rule
kat-ban s7i7a w kat-refdi kolshi:

| F l-config | Kay-fetter 3la authority |
|---|---|
| `hasRole("ADMIN")` | **`ROLE_ADMIN`** (Spring kay-zid l-prefix) |
| `hasAuthority("ADMIN")` | `ADMIN` (bla prefix) |

Ila khzenti l-authority b7al `"ADMIN"` w ktebti `hasRole("ADMIN")`, Spring kay-fetter
3la `ROLE_ADMIN`, ma kay-lqah-sh, w kay-rejje3 **403 l-admin rasso**.

7ettit l-prefix f blasa wehda (f l-record) bach ma yt-nsa.
Jarrebto: `userRoleOnAdminPathIs403` + `adminRoleReachesAdminPath` ✅

### 3lash `Role` f `common/security` w machi f `user/entity`

Nafs l-mantiq dyal `TokenParser`: l-authorization path kaykhtajo (l-filter,
l-rules), w `common` ma yqdar-sh y-dependi 3la feature package.
`user/entity/User` ghadi y-importih **mn hna** — l-dependency kat-mshi f
direction wehda.

### 3lash `record` w machi l-entity `User`

`AuthenticatedUser` fih **ghir** ash l-token kay-7mel w ash l-authorization
kaykhtaj: `id`, `username`, `role`. Bla email, bla password hash, w
**bla hta query l-DB f kol request**.

Ila 7ettina l-entity `User` b7al principal, kol request kan ghadi y-dir `SELECT`
3la `users` — w l-JWT kay-wqef ykun stateless.

---

## 8. `CurrentUser` — l-trap dyal l-anonymous

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication == null || !authentication.isAuthenticated()) {
    return Optional.empty();
}
return authentication.getPrincipal() instanceof AuthenticatedUser user
        ? Optional.of(user) : Optional.empty();
```

⚠️ **`isAuthenticated()` ma kafi-sh.** Spring kay-7ett
`AnonymousAuthenticationToken` f l-requests ghir m-identifyin, w dak l-object
kay-rejje3 **`isAuthenticated() == true`** b principal = l-String `"anonymousUser"`.

Ila wtheqt f `isAuthenticated()` ghir, l-cast l-`AuthenticatedUser` kay-rmi
`ClassCastException` → **500** 3iwad 401.

L-`instanceof` pattern (Java 16+) kay-7ell l-juj f star wehda: kay-check l-type
w kay-declari l-variable.

**3 méthodes:**

| | Imta |
|---|---|
| `currentUserId()` | 99% dyal l-7alat — Retahri w Nisrine kay-sta3mlu hadi |
| `currentUser()` | melli t-7taj l-role wla l-username 7ta |
| `findCurrentUser()` | endpoints li kay-khedmu l-anonymous w l-connecté bjuj |

---

## 9. `CorsConfig` — 3lash kayn

L-frontend Next.js f `localhost:3000`, l-backend f `localhost:8080`. **Origins
mkhalfin** → bla CORS, kol fetch mn l-browser kay-fshel f l-preflight w
l-console kay-t3emmer b erreurs.

`project_context.md` section 8: *"Zero warnings/errors in the browser console."*

```java
configuration.setAllowedOrigins(allowedOrigins);   // mn property, 3omro machi "*"
configuration.setAllowCredentials(true);
```

🔴 **`"*"` + `allowCredentials(true)` = illégal.** L-spec dyal CORS kay-men3ha
w l-browser kay-refdi l-response kamla. Ila ktebti `"*"` w l-credentials on,
ghadi tshuf erreur f l-console w ma t3ref 3lash.

3lahekka l-origins kay-jiw mn property:
```properties
app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}
```
W f `.env.example`. Default = localhost:3000 bach l-dev y-khdem bla config.

**`setMaxAge(3600)`** — kay-cachi l-jawab dyal l-preflight. Bla-h, l-browser
kay-sedd `OPTIONS` **qbel kol request** — double l-latence.

**`setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-Key"))`** —
`X-API-Key` 7ettito daba bach step 7 ma y-7tajsh y-bedel had l-file 3awtani.

---

## 10. `OpenApiConfig`

```java
.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
.components(new Components().addSecuritySchemes("bearerAuth",
        new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")))
```

Bla had l-bean, Swagger UI **ma kay-seddsh** header `Authorization`, w kol
endpoint protégé kay-rejje3 401 — l-docs kay-wellu bla hta fa'ida.

**`addSecurityItem`** kay-applika l-scheme 3la **kol** operation, 3lahekka ghir
l-endpoints l-publics khassom y-opt-out 3iwad kol endpoint protégé y-opt-in.

L-scheme `apiKeyAuth` dyal `publicapi/` jay f step 7 m3a l-filter.

---

## 11. Ash mverifi fe3lan

Ktebt test temporaire (9 tests) b stub dyal `TokenParser`, jrit-o, w msa7to.

| Jarrebt | Natija |
|---|---|
| Endpoint protégé bla token | ✅ 401, `code=UNAUTHENTICATED`, shape mettafe9 3liha |
| Token khayb (`Bearer not-a-real-token`) | ✅ 401 — **ma tle3-sh 500** |
| Token s7i7 | ✅ 200, w `CurrentUser.currentUserId()` rejje3 l-UUID s7i7 |
| `Role.USER` 3la `/api/admin/**` | ✅ 403, `code=FORBIDDEN` |
| `Role.ADMIN` 3la `/api/admin/**` | ✅ 200 — l-prefix `ROLE_` kay-khdem |
| `/api/auth/**` bla token | ✅ 200 |
| Token 9dim 3la endpoint public | ✅ 200 — l-filter ma 9te3-sh l-chain |
| `POST` bla CSRF token | ✅ 401 (machi 403) — CSRF fe3lan désactivé |
| `/v3/api-docs` + `/actuator/health` | ✅ 200 |

9/9.

Bghit l-tests permanents? Goulia — ghadi ydiru f
`src/test/java/com/trendenden/backend/common/security/`.

### ⚠️ Note 3la Testcontainers — l-diagnostic l-s7i7

Jarrebt n-sta3mel Testcontainers l-l-test, w fshel:
```
Could not find a valid Docker environment
```

**L-ewwel takhmin dyali kan ghalet.** Golt belli l-mushkil f l-path dyal l-socket
(`/var/run/docker.sock` vs `~/.docker/run/docker.sock`). Melli jarrebt:

```bash
ls -la /var/run/docker.sock
# lrwxr-xr-x  root  daemon  →  /Users/ader/.docker/run/docker.sock   ← kayn!
```

L-symlink **kayn**, w Testcontainers fe3lan **kay-wsel** l-socket. L-mushkil f blasa khra.

**L-sabab l-7a9i9i**, mn l-log l-kamel:
```
UnixSocketClientProviderStrategy:    failed with BadRequestException (Status 400 ...)
DockerDesktopClientProviderStrategy: failed with BadRequestException (Status 400 ...)
```

L-juj strategies connectaw, w mn be3d `GET /info` rejje3 **HTTP 400** b payload khawi
(`"ServerVersion":""`), 3lahekka `isDockerAvailable()` rejje3 `false`.

```bash
docker version --format '{{.Server.APIVersion}} / {{.Server.MinAPIVersion}}'
# 1.55 / 1.40
```

L-daemon howa **Docker 29.6.1** b **`MinAPIVersion 1.40`**. L-CLI kay-khdem 7it
kay-négocia l-version; l-client `docker-java` dyal Testcontainers 1.21.3 kay-tleb
version a9dem mn 1.40 → l-daemon kay-refdha.

Jarrebt `DOCKER_API_VERSION=1.41` → **nafs l-400**. W 1.21.3 howa l-jdid f Maven
Central (2026-11-08), 3lahekka ma kayn version n-rfe3 3liha.

**Natija:** incompatibilité **upstream** bin Testcontainers w Docker 29, machi
ghalta f l-config. Ma 3ndna hta haja n-sle7uha f l-repo.

**Daba:** l-tests kay-khdemu 3la l-Postgres dyal Compose
(`application.properties` 3ndo defaults li kay-matchiw `.env.example`):

```bash
docker compose up -d        # ⚠️ qbel kol ./mvnw test
cd backend && ./mvnw test
```

W hadi machi 7al mo'aqqat — `project_context.md` step 6 kaygol *"Use Testcontainers
**or the Compose database**"*, 3lahekka hadi wa7da mn l-juj khiyarat l-msmo7a.

**Imta ghadi t-welli mushkil:** melli n-saybu CI, 7it CI ma 3ndo-sh Compose.
L-7al dak l-wa9t: service `postgres` f l-config dyal CI, b nafs l-`POSTGRES_*`.

### 📌 L-dars l-3am

Ktebt f l-doc takhmin ("l-socket path") qbel ma n-jarrbo, w kan **ghalet**.
L-tari9a s7i7a: **t9ra l-log l-kamel w t-jarreb l-claim**. Had l-7wayj
li 3awnuni:

```bash
ls -la /var/run/docker.sock              # wash l-socket kayn fe3lan?
grep -A12 'Attempted configurations' log # ash jarreb TC b exactement?
docker version --format '{{.Server.MinAPIVersion}}'
```

---

## 12. Shared files li bdelt

| File | Ash zedt |
|---|---|
| `application.properties` | `app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}` |
| `.env.example` | `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000` |

`pom.xml` **ma bdelto-sh** f step 3 (baqi ghir l-`groupId` fix).

---

## 13. As'ila momkin f l-eval

**S: 3lash CSRF désactivé? Machi hadi faille?**
J: la, 7it ma kay-sta3mel-sh cookies. CSRF kay-protegi melli l-browser kay-7ett
l-credentials wahdo — cookie dyal session. Form mn site akhor ma yqdar-sh
y-setti header `Authorization`, 3lahekka l-token rasso proof dyal niyya.
Ila l-refresh token mcha f cookie, CSRF khasso yrja3 l-dak l-endpoint.

**S: 3lash `STATELESS`?**
J: l-token kay-7mel l-identité, 3lahekka ma khassna-sh session. Bla had l-setting
Spring kay-saweb `HttpSession` f kol request: l-backend kay-wqef ykun scalable
(session f l-mémoire dyal instance wa7ed), w kay-rje3 l-cookie — w dak l-wa9t
CSRF kay-welli lazem.

**S: 3lash `/actuator/health` w machi `/actuator/**`?**
J: `/actuator/**` kay-kchef `/actuator/env`, `/configprops` w `/beans`, li fihom
l-configuration kamla w l-secrets. `health` ghir li khassna l-l-liveness.

**S: Ash l-far9 bin `hasRole` w `hasAuthority`?**
J: `hasRole("ADMIN")` kay-fetter 3la authority smiyytha `ROLE_ADMIN`;
`hasAuthority("ADMIN")` kay-fetter 3la `ADMIN`. 3lahekka l-record kay-bni
`"ROLE_" + role.name()`. Khalt binathom = 403 l-admin rasso.

**S: L-401 dyal token khayb fin kay-tsaweb?**
J: f `RestAuthErrorHandler`, machi f `GlobalExceptionHandler`. L-filters dyal
Spring Security kay-jriw qbel l-DispatcherServlet, w `@RestControllerAdvice`
kay-shuf ghir ash kay-wsel-lo. L-juj kay-kitbu nafs l-`ErrorResponse`.

**S: 3lash `TokenParser` f `common` w l-implementation f `auth`?**
J: Dependency Inversion. `common` howa l-base layer — ila importa `auth`,
l-dependency kat-mshi f l-direction l-ghalta. L-interface f l-asfel, l-implementation
f l-fo9. Zid: Nisrine kat-sta3mel nafs l-interface f l-WebSocket CONNECT.

**S: L-app kat-bootiya daba w `auth/` mazal ma kayn?**
J: ah. `ObjectProvider<TokenParser>` kay-khelli l-bean optionnel, w l-fallback
(`token -> Optional.empty()`) kay-khelli kol request anonyme → endpoints protégés
kay-rejj3u 401. W hada s7i7: backend li ma yqdar-sh y-verifi tokens khasso
y-rejje3 401. Melli step 5 y-zid l-bean, kay-khdem wahdo.

**S: 3lash l-filter machi `@Component`?**
J: Spring Boot kay-registri ay bean dyal type `Filter` 3la kol request
automatiquement. `@Component` + zidto f l-security chain = kay-jri juj mrrat.
`SecurityConfig` kay-saybo b `new`.

---

## 14. Ash jay: step 4 (`user/`)

| File | Note |
|---|---|
| `entity/User.java` | ⚠️ `display_name` **NOT NULL** — 3emmerha mn l-username |
| `repository/UserRepository.java` | `findByEmail`, `existsByUsername` |
| `dto/RegisterRequest.java` | `@NotBlank @Email @Size` — l-validation handler dyal step 2 kay-stennaha |
| `dto/UserResponse.java` | 3omrek ma trejje3 entity |
| `service/UserService.java` | ⚠️ `toLowerCase(Locale.ROOT)` — l-CHECK f V10 kay-forci |
| `controller/UserController.java` | `POST /api/users` déjà `permitAll` f `SecurityConfig` |

**L-`Role` jay mn `common/security`**, machi mn `user/entity`.

W l-7wayj li khassek tgoulhom l-l-équipe daba:
1. **`UUID currentUserId()`**, machi `Long` — qbel ma ybdaw ykitbu services
2. L-import dyal Jackson: `tools.jackson.databind.ObjectMapper` f Boot 4
3. Khassom `docker compose up -d` qbel `./mvnw test`
