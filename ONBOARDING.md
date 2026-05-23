# Acetoy Performance Test Suite — Onboarding

A Gatling-based load test suite targeting the **Acetoy** demo e-commerce site (`https://acetoys.uk`). This document orients a new contributor (human or AI) before they make changes.

> **For quick Claude context:** see [`CLAUDE.md`](./CLAUDE.md) — terser, command-focused.
> **This doc:** explains the *why* and the *current state*, in human-readable form.

---

## 1. What this project does

- Models a realistic user journey through an e-commerce site: browse → category → product detail → cart → login → checkout → logout.
- Uses Gatling's Java DSL (3.8.4) to script the journey and Maven to run it.
- Produces HTML reports under `target/gatling/<simulation>-<timestamp>/index.html` showing response times (p50/p95/p99), throughput, and error rates.

Origin: source code for the *Advanced Gatling for Stress Testing Web Applications - Java Edition* Udemy course. Now being extended with broader performance-engineering capabilities (load/stress/spike/soak profiles, CI gates, monitoring).

---

## 2. Tech stack

| Layer | Choice | Why |
|---|---|---|
| Test framework | Gatling **3.8.4** (highcharts) | Code-first DSL, rich HTML report, high single-node throughput |
| Language | Java **17** | `maven.compiler.release=17` in `pom.xml`. JDK 8/11 toggles available but commented out. |
| Build | Maven (wrapper checked in) | `./mvnw` runs without a system Maven install |
| Runner plugin | `gatling-maven-plugin` 4.2.7 | Provides `mvn gatling:test` and `gatling:recorder` goals |

No production code is shipped — this is a `test`-scoped project; everything lives under `src/test/`.

---

## 3. Project layout

```
.
├── pom.xml                              # Maven config (Gatling deps, JDK 17 toolchain)
├── mvnw, mvnw.cmd                       # Maven wrapper (use instead of system mvn)
├── CLAUDE.md                            # Terse project guide for Claude Code
├── ONBOARDING.md                        # ← you are here
└── src/test/
    ├── java/
    │   ├── Engine.java                  # IDE entry point — calls Gatling.fromMap()
    │   ├── IDEPathHelper.java           # Resolves source/resource/result paths
    │   ├── Recorder.java                # HAR/proxy recorder entry point
    │   └── acetoy/
    │       ├── AcetoySimulation.java    # The simulation: protocol + scenario + setUp
    │       ├── pageobjects/
    │       │   ├── StaticPages.java     # Home, Our Story, Get In Touch
    │       │   ├── Category.java        # listProducts + cyclePagesOfProducts (asLongAs loop using #{categoryPages})
    │       │   ├── Customer.java        # login/logout — internally call UserSession.markLoggedIn/Out
    │       │   ├── Products.java        # viewProduct: data-driven via #{slug} + asserts #{name}
    │       │   └── Cart.java            # addToCart, viewCart (wraps doIf→login), increase/decrease/checkout
    │       └── session/
    │           └── UserSession.java     # init (flushCookieJar + customerLoggedIn=false + itemsInBasket=0 + productsListPageNumber=1) + markLoggedIn/Out
    └── resources/
        ├── gatling.conf                 # Gatling runtime config (mostly defaults)
        ├── recorder.conf                # HAR/proxy recorder config
        ├── logback-test.xml             # Log levels during simulations
        ├── data/                        # Feeders (CSV + JSON)
        │   ├── Category.csv             #   categoryName,categorySlug,categoryPages (4 categories)
        │   ├── Users.csv                #   username,password (3 users)
        │   └── ProductDetails.json      #   id,name,slug,description,categoryName,price (22 products)
        └── acetoy/acetoysimulation/     # Recorder output directory
```

---

## 4. Core design pattern: Page Objects

This is the most important thing to understand before adding features.

**Vanilla Gatling** scripts (especially recorded ones) put every HTTP request directly into the simulation class. That works for a 20-request flow but becomes unmaintainable as scenarios grow and overlap.

**This project** instead extracts each business action into a static `ChainBuilder` inside a page object class:

```java
// Cart.java — reusable building block
public static ChainBuilder addToCart(int productId) {
    return exec(
        http("Add Product id = " + productId + " to cart")
            .get("/cart/add/" + productId)
    );
}
```

```java
// AcetoySimulation.java — composes building blocks into a journey
private ScenarioBuilder scn = scenario("AcetoySimulation")
    .exec(StaticPages.homepage)
    .pause(Duration.ofSeconds(2), Duration.ofSeconds(5))
    .exec(Cart.addToCart(20))
    .pause(...)
    .exec(Cart.viewCart)
    // ...
```

