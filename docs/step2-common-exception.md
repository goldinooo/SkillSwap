# Step 2 — `common/exception/`

> Owner: **Risattou**
> Package: `backend/src/main/java/com/trendenden/backend/common/exception/`
> 8 files · compiled ✅ · 6 tests passed ✅

`project_context.md` section 9: *"Backend errors use one standard error response from the
global exception handler."* Had l-package howa dak l-"one standard". Retahri w Nisrine
ghadi y-throwiw mnu, 3lahekka khasso ykun s7i7 mn l-bdaya.

---

## 0. L-flow: mnin kaybda l-erreur w fin kaysali

```
Service kay-throwi NotFoundException("USER_NOT_FOUND", "...")
          │
          │  (RuntimeException → @Transactional kay-rolbacki)
          ▼
Controller ma kay-9betha-sh  ──►  DispatcherServlet
          │
          ▼
GlobalExceptionHandler.handleApiException(ex, request)
          │
          ▼
ErrorResponse(timestamp, 404, "USER_NOT_FOUND", "...", "/api/users/42", null)
          │
          │  Jackson kay-serializi
          ▼
HTTP 404 + JSON
```

L-fikra l-asasiyya: **l-service ma kaybni-sh `ResponseEntity`**. Kay-throwi exception
w kay-nsa. Blasa wehda f l-app kat-9arrer kifash l-erreur katban.

---

## 1. `ErrorResponse.java` — star b star

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
```

**`record`** — feature dyal Java 16+. Had star wehda kat3ti-k:
- constructeur b l-6 paramètres
- accessors: `timestamp()`, `status()`, ... (**bla `get`**, hadi convention dyal records)
- `equals()`, `hashCode()`, `toString()`
- kol field **`final`** — immutable

B `class` 3adiyya khassek ~60 star. W l-immutabilité mohimma hna: DTO dyal response
3omro ma khasso ytbedel mn be3d ma yt-saweb.

**`@JsonInclude(JsonInclude.Include.NON_NULL)`** — annotation dyal Jackson.
Bla biha, kol erreur kat-khrej hakka:
```json
{"timestamp":"...","status":404,"code":"USER_NOT_FOUND","message":"...",
 "path":"...","fieldErrors":null}
```
`"fieldErrors":null` zaid f kol 404 w kol 500. B l-annotation, Jackson kay-skippi
kol field li `null`. L-output l-7a9i9i (mn l-test):
```json
{"timestamp":"2026-11-05T17:35:18.915402Z","status":404,
 "code":"USER_NOT_FOUND","message":"No user with id 42.","path":"/probe/notfound"}
```
Shuf: `fieldErrors` ghab 3la 9ed.

**`Instant timestamp`** — machi `LocalDateTime`, machi `Date`.
`Instant` = nuqta f l-waqt b UTC, bla timezone. Jackson kay-serializiha ISO-8601 b `Z`
f l-ekhher (`2026-11-05T17:35:18.915402Z`). `LocalDateTime` ma 3ndo-sh timezone →
ila l-server f Casablanca w l-client f Tokyo, ma kayn tari9a t3ref shnu l-waqt l-7a9i9i.
Nafs l-mantiq dyal `TIMESTAMPTZ` f V10.

**`int status`** — l-code HTTP b raqm (404, 409...). Mkerrer m3a l-status dyal l-response
7it l-frontend 3ndo l-body bo7do f ba3d l-mawa9i3 (logs, error toasts) w ma kay-wsel-sh
l-l-status line.

**`String code`** w **`String message`** — **had l-far9 howa l-moham f had l-file.**

| | `code` | `message` |
|---|---|---|
| L-shkun | l-machine (frontend) | l-bnadem (dev, logs) |
| Mathal | `USER_NOT_FOUND` | `"No user with id 42."` |
| Stable? | **ah**, contrat | la, momkin ytbedel ay wa9t |
| Mtarjam? | la, kaybqa ASCII | la |
| L-frontend kaydir biha? | `switch (code)` | kay-werriha f dev ghir |

🔴 **3lash hadi mohimma l-had l-project b l-khusus:** `project_context.md` section 3
kay-claimi module **"Multiple languages (3)"** (1 pt). L-frontend khasso y-tarjem l-erreurs
l-3 lughat. Ila l-frontend kay-9ra `message` bach y3ref ash wqe3, l-i18n **ma t-khdem-sh** —
l-English kayji mn l-backend hard-coded. B `code`, l-frontend 3ndo:
```js
const t = { USER_NOT_FOUND: { ar: "...", fr: "...", en: "..." } }
```
W l-backend ma 3ndo 3la9a b l-lugha.

**`String path`** — l-URI dyal l-request. F l-logs, w f l-bug reports: user kay-sedd
screenshot dyal l-erreur w nta 3aref sh-men endpoint.

**`List<FieldError> fieldErrors`** — nullable. Kay-t3emmer ghir f l-validation.

```java
    public record FieldError(String field, String message) {
    }
