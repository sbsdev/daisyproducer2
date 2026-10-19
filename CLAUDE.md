# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Daisyproducer2** is a production management system for accessible media (Daisy Talking Books, Large Print, Braille), serving the Swiss Library for the Blind (SBS). It manages documents through their lifecycle: import from Alfresco, braille/hyphenation word dictionary management, and generation of accessible formats via Daisy Pipeline 2 and liblouis.

## Commands

### Build
```bash
lein uberjar                    # Build standalone JAR
```

### Run
```bash
java -Dconf=dev-config.edn -jar target/uberjar/daisyproducer2.jar
# Default port: 3000 — Swagger UI at http://localhost:3000/swagger-ui/index.html
```

### Test
```bash
lein test                       # All tests
lein test :non-database         # Fast tests (no DB required)
lein test :only daisyproducer2.test.words-test          # Single namespace
lein test :only daisyproducer2.test.words-test/test-fn  # Single test function
```

Tests marked `^:database` require a live MariaDB test database (configured in `test-config.edn`).

### Lint
```bash
lein splint                     # Splint (idiom/style linter)
lein clj-kondo --lint src       # clj-kondo
```

### Release
```bash
lein release                    # Bump the version, turn [Unreleased] in CHANGELOG.md into a section
                                # for the new version, commit, tag, bump to the next SNAPSHOT and push
git push origin <version>       # If pushing the tag failed: GitHub builds the release jar only for a pushed tag
```
The steps are defined in `:release-tasks` in `project.clj` (`lein help release` only shows the defaults). The changelog step (`scripts/changelog_release.clj`) stops the release if `[Unreleased]` is empty. Deployment is done separately with the Ansible playbook in the `sbs-infrastructure` repository, which also runs the database migrations.

### Frontend
```bash
npm install                     # Install JS dependencies (react, react-dom, xregexp)
```
Shadow-CLJS handles ClojureScript compilation (`shadow-cljs.edn`) but requires npm packages to be installed first.

## Architecture

**Stack:** Clojure backend + ClojureScript/React frontend (SPA), MariaDB 10.1 (old: no `utf8mb4_0900_*` collations, 767 byte index key limit), Luminus framework base.

### Backend (`src/clj/daisyproducer2/`)

- **`core.clj`** — Entry point, Mount component lifecycle
- **`handler.clj`** — Middleware stack and router assembly
- **`routes/services.clj`** — All REST API endpoints (Reitit, fully Swagger-documented)
- **`db/core.clj`** — Database layer via Conman; SQL lives in `resources/sql/`
- **`documents/`** — Document lifecycle: create, update, versions, images, products, state transitions (open/closed), previews
  - **`documents/abacus.clj`** — Imports document metadata from the Abacus ERP system
  - **`documents/alfresco/`** — Synchronization with Alfresco
- **`words/`** — Global and local braille/hyphenation word dictionaries; unknown word detection
- **`pipeline2/`** — HTTP client for the Daisy Pipeline 2 web service (`core.clj`) and its conversion scripts (`scripts.clj`)
- **`pipeline1.clj`** — Wrapper around the command line scripts of the legacy Daisy Pipeline 1
- **`louis.clj`**, **`hyphenate.clj`** — Braille translation (liblouis) and hyphenation
- **`ldap.clj`** — LDAP/IPA authentication

Roles: `admin`, `review`, `translator` — enforced via Buddy middleware.

### Frontend (`src/cljs/daisyproducer2/`)

Re-frame (event-driven state) + Reagent (React wrapper) + Bulma CSS.

- **`core.cljs`** — App entry, client-side routing (Reitit), navbar
- **`documents.cljs`** — Document list and detail pages
- **`words.cljs`** — Word dictionary management UI

### Database Migrations

SQL migration files live in `resources/migrations/`. They are **not** applied on startup; run them explicitly (Luminus Migrations / migratus), which then exits:

```bash
lein run migrate                                                       # dev, against dev-config.edn
java -Dconf=/etc/daisyproducer2/config.edn -jar daisyproducer2.jar migrate   # on a server
```

Applied migrations are recorded in the `schema_migrations` table. Separate statements in one migration file with `--;;`.

### Key External Integrations

| System | Purpose |
|--------|---------|
| Alfresco | Source document repository |
| Daisy Pipeline 2 | DTBook → Daisy/EPUB/Large Print generation |
| liblouis-java | Braille translation |
| jhyphen | Hyphenation |
| IPA/LDAP | Authentication |
| Abacus ERP | Document metadata import |

## Braille Grade Terminology

German braille has three grades with the following official names:

| API `contraction` value | German name | Description |
|-------------------------|-------------|-------------|
| `0` | Basisschrift | No contractions |
| `1` | Vollschrift | Partial contractions (Grade 1) |
| `2` | Kurzschrift | Full contractions (Grade 2) |

The API uses numeric values `0`/`1`/`2` rather than the German names. The generated files use `.bv` (contraction 0 or 1) and `.bk` (contraction 2) extensions. Internally, liblouis uses `sbs-de-g0-core.mod`, `sbs-de-g1-core.mod`, and `sbs-de-g2-core.mod` respectively.

## Manual HTTP Testing

All three files use Emacs `restclient-mode` format (`# -*- mode: restclient; -*-`).

- **`test/rest-api.http`** — Authoritative manual test suite for the dp2 REST API. Covers auth, documents, versions, images, products, words (global/local/unknown), Abacus import, Alfresco sync, and preview generation. Set `:auth-token` at the top before using authenticated endpoints.
- **`test/alfresco.http`** — Exploratory requests directly against the Alfresco Content Services API (`pam04.sbszh.ch`). Contains placeholder credentials (`:auth = Basic <>`). Used for research; completeness and correctness not guaranteed.
- **`test/pipeline2.http`** — Reference/documentation for the Daisy Pipeline 2 Web Service API (`localhost:8181`). Contains hardcoded job IDs and local file paths. Used sporadically; treat as illustrative rather than runnable as-is.

## Configuration

- `dev-config.edn` — Local dev config (DB, LDAP, paths, JWT secret)
- `test-config.edn` — Test DB only
- `env/dev/`, `env/prod/`, `env/test/` — Environment-specific overrides