**Conventions:**
- Parameter-free actions → `public static final ChainBuilder fieldName`
- Parameterized actions (need an ID/value) → `public static ChainBuilder methodName(args)`
- Each chain owns its own assertions (`.check(...)`) — assertions don't leak into the simulation.
- One class per page or domain concept; group methods inside.

**When adding a new app target:** mirror the layout — `src/test/java/<app>/<App>Simulation.java` plus a `<app>/pageobjects/` subpackage.

---

## 5. Current scenario flow

`AcetoySimulation` runs this journey (with 2-5s random pauses between every step):

```
 1. Load homepage                       ← captures CSRF token into session
 2. Get "Our Story" page
 3. Get "Get In Touch" page
 4. Browse category (categoryFeeder)    ← .feed(categoryFeeder).exec(Category.listProducts)
 5. Paginate page 1 of same category
 6. Paginate page 2 of same category
 7. Add product to cart (productFeeder) ← .feed(productFeeder).exec(Cart.addToCart)
 8. Browse category (categoryFeeder)
 9. Add product to cart (productFeeder)
10. Browse category (categoryFeeder)
11. View product (productFeeder)        ← .feed(productFeeder).exec(Products.viewProduct), asserts #{name} in body
12. Browse category (categoryFeeder)
13. Add product to cart (productFeeder)
14. View cart
15. Pick random user (userFeeder)       ← .feed(userFeeder)
16. Log in (Customer.login: re-saves CSRF, asserts "Logout" visible)
17. Increase quantity of FIRST added    ← .exec(session.set("id", session.getString("firstId"))).exec(Cart.increaseQuantity)
18. Decrease quantity of LAST added     ← .exec(session.set("id", session.getString("lastId"))).exec(Cart.decreaseQuantity)
19. Checkout                            ← asserts "Order complete!"
20. randomSwitch: 10% chance of logout  ← .randomSwitch().on(Choice.withWeight(10.0, exec(Customer.logout)))
21. Redirect to homepage
```

Feeder strategies: `categoryFeeder` `.circular()`, `userFeeder` `.random()`, `productFeeder` (`jsonFile`) `.random()`.

**Cart tracking:** the simulation saves the 1st `addToCart`'s `#{id}` as `#{firstId}` and the 3rd's as `#{lastId}`. Before increase/decrease, it copies the right one back into `#{id}` so each op targets a product actually in the cart.

Run profile: **`atOnceUsers(1)`** — a smoke test, *not* a load test (this is the next thing the performance roadmap will change).

---

## 6. Key patterns to know

### 6.1 CSRF token handling

The site rotates a CSRF token in `<meta name="_csrf" content="...">`. The pattern:

1. **Capture on first page load** — `StaticPages.homepage` saves it as `csrfToken` in the session.
2. **Replay in POST forms** — `Customer.login` and `Customer.logout` send `_csrf=#{csrfToken}` as a form parameter.
3. **Re-capture after rotation** — both `Customer.login` and `Customer.logout` re-save the rotated token from the response.

**If you add any new POST request, follow this pattern** or it will 403.

### 6.2 Static asset filtering

```java
.inferHtmlResources(AllowList(), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ...))
```

Gatling will automatically fetch resources linked in HTML responses (like a browser). The `DenyList` excludes images, fonts, CSS, JS, and `detectportal.firefox.com` so the report focuses on application requests, not asset traffic.

### 6.3 Base URL parameterization

```java
private static final String BASE_URL = System.getProperty("baseUrl", "https://acetoys.uk");
```

Override at runtime: `./mvnw gatling:test -DbaseUrl=https://staging.example.com`. **No need to edit code** to point at a different environment.

---

## 7. How to run

### Run all simulations (interactive picker)
```bash
./mvnw gatling:test
```

### Run a specific simulation non-interactively
```bash
./mvnw gatling:test -Dgatling.simulationClass=acetoy.AcetoySimulation
```

### Override target environment
```bash
./mvnw gatling:test \
  -Dgatling.simulationClass=acetoy.AcetoySimulation \
  -DbaseUrl=https://staging.example.com
```

### Compile only
```bash
./mvnw test-compile
```

### Clean reports / build artifacts
```bash
./mvnw clean
```

### Run from IntelliJ / IDE
Execute `Engine.main` (in `src/test/java/Engine.java`). It uses `IDEPathHelper` to find resources/results paths automatically. No Maven needed for the run itself.

### Record a new simulation from a browser session
Execute `Recorder.main`. Configure HAR or HTTP-proxy mode in `src/test/resources/recorder.conf`.

### View the report
```bash
open target/gatling/<simulation>-<timestamp>/index.html
```

---

## 8. Current state — what works, what's missing