```

**Record nested** dakhel l-record. 3lash dakhel: `FieldError` ma 3ndha hta ma3na bra
`ErrorResponse`. Zid 3la hadchi, kayn **`org.springframework.validation.FieldError`**
dyal Spring b nafs l-isem. Melli hiya nested, kat-3eyyet-lha `ErrorResponse.FieldError`
w **l-ambiguïté kat-mshi** — ma khassek-sh alias wla import mkerrer f l-handler.

```java
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path, null);
    }
```

**`static` factory** — 3lash machi `new ErrorResponse(...)` direct f l-handler?

1. `Instant.now()` kay-tsawweb f blasa wehda. Bla hadi, 9 handlers kol wahd kaykteb
   `Instant.now()` — w nhar wa7ed shi wahd kaynsa.
2. L-`null` dyal `fieldErrors` makhfi. L-handler kaykteb
   `ErrorResponse.of(404, "X", "msg", path)` — ndif.
3. Juj overloads = juj 7alat ghir: b fieldErrors w bla.

---

## 2. `ApiException.java` — star b star

```java
public class ApiException extends RuntimeException {
```

**`extends RuntimeException` w machi `Exception`** — had l-9rar fih juj asbab,
w l-tani khatir:

**(a) Checked vs unchecked.** B `Exception`, kol méthode f l-chaîne khassha
`throws ApiException`:
```java
// b Exception — kol star kaykhtaj throws
public User find(Long id) throws ApiException { ... }
public UserResponse profile(Long id) throws ApiException { ... }
public ResponseEntity<?> get(Long id) throws ApiException { ... }
```
Dghya kaywelli bruit bla ma3na.

**(b) 🔴 Rollback dyal `@Transactional`.** Spring **by default** kay-rolbacki ghir
3la `RuntimeException` w `Error`. 3la exception **checked**, l-transaction
**kat-commiti** 3la 9ed l-khruj.

Tsawwer l-code dyal Retahri:
```java
@Transactional
public void spend(Long userId, Long sessionId) {
    int rows = userRepo.decrementCredits(userId);   // credits -= 1 ✅
    if (rows == 0) {
        throw new InsufficientCreditsException();   // ila kanet checked...
    }
    ledgerRepo.save(...);
}
```
Ila kanet `InsufficientCreditsException` **checked**, l-`decrementCredits` kay-commiti,
w l-ledger row ma kat-tkteb-sh. **L-balance w l-ledger kay-tfar9u** — w `project_context.md`
section 8 kaygol *"Balance must equal the ledger sum"*. Data corruption, w l-module
dyal l-credits = 0 points.

B `RuntimeException`, l-exception kay-rolbacki kolshi. 3lahekka had l-star wehda
`extends RuntimeException` kat-protegi l-logic dyal Retahri bla ma howa ydir hta haja.

> **Note:** momkin t-forci b `@Transactional(rollbackFor = Exception.class)`.
> Walakin dakshi kaykhelli l-9a3ida f kol annotation, w kafi tensa mrra wehda.
> Unchecked = sa7i7 by default.

```java
    private final HttpStatus status;
    private final String code;
```

**`private final`** — `final` = ma yt-bedel-sh mn be3d l-constructeur. Exception khassha
tkun immutable: ila chi code bdel l-`status` f nos l-chaîne, l-handler kay-rejje3
haja mkhalfa 3la li t-throwat.

**`HttpStatus`** (enum dyal Spring) w machi `int` — l-compilateur kay-men3
`new ApiException(999, ...)`.

```java
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
```

**`super(message)`** — kay-3eyyet l-constructeur dyal `RuntimeException`, li kay-khzen
l-message f `Throwable`. 3lahekka `ex.getMessage()` kay-khdem bla ma nktbu field.

```java
    public ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
```

**Overload b `cause`** — kay-7afed 3la l-**chaîne** dyal l-exceptions:
```java
try {
    return objectMapper.readValue(payload, Dto.class);
} catch (JsonProcessingException e) {
    throw new BadRequestException("MALFORMED_PAYLOAD", "Bad payload.", e);
}
```
F l-log ghadi tshuf `Caused by: JsonProcessingException: ...` m3a l-stack trace l-asliyya.
Bla `cause`, l-exception l-asliyya kat-dya3 w l-debug kaywelli mustahil.

```java
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
```

Hna **`getX()`** w machi `x()` b7al f l-record — 7it hadi `class` 3adiyya w
l-convention JavaBean hiya `getX`. Cohérence m3a l-ba9i dyal Spring.

> **Optimisation li ma dert-sh:** `super(message, cause, false, false)` kay-désactivi
> l-capture dyal l-stack trace. L-exceptions dyalna "métier" (user ma kayn, email pris)
> kayn b7al control flow — l-stack trace dyalhom ma kay-khdem hta wahd, w l-capture
> dyalha ghalya. Ila l-profiling werra belli hadi mushkil, hadi l-7al. Daba
> l-stack trace kay-3awen f l-debug 3la l-production.

---

## 3. L-5 subclasses

Kolhom nafs l-shakl, 5 star kol wahd:

```java
public class NotFoundException extends ApiException {
    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
```

| Class | Status | Imta |
|---|---|---|
| `BadRequestException` | 400 | request m-fahma walakin kat-telleb haja mustahila: code TOTP ghalet, transition machi msmo7a |
| `UnauthorizedException` | 401 | ma t3arref-sh: bla token, token expiré, password ghalet |
| `ForbiddenException` | 403 | t3arref walakin machi msmo7 lih: USER 3la endpoint ADMIN |
| `NotFoundException` | 404 | l-resource ma kayn-sh |
| `ConflictException` | 409 | kay-t3arek m3a data mawjuda: email déjà pris |

**3lash subclasses w machi `new ApiException(HttpStatus.NOT_FOUND, ...)` direct?**
Liqra'a: `throw new NotFoundException(...)` kat-fhem mn ghir ma t9ra l-paramètres.
W `ApiException` **machi `abstract`**, 3lahekka Retahri baqi yqder y-throwiha direct
l-7ala khassa:
```java
throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SESSION_NOT_CONFIRMED", "...");
```

### 🔒 Juj notes dyal sécurité f l-javadoc, machi décoration

**F `UnauthorizedException`:**
> *"Use one single message for 'no such email' and 'wrong password'."*

Ila rejje3ti `EMAIL_NOT_FOUND` l-email machi msejjel w `WRONG_PASSWORD` l-password ghalet,
3titi l-attaquant **account-enumeration oracle**: kay-jarreb 10000 email b password
khawi, w kolla `WRONG_PASSWORD` kaygoul-lo "had l-email msejjel f SkillSwap".
Mn be3d kay-sta3mel dak l-lista f credential-stuffing wla phishing.
**L-juj 7alat khassom y-rejj3u `INVALID_CREDENTIALS` b nafs l-message.**

**F `NotFoundException`:**
> *"answering 403 for 'exists but not yours' and 404 for 'does not exist' lets a caller probe which ids are real."*

Nafs l-mantiq b IDs. Ila `GET /api/sessions/5` kay-rejje3 403 (kayna machi dyalk)
w `GET /api/sessions/9999` kay-rejje3 404 (ma kayna-sh), l-attaquant kay-mappi
kol l-sessions f l-app. F l-endpoints li kat-mess data dyal l-akhorin, **404 l-juj**.

---

## 4. `GlobalExceptionHandler.java` — handler b handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
```

**`@RestControllerAdvice`** = `@ControllerAdvice` + `@ResponseBody`.
- `@ControllerAdvice` = `@Component` li Spring kay-applikih 3la **kol** l-controllers
- `@ResponseBody` = l-return value kay-tserializa l-JSON, machi kay-t9elleb 3la view

Bla l-`@ResponseBody` (ya3ni b `@ControllerAdvice` ghir), Spring ghadi y-fetter 3la
template HTML b l-isem dyal l-return → 500.

```java
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
```

**`private static final`** — `static` 7it logger wa7ed l-kol l-class
(machi wa7ed l-kol instance; w hna l-bean singleton ay7al). `final` 7it ma kayt-bedel.
Hadi l-idiome standard dyal SLF4J.

### Handler 1 — `ApiException`

```java
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(
                ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }
```

**`@ExceptionHandler(ApiException.class)`** — "melli ay exception mn had l-type (wla
subclass dyalo) ykhrej mn controller, 3eyyet had l-méthode".

🔑 **`ApiException.class` kay-9bet l-5 subclasses kollhom.** Hadi l-fikra dyal
l-hiérarchie: handler wa7ed, 5 types. Melli Retahri yzid `InsufficientCreditsException
extends ApiException`, **kat-khdem bla ma nzidu hta star hna.**

**`HttpServletRequest request`** — Spring kay-injectih f l-paramètres. Ma kayn-sh
`@Autowired` 7it `@ExceptionHandler` kay-supporti majmo3a mn l-types automatiquement.

**`request.getRequestURI()`** — `/api/users/42`. ⚠️ Machi `getRequestURL()`
(li kay-3ti URL kamla b l-host) w machi `getQueryString()` — l-query params ymken
ykunu fihom tokens wla data sensible, w had l-string ghadi t-kteb f l-logs.

**`ex.getStatus().value()`** — `HttpStatus.NOT_FOUND` → `404` (int). Kat-mla
l-field `status` f l-record.

### Handler 2 — validation dyal l-body

```java
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
```

Had l-exception kay-rmiha Spring melli `@Valid @RequestBody` kay-fshel. Mathal step 4:
```java
public record RegisterRequest(
    @NotBlank @Size(min=3, max=30) String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min=8) String password) {}
```

**`ex.getBindingResult()`** — object dyal Spring fih kol l-akhta' dyal l-binding.
**`.getFieldErrors()`** — `List<org.springframework.validation.FieldError>`
(**l-wehda dyal Spring**, machi dyalna).

**`.getField()`** → `"username"`
**`.getDefaultMessage()`** → `"must not be blank"` — mnin jay had l-texte?
Mn l-default dyal Jakarta Bean Validation. Momkin t-personnalizih:
`@NotBlank(message = "USERNAME_REQUIRED")`.

**`.stream().map(...).toList()`** — kay-converti mn l-type dyal Spring l-type dyalna.
3lash l-conversion w machi t-rejje3 l-wehda dyal Spring direct? 7it `FieldError`
dyal Spring fih ~10 field (`objectName`, `rejectedValue`, `bindingFailure`,
`codes[]`...). **`rejectedValue` fih l-value li l-user 3eta** — ya3ni ila l-user
dekhel password dai3f, l-password l-khawi kay-khrej f l-JSON dyal l-erreur w kay-kteb
f l-logs. Dyalna fih **juj fields ghir**: `field` w `message`.

**`.toList()`** — Java 16+, kay-rejje3 lista **immutable**. Qbel kan
`.collect(Collectors.toList())` (mutable, w ktar typing).

**L-output l-7a9i9i** (mn l-test, b `username=""` w `email="not-an-email"`):
```json
{"timestamp":"2026-11-05T17:35:19.055434Z","status":400,"code":"VALIDATION_FAILED",
 "message":"Request validation failed.","path":"/probe/validate",
 "fieldErrors":[{"field":"username","message":"must not be blank"},
                {"field":"email","message":"must be a well-formed email address"}]}
```
**L-juj akhta' f response wehda** — machi "jarreb 3awtani" 3la kol field.

### Handler 3 — validation dyal l-params

```java
    @ExceptionHandler(HandlerMethodValidationException.class)
        List<ErrorResponse.FieldError> fields = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorResponse.FieldError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
```

Hada l-contraintes 3la `@RequestParam` / `@PathVariable`, machi 3la l-body:
```java
@GetMapping("/api/users")
List<UserResponse> search(@RequestParam @Size(min=2) String q,
                          @RequestParam @Max(100) int limit) { ... }
```

**`flatMap` w machi `map`** — 7it l-structure nested: kol **paramètre** 3ndo
**lista** dyal akhta' (`@Size` w `@Pattern` f nafs l-param = juj akhta'). `map`
kay-3ti `Stream<List<FieldError>>`; `flatMap` kay-flattih l-`Stream<FieldError>`.

