# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Gatling 3.8.4 load-test project (Java DSL) targeting the demo site `https://acetoys.uk`. Built with Maven; JDK 17 is the active toolchain (JDK 8/11 toggles are commented in `pom.xml`).

## Commands

The Maven wrapper is checked in — use it instead of a system `mvn`.

- Run all simulations interactively (prompts to pick one): `./mvnw gatling:test`
- Run a specific simulation non-interactively: `./mvnw gatling:test -Dgatling.simulationClass=acetoy.AcetoySimulation`
- Override the target base URL (read by `AcetoySimulation` via `System.getProperty("baseUrl", ...)`): `./mvnw gatling:test -DbaseUrl=https://staging.example.com`
- Compile only: `./mvnw test-compile`
- Clean reports/binaries: `./mvnw clean`
- Run from IDE: execute `Engine.main` (in `src/test/java/Engine.java`). Paths are resolved by `IDEPathHelper`, which locates the project root via `gatling.conf` on the classpath.
- Record a new simulation from a browser session: run `Recorder.main` (in `src/test/java/Recorder.java`); config is in `src/test/resources/recorder.conf`.

Reports land in `target/gatling/<simulation>-<timestamp>/index.html`.

## Architecture

Simulations live under `src/test/java/<app>/` and follow a Page-Object-style layout — distinct from a vanilla recorded Gatling script:

- The simulation class (e.g. `acetoy/AcetoySimulation.java`) wires the `HttpProtocolBuilder`, composes the `ScenarioBuilder` from page-object chains, and calls `setUp(...)` in an instance initializer.
- Page objects (e.g. `acetoy/pageobjects/{StaticPages,Category,Products,Cart}.java`) expose reusable `ChainBuilder` values — either `public static final` fields for parameter-free actions, or `public static` methods returning a `ChainBuilder` when the request needs an argument (e.g. `Cart.addToCart(int productId)`). Each chain owns its own assertions (`check(...)`).
- CSRF handling: `StaticPages.homepage` captures `meta[name='_csrf']` into the session as `csrfToken` on the first page load, and the login chain in `AcetoySimulation` re-captures it after authenticating. POSTs (login, logout) re-send `#{csrfToken}` as a form param. New scenarios that POST must follow this pattern.
- Static asset filtering: `inferHtmlResources` is enabled with a DenyList for images/fonts/JS/CSS so the simulation models real browser fetches without overcounting asset traffic.

When adding a new application target, create `src/test/java/<app>/` with a `<App>Simulation.java` plus a `pageobjects/` subpackage, mirroring the `acetoy` structure. The `Engine`, `IDEPathHelper`, and `Recorder` classes at the top of `src/test/java/` are the standard Gatling Maven archetype and should not need editing.

## Resources

- `src/test/resources/gatling.conf` — Gatling runtime config (mostly defaults; uncomment keys to override).
- `src/test/resources/recorder.conf` — HAR/proxy recorder defaults.
- `src/test/resources/logback-test.xml` — log levels during simulations.
- `src/test/resources/<app>/<simulation>/` — output directory used by the recorder for that simulation (e.g. `acetoy/acetoysimulation/`).