### ✅ Working well
- Page Object pattern is clean and used consistently in `Category`, `Cart`, `StaticPages`, `Products`.
- CSRF capture/replay/re-capture is correct.
- Static asset filtering keeps reports focused on app traffic.
- Base URL is parameterized for multi-environment use.
- Most chains have per-request assertions (status codes, content snippets).

### ✅ Recently closed

- ~~Login/Logout inlined~~ → extracted into `Customer.java` page object with status/CSRF/content assertions.
- ~~Logout has no `.check(...)`~~ → `Customer.logout` asserts status, re-saves CSRF, and checks `"Login"` text visible.
- ~~Hardcoded credentials `user1`/`pass`~~ → `Users.csv` feeder (3 users) wired via `.feed(userFeeder)`.
- ~~`.isEL("All Products")` with literal string~~ → `Category.java` rewritten to use `.isEL("#{categoryName}")` with session vars.
- ~~Hardcoded 6 category-specific methods~~ → `Category.listProducts` + `listProductsPage(int)`, driven by `Category.csv` feeder.
- ~~No logout variability~~ → `randomSwitch` gives 10% chance of explicit logout; 90% just abandon (realistic).
- ~~Hardcoded product slug `doctors-play-kit`~~ → `Products.viewProduct` data-driven via `ProductDetails.json` feeder, asserts `#{name}` in response.
- ~~Hardcoded product IDs `20`, `4`, `13`~~ → `Cart.addToCart`/`increaseQuantity`/`decreaseQuantity` use `#{id}` from `ProductDetails.json` feeder.
- ~~Substring assertions `"Logout"` / `"Login"` unverified~~ → both proven via 1-user run (login + 100%-logout force run).
- ~~Products data in CSV~~ → migrated to `ProductDetails.json` (richer schema: id, name, slug, description, categoryName, price).
- ~~Cart increase/decrease used random products, not ones in cart~~ → simulation now tracks `firstId`/`lastId` from `addToCart` and restores `#{id}` before increase/decrease.
- ~~Login state not tracked in session~~ → new `acetoy/session/UserSession.java` exposes `customerLoggedIn` flag + `init`/`markLoggedIn`/`markLoggedOut` helpers. `Customer.login`/`logout` automatically mark the flag at the end of their chains.
- ~~`viewCart` had no auth handling~~ → `Cart.viewCart` wraps with `doIf(session -> !session.getBoolean("customerLoggedIn")).then(exec(Customer.login)).exec(...)` — encapsulated per the trainer's pattern.
- ~~Pagination assertion failed on single-page categories~~ → `Category.cyclePagesOfProducts` uses `asLongAs` loop with a new `categoryPages` column in `Category.csv`. "all" has 3 pages, others have 1, so the loop never runs for single-page categories.
- ~~Anonymous addToCart returned the login page, breaking cart-state assertions~~ → login moved BEFORE first `addToCart` in the scenario (anonymous browse → login → all cart actions logged-in).
- ~~`UserSession.init` placed after `homepage` wiped the server session~~ → `init` (which includes `flushCookieJar()`) now runs FIRST, before any HTTP request.

### ⚠️ Gaps still open

Ranked by priority:

| # | Issue | Where | Impact |
|---|---|---|---|
| 1 | `atOnceUsers(1)` is a smoke test, not a load test | `AcetoySimulation.java` end | No realistic load profile yet |
| 4 | No global assertions for CI gates | `AcetoySimulation.java` end | Build can't fail on p95 / error-rate regression |
| 7 | Single hardcoded flow; no `randomSwitch` for browse-only vs checkout users | `AcetoySimulation.java` scenario | Doesn't model real user diversity beyond logout variability |
| 9 | Uncommitted deletion of `computerdatabase` simulation | git working tree | Tidy up before starting new work |
| 12 | `ProductDetails.json` has `categoryName` field that collides with `Category.csv`'s `categoryName` session var | `data/ProductDetails.json` | Currently safe (flow order makes overwrite harmless), but a future reorder could break category checks. Rename to `productCategoryName` if you reorder. |
| 13 | Basket-count substring assertion (`"You have <span>#{itemsInBasket}</span> products in your basket"`) doesn't match Acetoy's current response | `Cart.addToCart` | Trainer's pattern from course screenshot; Acetoy's HTML appears to differ. Debug probe currently in place; need to capture actual response (use `bodyString().saveAs(...)`) and refine the assertion. |
| 14 | Temporary diagnostics still in place (task #11) | `AcetoySimulation` | `atOnceUsers(3)` and `>>> VU ...` println pending revert once verification confidence is achieved. |

---

## 9. Performance roadmap

Order chosen so each step lands a small, safe change before the next:

1. **Tidy** — commit (or revert) the `computerdatabase` deletion so `git status` is clean.
2. ✅ **Refactor Customer (login/logout)** — done. Page object at `acetoy/pageobjects/Customer.java`; logout wrapped in `randomSwitch` (10%).
3. ✅ **Add feeders (users + categories + products)** — done. `data/Users.csv`, `data/Category.csv`, and `data/ProductDetails.json` (JSON for products) all wired via `.feed(...)`. All four page objects (Customer, Category, Products, Cart) are data-driven.
4. **Split simulations by test type** (#1) — extract the scenario, then create:
   - `AcetoyLoadSimulation` — `rampUsers` + `constantUsersPerSec` for expected peak.
   - `AcetoyStressSimulation` — staircase ramp to find the breaking point.
   - `AcetoySpikeSimulation` — sudden burst then recover.
   - `AcetoySoakSimulation` — moderate load for 4+ hours, watching for memory leaks.
5. **Add CI assertions** (#4) — `global().responseTime().percentile(95).lt(500)` etc. on each simulation so the build fails on regression.
6. **Diversify flow** (#7) — `randomSwitch` to model browse-only, browse-and-cart, and checkout users in realistic proportions.
7. **Wire into CI/CD** — GitHub Actions workflow that runs the load simulation against staging on every PR and posts the report as an artifact.
8. **Monitoring integration** — forward Gatling metrics to Prometheus/Grafana via the Graphite-compatible sink in `gatling.conf`.

> Reference patterns for each step live in the installed `performance-engineering` skill:
> `~/.claude/skills/performance-engineering/references/gatling-patterns.md`.

---

## 10. References

- [`CLAUDE.md`](./CLAUDE.md) — Claude Code briefing (terse, command-focused).
- `~/.claude/skills/performance-engineering/SKILL.md` — installed skill covering test strategy, CI gates, DB optimization, memory leaks, monitoring.
- [Gatling Java docs](https://docs.gatling.io/reference/script/core/simulation/) — official DSL reference.
- `pom.xml` — pinned versions and JDK toggles.
- `src/test/resources/gatling.conf` — runtime tuning knobs (mostly commented defaults).

---

## 11. Session log

### 2026-05-19

Big iteration day — aligned the codebase to the trainer's course patterns and exercised it against acetoys.uk.

- **UserSession**: created `acetoy/session/UserSession.java` with `customerLoggedIn`, `itemsInBasket`, `productsListPageNumber`, plus `flushCookieJar()` in `init`. After hitting a 405 login error, learned that `init` must run **before** any HTTP request (it now does).
- **Conditional login**: refactored `Cart.viewCart` to wrap with `doIf` for not-logged-in users — matches trainer's screenshot exactly. Also baked `markLoggedIn`/`markLoggedOut` into `Customer.login`/`logout` so the simulation doesn't manage the flag manually.
- **Pagination**: replaced fixed `listProductsPage(0)`/`(1)` calls with an `asLongAs` loop driven by a new `categoryPages` column in `Category.csv`. No more assertion failures on single-page categories.
- **ProductDetails.json**: migrated `Products.csv` → `ProductDetails.json` per trainer's course; richer schema, native JSON feeder.
- **Cart feeder**: refactored `addToCart`/`increaseQuantity`/`decreaseQuantity` to use `#{id}` from `ProductDetails.json`. Closed the consistency gap — all 4 page objects now data-driven.
- **firstId/lastId tracking**: added session-var bookkeeping so increase/decrease target products actually in the cart, not random new feeds.
- **Scenario restructure**: moved login to BEFORE the first `addToCart` (anonymous addToCart on Acetoy redirects to login — assertions on response body wouldn't have matched otherwise).
- **Basket-count assertion (in progress)**: attempted trainer's `substring("You have <span>#{itemsInBasket}</span> products in your basket")` — currently fails (Acetoy markup differs from screenshot). Debug probe in place; needs `bodyString().saveAs(...)` capture + lambda inspection to find the actual format. See gap #13.
- **Permanent status checks**: added `.check(status().in(200, 302))` to all four Cart product-touching methods + `.check(status().is(200))` to `StaticPages.getInTouch`.

Verification: 27/27 OK after each refactor checkpoint with `atOnceUsers(1)`. Last verification run with `atOnceUsers(3)`: 74/74 OK after pagination refactor; mean response 286ms, p95 765ms (acetoys.uk demo, public network).

**Lessons saved to memory:**
- Session-init ordering matters: `flushCookieJar()` before any HTTP request.
- `.check(...).saveAs(...)` fails the request when pattern doesn't match — use `.optional()` or `bodyString().saveAs(...)` for non-failing capture.
- Don't assume substring/CSS assertion text matches the live site; verify with a 1-user probe first.