> ### 🔴 Note mohimma 3la Boot 4
>
> Hna **l-build fshel l-ewwel mrra** b:
> ```
> cannot find symbol: method getAllValidationResults()
> ```
> `getAllValidationResults()` howa l-isem f **Spring 6** (Boot 3), w hada li ghadi
> tshufo f **kol** tutorial w kol jawab f StackOverflow. F **Spring 7** (Boot 4,
> li 7na kay-sta3mluh) wellat **`getParameterValidationResults()`**.
>
> Kifash lqit l-isem s7i7 — had l-tari9a khassek t3refha, ghadi t-7tajha 3awtani
> 7it Boot 4.2.0-M2 **milestone** w l-docs b9aw mora:
> ```bash
> JAR=~/.m2/repository/org/springframework/spring-web/7.1.0-M2/spring-web-7.1.0-M2.jar
> javap -cp "$JAR" org.springframework.web.method.annotation.HandlerMethodValidationException
> ```
> `javap` kay-9ra l-jar w kay-werri l-méthodes l-7a9i9iyyin. **L-vérité kat-ji mn l-jar,
> machi mn l-blog.**

### Handler 4 — JSON khayb

```java
    @ExceptionHandler(HttpMessageNotReadableException.class)
        log.debug("Unreadable request body on {}", request.getRequestURI(), ex);
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST",
                "Request body is missing or not valid JSON.", request.getRequestURI()));
```

Melli l-body machi JSON s7i7 (`{not json`), wla khawi, wla chi value ma dkhelat f
l-type dyalha.

🔒 **3lash ma rejje3t-sh `ex.getMessage()`:** Jackson kay-3ti message b7al hada:
```
JSON parse error: Unexpected character ('n' (code 110)): was expecting double-quote...
at [Source: (String)"{not json"; line: 1, column: 3]
(through reference chain: com.trendenden.backend.user.dto.RegisterRequest["username"])
```
Had l-texte kay-kchef **l-noms dyal l-classes w l-package dyalna** w kay-redd l-input
dyal l-user. L-ewwel = information disclosure (l-attaquant kay-mappi l-structure
dyal l-app); l-tani = ila l-user sedd password f l-JSON l-khayb, l-password kay-rja3
f l-response. 3lahekka: `log.debug` l-l-dev, message générique l-l-client.

**`log.debug` w machi `log.warn`** — JSON khayb ghalibann l-client li ghalet,
machi l-backend. Ma khassha-sh t3emmer l-logs f production.

**`ResponseEntity.badRequest()`** = shortcut l-`ResponseEntity.status(400)`.

### Handler 5 — type ghalet f l-URL

```java
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
                "Parameter '" + ex.getName() + "' has the wrong type.", request.getRequestURI(),
                List.of(new ErrorResponse.FieldError(ex.getName(), "wrong type"))));
```

`GET /api/users/abc` w l-signature `@PathVariable Long id` → `"abc"` ma kay-dkhel-sh f `Long`.

**`ex.getName()`** → `"id"` (l-isem dyal l-paramètre). **Safe t-rejj3o** — isem dyal
paramètre f l-API dyalk, machi data dyal l-user.
⚠️ **Ma t-rejje3-sh `ex.getValue()`** — hadak l-input dyal l-user.

Bla had l-handler, l-fallback (handler 9) kay-9betha w kay-rejje3 **500** 3la haja
li hiya **ghalat dyal l-client**. L-monitoring kay-3eyyet 3la walo.

### Handler 6 — path ma kayn

```java
    @ExceptionHandler(NoResourceFoundException.class)
```

🔑 **Had l-handler kaynaqed l-app mn mushkil 7a9i9i.**

`NoResourceFoundException` kat-rma melli l-URL ma t-matcha hta controller
(w ma kayn-sh static file b dak l-isem). Fiha `extends ServletException` → ya3ni
**l-fallback `Exception.class` ghadi y-9betha** w y-rejje3:

- **500** 3iwad 404
- `log.error(...)` m3a stack trace kamla

Natija: kol `/favicon.ico`, kol bot li kay-skanni `/admin.php`, kol typo f URL =
**ERROR f l-logs**. F yum wa7ed f production, alaf dyal error line 3la walo,
w l-erreur l-7a9i9iyya kat-dfen fihom.

### Handler 7 — constraint dyal l-DB

```java
    @ExceptionHandler(DataIntegrityViolationException.class)
        log.warn("Database constraint rejected a write on {}", request.getRequestURI(), ex);
        ... "CONSTRAINT_VIOLATION", "The request conflicts with data that already exists."
```

**Hada howa l-chemin dyal l-race condition**, w kay-rbot direct m3a V10:

```
t=0ms   Request A: existsByEmail("x@y.com") → false
t=1ms   Request B: existsByEmail("x@y.com") → false     ← l-juj shafo "free"
t=2ms   Request A: INSERT → ✅
t=3ms   Request B: INSERT → 🔴 violates "users_email_key"
```

L-check f l-service **ma kafi-sh** — kayn window bin l-check w l-insert.
L-`UNIQUE` f V10 howa l-garantie l-7a9i9iyya, w had l-handler kay-translatiha
l-409 ndif 3iwad 500.

> `project_context.md` section 8: *"Many users at once: no race conditions, no data corruption."*
> Had l-handler + l-contraintes f V10 = kifash kay-t7a99e9 dakshi.

🔒 **`log.warn(..., ex)` walakin message générique f l-response.** L-message dyal
l-driver howa:
```
ERROR: duplicate key value violates unique constraint "users_email_key"
  Detail: Key (email)=(x@y.com) already exists.
```
Fih **l-isem dyal l-table, l-column, w l-contrainte** = l-schema dyalna f response
HTTP. L-attaquant kay-bni l-map dyal l-DB mn l-erreurs. W `Detail:` fih **l-email
dyal shi wahd akhor** = data leak dyal user tani.

**`log.warn` w machi `log.error`** — ymken normal (race, wla user kay-clicki juj mrrat)
walakin ymken 7ta bug f l-logic. `warn` = "chuf-ha ila t3awdat bzzaf".

**📌 SLF4J:** `log.warn("... {}", uri, ex)` — placeholder wa7ed `{}`, **juj** arguments.
SLF4J kay-3aref belli l-akhir `Throwable` → kay-tabe3 l-stack trace. **Ma tzid-sh
`{}` tani** l-l-exception.

### Handler 8 — `AccessDeniedException`

```java
    @ExceptionHandler(AccessDeniedException.class)
```

Hadi dyal `@PreAuthorize("hasRole('ADMIN')")` 3la méthode (l-module **Advanced
permissions**, 2 pts bonus). Chouf section 5 t7t 3la l-7udud.

### Handler 9 — l-fallback

```java
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ... "INTERNAL_ERROR", "An unexpected error occurred."
```

**3lash kaykhdem l-ordre s7i7 bla ma ndiru `@Order`:** Spring kay-khtar l-handler
**l-a9rab** f l-hiérarchie. `NotFoundException` kayn `ApiException.class` handler
(a9rab) w `Exception.class` (b3id) → kay-rbe7 l-ewwel. **Ma 3ndek-sh 3la ash t-rettib.**

**L-test l-7a9i9i** — rmina:
```java
throw new IllegalStateException("secret internal detail /Users/ader/db.sql");
```
W l-response kanet:
```json
{"status":500,"code":"INTERNAL_ERROR","message":"An unexpected error occurred."}
```
L-path dyal l-filesystem **ma khrej-sh**.

🔒 Bla had l-handler, Spring kay-rejje3 l-whitelabel error page wla `ProblemDetail`
b `trace` fiha stack trace kamla. Hadchi kay-kchef: structure dyal l-packages,
l-versions dyal l-libraries (w l-CVEs dyalhom), paths dyal l-filesystem, w ba3d
l-mrrat SQL b l-data fih.

`project_context.md` section 8: *"Zero warnings/errors in the browser console."*
Response 500 b JSON ndif ma kat-3eyyet-sh f l-console; stack trace HTML kat-3eyyet.

---

## 5. ⚠️ Ash had l-handler **ma kay-9betsh**

L-7add l-aham f had l-class, w maktub f l-javadoc:

```
Request b JWT khayb
        │
        ▼
┌──────────────────────────────┐
│ Spring Security Filter Chain │ ← JwtAuthenticationFilter (step 3)
│                              │
│  401/403 kay-tsaweb HNA      │ ──► ❌ 3omro ma kay-wsel l-@RestControllerAdvice
└──────────────────────────────┘
        │ (ila n-je7)
        ▼
┌──────────────────────────────┐
│     DispatcherServlet        │
│                              │
│   GlobalExceptionHandler ✅  │
└──────────────────────────────┘
```

L-filters dyal Spring Security kay-jriw **qbel** l-`DispatcherServlet`.
`@RestControllerAdvice` kay-khdem ghir 3la ash kay-wsel l-l-DispatcherServlet.

**Natija l-step 3:** `SecurityConfig` khasso y-configuri:
- `AuthenticationEntryPoint` → l-401 (bla token, token khayb)
- `AccessDeniedHandler` → l-403 (token s7i7, machi msmo7)

**w l-juj khassom ykitbu `ErrorResponse`.** Ila nsina, l-401 w l-403 — **l-erreurs
li l-clients kay-shufuhom ktar mn kolshi** — ghadi ykunu b shape mkhalfa, w l-wa3d
dyal "shape wehda" kay-therres f dak l-blasa bzzaf.

**Far9 dqiq:** `AccessDeniedException` mn `@PreAuthorize` 3la **méthode** kat-rma
dakhel l-DispatcherServlet (f l-AOP interceptor) → **kat-wsel** l-handler 8.
Nafs l-exception mn `.requestMatchers().hasRole()` f l-filter chain → **ma kat-wsel-sh**.
3lahekka l-juj khassom ykunu mawjudin. Hadi machi redondance.

---

## 6. Kifash kay-sta3mluha Retahri w Nisrine

```java
// Retahri, session/
public Session confirm(Long sessionId, Long learnerId) {
    Session s = sessionRepo.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "No session " + sessionId));

    if (!s.getLearnerId().equals(learnerId)) {
        throw new ForbiddenException("NOT_YOUR_SESSION", "Only the learner can confirm.");
    }
    if (s.getStatus() != REQUESTED) {
        throw new ConflictException("SESSION_NOT_REQUESTED", "Session is " + s.getStatus());
    }
    creditService.spend(learnerId, sessionId);   // kay-throwi mn ApiException 7ta howa
    ...
}
```

**Ma kayn hta `ResponseEntity`, hta `try/catch`, hta `@ResponseStatus`.** L-service
kaygoul ash fshel, w l-handler kay-translatih.

**`.orElseThrow(() -> ...)`** 3la `Optional` — hadi l-idiome li ghadi tsta3mluh ktar.

| Hom khassom | Exception |
|---|---|
| Resource ma kayn | `NotFoundException` |
| Machi dyalk | `ForbiddenException` (wla `NotFoundException`, chouf section 3) |
| State ghalet / déjà kayn | `ConflictException` |
| Input machi s7i7 | `BadRequestException` |
| Ma t3arref-sh | `UnauthorizedException` |

---

## 7. Ash mverifi fe3lan

Kteb test temporaire (6 tests) m3a controller li kay-throwi kol 7ala,
jrit-o (6/6 ✅), w msa7to — l-test l-permanent dyalk nta.

| Ash jarrebt | Natija |
|---|---|
| `NotFoundException` | 404, `code=USER_NOT_FOUND`, `fieldErrors` **ghab** (`@JsonInclude` kay-khdem) |
| `ConflictException` | 409, `code=EMAIL_ALREADY_USED` |
| `@Valid` b juj fields khaybin | 400, `VALIDATION_FAILED`, **juj** entries f `fieldErrors` |
| `{not json` | 400, `MALFORMED_REQUEST` — l-message dyal Jackson ma khrej-sh |
| `/probe/typed/abc` 3la `Long` | 400, `INVALID_PARAMETER`, `fieldErrors[0].field = "id"` |
| `DataIntegrityViolationException` b `"users_email_key"` | 409, w **l-isem dyal l-contrainte ma khrej-sh** |
| `IllegalStateException` b path dyal filesystem | 500, w **l-path ma khrej-sh** |

Bghit l-test l-permanent? Goulia w n-rje3 ndiro f
`src/test/java/com/trendenden/backend/common/exception/`.

---

## 8. As'ila momkin f l-eval

**S: 3lash `RuntimeException` w machi `Exception`?**
J: juj asbab. L-ewwel, ma kat-forci-sh `throws` f kol signature. L-tani w l-moham:
`@Transactional` kay-rolbacki by default ghir 3la unchecked. B checked, l-transaction
kat-commiti 3la 9ed l-khruj — ya3ni f l-logic dyal l-credits, l-balance kat-tbedel
w l-ledger row ma kat-tkteb-sh, w l-juj kay-tfar9u.

**S: 3lash `code` w `message` bjuj?**
J: `code` l-l-machine, `message` l-l-bnadem. Kay-claimi module i18n (3 lughat) —
l-frontend khasso y-tarjem, w ma yqdar-sh y-tarjem string English mn l-backend.
Kay-switchi 3la `code` w kay-khtar l-texte dyalo.

**S: 3lash ma kat-rejje3-sh l-message dyal l-exception f l-500?**
J: fih l-noms dyal l-classes, paths dyal l-filesystem, versions dyal l-libraries
(w l-CVEs dyalhom), w ba3d l-mrrat SQL b data dyal users akhorin. Kay-kteb f l-log
(fin l-dev kay-shufo) w ma kay-khrej-sh f l-response.

**S: 3lash `DataIntegrityViolationException` 409 w machi 500?**
J: 7it ghalibann machi bug — hiya l-race condition. Juj requests kay-dozu l-check
"email free?", w l-`UNIQUE` f V10 kay-wa99ef l-tani. Hadi 7ala mutawaqqa3a f l-code
dyalna, w 409 "conflict" howa l-ma3na l-s7i7. L-check f l-service wa7do ma kafi-sh —
kayn window bin l-check w l-INSERT.

**S: Melli ykun 3ndek 9 handlers, kifash Spring kay3ref ashmen wa7ed?**
J: kay-khtar l-a9rab f l-hiérarchie dyal l-classes. `NotFoundException` kayn
`ApiException` (a9rab) w `Exception` (b3id) → l-ewwel kay-rbe7. Ma khassni-sh
`@Order`.

**S: Had l-handler kay-9bet l-401 dyal token khayb?**
J: **La.** L-filters dyal Spring Security kay-jriw qbel l-DispatcherServlet, w
`@RestControllerAdvice` kay-shuf ghir ash kay-wsel l-DispatcherServlet. L-401 w l-403
dyal l-filter chain kay-tsawbu f `SecurityConfig` b `AuthenticationEntryPoint` w
`AccessDeniedHandler`, w l-juj khassom ykitbu nafs l-`ErrorResponse`.

---

## 9. Ash jay: step 3

`common/security/` — 8 files. W **l-ewwel haja f l-step 3** hiya dak l-7add li
shre7na f section 5: `RestAuthEntryPoint` li kaykteb `ErrorResponse`.

Juj 7wayj khassek t-confirmi m3a l-équipe qbel step 4 (`TASKS.md` kaygol ila ma dertiha,
kol wahd ghadi y-devini l-isem w ytherres f merge):

| Interface | Fin | Signature |
|---|---|---|
| `CurrentUser` | `common/security/` | `Long currentUserId()` |
| `TokenParser` | `common/security/` | `Optional<AuthenticatedUser> parse(String token)` |
| `ErrorResponse` + `ApiException` | ✅ **salat, hadi hiya** | shape wehda l-kolshi |

L-talita salat — sedd-ha-lhom daba.
