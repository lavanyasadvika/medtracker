# MedTracker

A comprehensive medication tracking and management system built with Java 17, Spring Boot 3.x, PostgreSQL 15, and a Vite React 18 frontend. MedTracker enables healthcare teams to manage medicine inventory, scan labels/barcodes, track expiry and low-stock alerts, export reports, and maintain role-based access control.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Client Setup](#client-setup)
- [Running with Docker](#running-with-docker)
- [Monitoring & Observability](#monitoring--observability)
- [Project Structure](#project-structure)
- [Architecture & Design](#architecture--design)
- [Client Pages](#client-pages)
- [Development Guidelines](#development-guidelines)
- [Testing](#testing)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [Repository](#repository)
- [Authors](#authors)

---

## Project Overview

MedTracker is a full-stack medicine inventory and expiry tracking application designed to streamline medication management for healthcare environments. It provides:

- **Medicine Inventory Management** – Create, update, view, soft-delete, dispense, and restock medicines.
- **Role-Based Access Control (RBAC)** – Enforced on every backend endpoint and mirrored in frontend route navigation.
- **React Client Portal** – Vite React UI for login, signup, dashboard, inventory, scan, reports, and admin routes.
- **Scan & Reports Workflows** – API support for barcode/label scan pre-fill and inventory/expiry/low-stock reporting.
- **Audit & Compliance** – Structured error handling, JWT security, validation, and documented API contracts.

> **Reference Documents:**
> - `MedTracker_BRD_v1.3.docx` – Business Requirements Document
> - `MedTracker_DB_Design_Final.docx` – Database Design & DDL
> - `system architecture.pdf` – System Architecture Overview

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 17 |
| Backend Framework | Spring Boot 3.2.x |
| Frontend | React 18, Vite 5, React Router 6, Axios 1.x |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Validation | jakarta.validation |
| Security | Spring Security stateless JWT (HS256, 24h access tokens), CORS allow-list |
| Utilities | Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@Builder`) |
| Backend Testing | JUnit 5 + Mockito |
| Code Quality | SonarQube |
| Package Root | `com.medtracker` |
| Scan Integrations | ZXing, OpenFDA, Groq Vision |
| Report Exports | CSV writer, Apache PDFBox PDF generation |

---

## Prerequisites

Ensure the following are installed before setting up the project:

- **Java 17** (JDK 17+)
- **PostgreSQL 15**
- **Maven 3.8+**
- **Node.js 18+** and **npm** for the React client
- **Git**

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://pscode.lioncloud.net/sdecraft/trainings/leading-w-psai-products/batch-20/group-7/medtracker.git
cd medtracker
```

Or push an existing repository:

```bash
cd existing_repo
git remote add origin https://pscode.lioncloud.net/sdecraft/trainings/leading-w-psai-products/batch-20/group-7/medtracker.git
git branch -M main
git push -uf origin main
```

### 2. Configure the Database

Create a PostgreSQL 15 database, copy `server/src/main/resources/application-example.yml` to `server/src/main/resources/application.yml`, and fill in local environment values. Do not commit `application.yml`.

Minimum backend variables:

```bash
DB_USERNAME=<your_db_user>
DB_PASSWORD=<your_db_password>
JWT_SECRET=<at-least-256-bit-random-string>
REACT_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

JWT signing is configured with `app.jwt.secret` (provided via `JWT_SECRET`) and must be at least 256 bits for HS256. React CORS access is configured with `app.cors.allowed-origins` (provided via `REACT_ALLOWED_ORIGINS`). The current client `vite.config.js` runs on `http://localhost:3000`; `http://localhost:5173` is also allowed for Vite default compatibility.

Medicine label scan extraction uses `app.groq.api-key`, `app.groq.base-url`, `app.groq.model`, `app.groq.timeout-seconds`, and `app.groq.mock-enabled`. The default Groq model is `meta-llama/llama-4-scout-17b-16e-instruct`; keep `GROQ_MODEL` aligned with an active Groq vision-capable model before live testing, and set `app.groq.mock-enabled=true` for offline tests. Barcode lookup uses `app.openfda.base-url` and `app.openfda.timeout-seconds`.

Scheduled alert emails use `spring.mail.*` SMTP settings, `app.mail.from`, `app.mail.from-name`, and `app.alerts.*`. The default alert cron is `0 0 8 * * *`, expiry lead time defaults to 30 days, and medicines without an item-level low-stock threshold use `app.alerts.default-low-stock-threshold`.

> **Important:** Follow the DDL defined in `MedTracker_DB_Design_Final.docx` exactly. Do not invent columns or tables beyond the DDL and agreed category definitions.

### 3. Build the Backend

```bash
cd server
mvn clean install
```

### 4. Run the Backend

```bash
cd server
mvn spring-boot:run
```

The backend starts on `http://localhost:8080` with servlet context path `/api`.

---

## Client Setup

The React client lives in `client/` and uses Vite port `3000`.

```bash
cd client
npm install
copy env.example .env
npm run dev
```

Client environment:

```bash
VITE_API_URL=http://localhost:8080/api
```

Login calls `POST /api/auth/login` with `{ email, password }`. Registration calls `POST /api/auth/register` with `{ name, email, password, role }`. The server returns `AuthResponse { token, tokenType, expiresInMs, role, name, email }`, which the client stores in `sessionStorage` for the active browser session.

If the browser reports a CORS preflight failure from `http://localhost:3000`, ensure the backend `application.yml` or environment has:

```bash
REACT_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

---

## Running with Docker

The whole stack (PostgreSQL + backend + frontend) can run in containers with no local
Java/Maven/Node/Postgres install required. This is the fastest way to stand the app up
on a brand-new machine.

### What's in the repo

| File | Purpose |
|---|---|
| `docker-compose.yml` | Defines the 3 services (`db`, `backend`, `frontend`), their network, and the named volume that persists Postgres data |
| `server/Dockerfile` | Multi-stage build: compiles the backend with Maven, then copies the jar into a slim JRE image |
| `server/src/main/resources/application-docker.yml` | Spring profile used inside the container (`SPRING_PROFILES_ACTIVE=docker`); every value is env-var driven, no secrets are baked into the image |
| `client/Dockerfile` | Multi-stage build: compiles the React app with Vite, then serves the static build with Nginx |
| `client/nginx.conf` | Nginx config with SPA fallback routing (so React Router paths work on refresh) |
| `.env.example` | Safe template of every variable `docker-compose.yml` reads — commit-safe, no real secrets |
| `.env` | Your real local values (DB creds, JWT secret, Groq key, SMTP creds). **Gitignored — never commit this.** |

Every setting has a safe fallback baked into `docker-compose.yml`, so the stack boots
even with an empty/missing `.env` — you only need to fill in `GROQ_API_KEY` if you want
real AI label-scanning instead of the mocked response.

### First-time setup on a new machine

1. **Install Docker Desktop** (includes Docker Engine + Compose). On Windows, make sure
   it's running (check the whale icon in the system tray) before running any command below.
2. **Clone the repo** and `cd` into the project root (the folder containing `docker-compose.yml`).
3. **Create your env file:**
   ```bash
   cp .env.example .env
   ```
   Open `.env` and fill in `GROQ_API_KEY` if you have one (leave `GROQ_MOCK_ENABLED=true`
   otherwise). The DB credentials, JWT secret, and everything else already have working
   defaults for local/demo use.
4. **Build and start everything:**
   ```bash
   docker compose up --build -d
   ```
   First run takes a few minutes (downloading base images, Maven dependencies, npm
   packages). Subsequent runs are much faster thanks to Docker's layer cache.
5. **That's it — there is no separate database setup step.** The `db` service starts a
   fresh PostgreSQL 15 container, and when the backend container starts it automatically
   runs all Flyway migrations against it (creates every table and seeds the bootstrap
   admin user). You never need to run `psql` or create the schema by hand.
6. **Verify it worked:**
   - Frontend: [http://localhost:3000](http://localhost:3000)
   - Backend health: [http://localhost:8080/api/actuator/health](http://localhost:8080/api/actuator/health) → `{"status":"UP"}`
   - Log in with `admin@medtracker.local` / `Admin@123`

Postgres itself is **not** published on host port 5432, so it won't conflict with a
locally-installed Postgres — the containers reach it over Docker's internal network by
the hostname `db`. If you need to inspect the database directly, use:
```bash
docker compose exec db psql -U postgres -d medtracker
```

### Everyday commands

| Action | Command |
|---|---|
| Start everything (after first build) | `docker compose up -d` |
| Start and rebuild (after pulling code changes) | `docker compose up --build -d` |
| Stop everything (keeps containers + data, quick to resume) | `docker compose stop` |
| Resume after `stop` | `docker compose start` |
| Stop and remove containers (data volume is kept) | `docker compose down` |
| **Full reset** — also deletes the database volume | `docker compose down -v` |
| View status of all 3 containers | `docker compose ps` |
| Tail logs (all services) | `docker compose logs -f` |
| Tail logs (one service) | `docker compose logs -f backend` |
| Rebuild just one service after a code change | `docker compose build backend && docker compose up -d backend` |

`docker compose stop`/`start` is the normal day-to-day pair — it's fast because it
doesn't touch images or the database volume. Reach for `down`/`up --build` when you've
changed a `Dockerfile`, `docker-compose.yml`, or dependency (`pom.xml`/`package.json`).
Only use `down -v` when you deliberately want a clean database (e.g. testing the
first-run experience) — it deletes all inventory/user data permanently.

### Data persistence

Postgres data lives in the named Docker volume `medtracker-db-data`, independent of the
containers. Stopping, restarting, or rebuilding the `backend`/`frontend` images never
touches it — only `docker compose down -v` (or manually removing the volume) deletes it.

### Ports used

| Service | Host port | Notes |
|---|---|---|
| frontend (Nginx) | `3000` | Change the left side of `"3000:80"` in `docker-compose.yml` if it's taken |
| backend (Spring Boot) | `8080` | Change the left side of `"8080:8080"` if it's taken |
| db (Postgres) | *(not published)* | Only reachable from other containers, as `db:5432` |

If `3000` or `8080` are already in use by a local `npm run dev` / `mvn spring-boot:run`
process from earlier manual runs, stop those first — Docker and the local dev servers
will otherwise fight over the same port.

---

## Monitoring & Observability

The Docker Compose stack includes three additional containers for metrics and
distributed tracing: **Prometheus**, **Grafana**, and **Zipkin**. They start
automatically with `docker compose up -d` — nothing extra to run.

| Tool | URL | Credentials | Purpose |
|---|---|---|---|
| Grafana | [http://localhost:3001](http://localhost:3001) | `admin` / `admin` (or your `GRAFANA_ADMIN_*` values in `.env`) | Dashboards — pre-provisioned **MedTracker Overview** dashboard under the "MedTracker" folder |
| Prometheus | [http://localhost:9090](http://localhost:9090) | none | Raw metrics + scrape target status (**Status → Targets** should show `medtracker-backend` as `UP`) |
| Zipkin | [http://localhost:9411](http://localhost:9411) | none | Distributed traces — search by service name `medtracker-backend` to see per-request timing, including the OpenFDA/Groq calls a scan request makes |

### How it's wired

- The backend exposes Prometheus-formatted metrics at `/api/actuator/prometheus`
  (via `micrometer-registry-prometheus`). Prometheus scrapes that endpoint every
  10s (config: `monitoring/prometheus/prometheus.yml`).
- The backend also reports traces to Zipkin (via `micrometer-tracing-bridge-brave`
  + `zipkin-reporter-brave`) at `${ZIPKIN_ENDPOINT}` — every incoming HTTP request
  gets a trace, and outbound calls to OpenFDA/Groq during a scan appear as child
  spans within that trace, so you can see exactly where time is spent.
- Grafana auto-loads Prometheus and Zipkin as datasources on startup
  (`monitoring/grafana/provisioning/datasources/datasources.yml`) and auto-loads
  the dashboard JSON in `monitoring/grafana/dashboards/` — no manual "Add
  datasource" or "Import dashboard" clicking required.
- `TRACING_SAMPLING_PROBABILITY` (default `1.0`, i.e. 100%) controls what
  fraction of requests get traced — fine for local/demo traffic, but you'd turn
  this down on real production volume.

### Seeing it work

1. Open the app ([http://localhost:3000](http://localhost:3000)) and click
   around — dashboard, inventory, a scan upload.
2. Open Grafana → **MedTracker Overview** dashboard to see request rate,
   latency percentiles, JVM heap, and the Hikari connection pool update live.
3. Open Zipkin, click **Run Query** to see the recent traces — click into a
   `/api/scan/upload` trace to see the OpenFDA/Groq call as a nested span with
   its own duration.

---

## Project Structure

```
medtracker
├── client/                         # Vite React client
│   ├── env.example                 # Client API URL template
│   ├── package.json                # React/Vite dependencies and scripts
│   └── src/
│       ├── api/                    # Axios API client and interceptors
│       ├── components/auth/        # ProtectedRoute and RoleRoute guards
│       ├── components/layout/      # Layout and Navigation
│       ├── context/                # AuthContext session state
│       ├── pages/
│       │   ├── auth/                   # LoginPage, SignupPage
│       │   ├── dashboard/               # DashboardPage
│       │   ├── inventory/               # InventoryPage, AddMedicine, EditMedicine, DispenseModal, RestockModal
│       │   ├── scan/                    # ScanPage (upload), ScanConfirm (pre-filled confirm form)
│       │   ├── reports/                 # ReportsPage (tabs), ExpiryReport, InventoryReport, LowStockReport
│       │   └── admin/                   # AdminPage (tabs), UserManagement, AlertSettings
│       └── utils/                  # Frontend role permission matrix, blob-download helper
├── server/                         # Spring Boot backend
│   └── src/main/java/com/medtracker
│       ├── config/                 # Non-security configuration classes/properties
│       ├── controller/             # REST controllers with RBAC enforcement
│       ├── dto/                    # Request/Response DTOs
│       ├── entity/                 # JPA entity classes and enums
│       ├── exception/              # GlobalExceptionHandler and custom exceptions
│       ├── external/               # Groq, OpenFDA, and ZXing integrations
│       ├── repository/             # Spring Data JPA repositories
│       ├── security/               # JWT utilities, filter, entry point, CORS, and SecurityConfig
│       └── service/                # Business logic layer
└── README.md
```

> Follow the package structure defined in the DDL and BRD exactly. Active web security/CORS configuration lives in `server/src/main/java/com/medtracker/security/SecurityConfig.java`.

---

## Architecture & Design

- **Layered Architecture:** Controller → Service → Repository
- **Dependency Injection:** Constructor injection only, via Lombok `@RequiredArgsConstructor`
- **RBAC:** Every endpoint enforces role-based access control per the BRD permission matrix
- **Security:** Stateless Spring Security resource server using Bearer JWTs; `/api/auth/login` and `/api/auth/register` are public while all other routes require authentication
- **CORS:** Backend allow-list supports comma-separated `app.cors.allowed-origins`, with local defaults for React dev ports `3000` and `5173`
- **Frontend Auth:** React stores JWT and `{ name, email, role }` in `sessionStorage`; route visibility mirrors server roles `ADMIN`, `USER`, `PHARMACIST`, and `VIEWER`
- **Scan Pipeline:** `/api/scan/upload` first attempts ZXing barcode decoding, then OpenFDA lookup, and falls back to Groq Vision only when no barcode is detected; scan results pre-fill the confirmation form and do not persist automatically
- **Scheduled Alerts:** Spring Scheduling runs daily configurable expiry and low-stock email alerts to active ADMIN and PHARMACIST users only
- **Error Handling:** All errors are routed through a single `GlobalExceptionHandler` (`@RestControllerAdvice`)
- **Input Validation:** All inputs validated using `jakarta.validation` annotations
- **Logging:** `@Slf4j` used consistently across service and controller layers
- **Builder Pattern:** `@Builder` applied where natural for object construction

---

## Client Pages

- **Scan (`/scan`, ADMIN/PHARMACIST only):** `ScanPage` uploads a photo (gallery or `capture="environment"` camera input) via multipart `POST /api/scan/upload`. A `200` pre-fills `ScanConfirm` with the returned `drugName`/`expiryDate`/`source`/`manufacturer`/`barcode` (FR-11) for review/edit; a `422` extraction failure offers **Retake Image** or **Enter Manually** (which opens the existing `AddMedicine` modal). `ScanConfirm` persists only on explicit confirm, via `POST /api/medicines` — the scan call itself never saves a record.
- **Dispense/Restock modals (from `InventoryPage` rows):** `DispenseModal` (ADMIN/PHARMACIST/VIEWER) calls `POST /api/medicines/{id}/dispense`; a `400` insufficient-stock response shows `Insufficient stock, available: N` inline (FR-17). `RestockModal` (ADMIN/PHARMACIST only) calls `PUT /api/medicines/{id}/restock`. Both refresh the inventory row's quantity live on success; VIEWER never sees the restock action.
- **Reports (`/reports`, all authenticated roles):** `ReportsPage` tabs between `ExpiryReport` (colour-coded red/amber/green per FR-23), `InventoryReport`, and `LowStockReport` (FR-25..27), each backed by `GET /api/reports/{expiry|inventory|low-stock}`. Export buttons (CSV/PDF via `GET /api/reports/{type}/export`) are visible only to ADMIN/PHARMACIST (FR-28) and trigger a client-side file download.
- **Admin (`/admin`, ADMIN only):** `AdminPage` tabs between `UserManagement` (list/create/edit-role/deactivate via `/api/admin/users*`) and `AlertSettings` (expiry lead time + default low-stock threshold via `/api/admin/settings`, FR-19/FR-24). There is no reactivate endpoint, so deactivated users show a status badge with no further action.

---

## Development Guidelines

All contributors must adhere to the following rules (as defined in `.agent/instructions.md`):

1. **Language & Framework:** Java 17, Spring Boot 3.x, PostgreSQL 15 — versions must be followed strictly.
2. **Package Root:** `com.medtracker` — follow the package structure from the DDL exactly.
3. **Dependency Injection:** Use constructor injection only via `@RequiredArgsConstructor`. No field injection.
4. **Lombok:** Use `@Slf4j` for logging, `@Builder` where natural.
5. **RBAC:** Every endpoint must enforce RBAC per the BRD permission matrix.
6. **Validation:** Use `jakarta.validation` for all input validation.
7. **Error Handling:** Route all errors through the single `GlobalExceptionHandler`.
8. **Code Quality:** Prefer minimal, targeted code. Avoid unnecessary scaffolding.
9. **Database:** Do not invent columns or tables beyond the DDL and agreed category definitions.

---

## Testing

Unit tests are written using **JUnit 5 + Mockito**.

### Running Tests

```bash
# Maven
mvn test

# Gradle
gradle test
```

### Testing Standards

- **Controller Tests:** Use `MockMvcBuilders.standaloneSetup(...)` — do not load the full security context.
- **Mocking:** No nested `when()` stubs. Keep cognitive complexity low (SonarQube-clean).
- **Coverage:** Aim for high coverage on service and controller layers.
- **Test Naming:** Use descriptive names following the `should_<action>_when_<condition>` convention.

---

## API Documentation

The Swagger/OpenAPI document is maintained at:

```
server/src/main/resources/openapi.yml
```

Swagger UI is public and available without a Bearer token once the application is running:

```
http://localhost:8080/api/swagger-ui.html
```

The OpenAPI JSON is also public at:

```
http://localhost:8080/api/v3/api-docs
```

### Authentication API

Base path: `/api/auth`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/register` | Public demo endpoint | Registers a user from `{ name, email, password, role }`, BCrypt-hashes the password, defaults `is_active=true`, rejects duplicate emails with `409`, and returns `AuthResponse`. |
| POST | `/login` | Public | Authenticates with `{ email, password }` through `AuthenticationManager`; returns `AuthResponse { token, tokenType, expiresInMs, role, name, email }` on success; bad credentials return `401`. |
| GET | `/me` | Authenticated | Returns the current user's profile from the Bearer JWT subject. |

### Medicine API

Base path: `/api/medicines`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/` | ADMIN, PHARMACIST | Adds a medicine, including manual entries with `source=MANUAL`; accepts `source`, `category`, and `lowStockThreshold`; sets `created_by` and `updated_by` from the current JWT user id. |
| GET | `/` | ADMIN, PHARMACIST, VIEWER, USER | Lists active medicines only. Optional filters: `expiringInDays`, `lowStock=true`, and `q` for drug-name search. |
| GET | `/{id}` | ADMIN, PHARMACIST, VIEWER, USER | Returns one active medicine. Missing or soft-deleted ids return `404`. |
| PUT | `/{id}` | ADMIN, PHARMACIST | Updates an active medicine and sets `updated_by` from the current JWT user id. Soft-deleted ids return `404`. |
| DELETE | `/{id}` | ADMIN | Soft-deletes a medicine by setting `is_active=false`; records are never hard-deleted. |
| POST | `/{id}/dispense` | ADMIN, PHARMACIST, VIEWER | Reduces stock by request body `{ "quantity": N }`, updates `updated_by`, and returns the updated `MedicineDTO`. Over-dispense returns `400` with `message` and `available`. |
| PUT | `/{id}/restock` | ADMIN, PHARMACIST | Increases stock by request body `{ "quantity": N }`, updates `updated_by`, and returns the updated `MedicineDTO`. |

### Medicine Scan API

Base path: `/api/scan`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/upload` | ADMIN, PHARMACIST | Accepts multipart field `image`, runs ZXing barcode detection first, uses OpenFDA for barcode matches, falls back to Groq Vision for label-only images, and returns `ScanResultDTO` to pre-fill the React confirmation form. This endpoint does **not** save the medicine; persistence still happens through `POST /api/medicines` after user confirmation. |

Scan outcomes:

- Barcode + OpenFDA hit: `source=BARCODE`, `drugName`, `manufacturer`, and `barcode` are returned.
- Barcode + OpenFDA miss: `source=BARCODE`, `barcode`, `manualEntryRequired=true`, and a manual-entry message are returned without failing.
- Label-only image: Groq returns strict JSON `{\"drug_name\":\"...\",\"expiry_date\":\"MM/YYYY\"}`, parsed to `source=GROQ` and an end-of-month `expiryDate`.
- Extraction failure: returns `422 Unprocessable Entity` with a retake/manual-entry message.

### Scheduled Alerts

Scheduled alerts are not exposed as public REST endpoints. `AlertScheduler.runScheduledAlerts()` runs with Spring `@Scheduled(cron = "${app.alerts.cron:0 0 8 * * *}")` and sends:

- Expiry alerts when active medicines are expired or expiring within `app.alerts.expiry-lead-days` (default `30`).
- Low-stock alerts when active medicine quantity is at/below its item threshold, or `app.alerts.default-low-stock-threshold` when no item threshold is set.
- Recipients are active users with role `ADMIN` or `PHARMACIST`; `VIEWER` users never receive alert emails.

For local Mailtrap/SMTP testing, configure `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`, `app.mail.from`, and temporarily set `app.alerts.cron` to a near-term cron expression. Restore the default cron after verification.

### Reports API

Base path: `/api/reports`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/inventory` | ADMIN, PHARMACIST, VIEWER, USER | Returns all active medicines with `name`, `category`, `quantity`, `expiry`, and `manufacturer`. |
| GET | `/expiry` | ADMIN, PHARMACIST, VIEWER, USER | Returns `expired`, `expiring`, and `safe` buckets. Each row includes status colour `red`, `amber`, or `green` per FR-23. |
| GET | `/low-stock` | ADMIN, PHARMACIST, VIEWER, USER | Returns active medicines where `quantity <= low_stock_threshold`, using the configured default threshold where applicable. |
| GET | `/{type}/export?format=csv|pdf` | ADMIN, PHARMACIST | Downloads `inventory`, `expiry`, or `low-stock` as CSV or PDF with a `Content-Disposition: attachment` filename. |

PDF exports use Apache PDFBox (`org.apache.pdfbox:pdfbox`) and CSV exports use a simple in-service writer.

### Admin API

Base path: `/api/admin`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/users` | ADMIN | Lists all users for admin management. |
| POST | `/users` | ADMIN | Creates a user by delegating to `AuthService.register`; duplicate email still returns `409`. |
| PUT | `/users/{id}` | ADMIN | Updates the user's name and role. |
| PATCH | `/users/{id}/deactivate` | ADMIN | Sets `is_active=false`; users are never hard-deleted and inactive users cannot authenticate. |
| GET | `/settings` | ADMIN | Reads runtime settings for expiry lead time and default low-stock threshold. |
| PUT | `/settings` | ADMIN | Updates runtime settings in memory for v1. |

Admin settings are backed initially by `app.alerts.expiry-lead-days` and `app.alerts.default-low-stock-threshold` through `AlertProperties`. For the demo, `PUT /api/admin/settings` mutates that `@ConfigurationProperties` bean in memory so reports and scheduled alerts use the new values immediately. These settings reset to application config on restart; a persistent settings table can replace this later without changing the REST contract.

Refer to `MedTracker_BRD_v1.3.docx` for the complete API specification and permission matrix.

---

## Contributing

1. Fork the repository and create a feature branch from `main`.
2. Follow all [Development Guidelines](#development-guidelines) strictly.
3. Write unit tests for all new functionality.
4. Ensure SonarQube analysis passes with no new critical/blocker issues.
5. Submit a merge request with a clear description of changes.

### GitLab Collaboration

- [Invite team members and collaborators](https://docs.gitlab.com/user/project/members/)
- [Create a new merge request](https://docs.gitlab.com/user/project/merge_requests/creating_merge_requests/)
- [Enable merge request approvals](https://docs.gitlab.com/user/project/merge_requests/approvals/)
- [Set up project integrations](https://pscode.lioncloud.net/sdecraft/trainings/leading-w-psai-products/batch-20/group-7/medtracker/-/settings/integrations)
- [Get started with GitLab CI/CD](https://docs.gitlab.com/ci/quick_start/)
- [Analyze your code with SAST](https://docs.gitlab.com/user/application_security/sast/)

---

## Project status
If you have run out of energy or time for your project, put a note at the top of the README saying that development has slowed down or stopped completely. Someone may choose to fork your project or volunteer to step in as a maintainer or owner, allowing your project to keep going. You can also make an explicit request for maintainers.

1. Fork the repository and create a feature branch from `main`.
2. Follow all [Development Guidelines](#development-guidelines) strictly.
3. Write unit tests for all new functionality.
4. Ensure SonarQube analysis passes with no new critical/blocker issues.
5. Submit a merge request with a clear description of changes.

### GitLab Collaboration

- [Invite team members and collaborators](https://docs.gitlab.com/user/project/members/)
- [Create a new merge request](https://docs.gitlab.com/user/project/merge_requests/creating_merge_requests/)
- [Enable merge request approvals](https://docs.gitlab.com/user/project/merge_requests/approvals/)
- [Set up project integrations](https://pscode.lioncloud.net/sdecraft/trainings/leading-w-psai-products/batch-20/group-7/medtracker/-/settings/integrations)
- [Get started with GitLab CI/CD](https://docs.gitlab.com/ci/quick_start/)
- [Analyze your code with SAST](https://docs.gitlab.com/user/application_security/sast/)

---

## Repository

**GitLab:** [https://pscode.lioncloud.net/sdecraft/trainings/leading-w-psai-products/batch-20/group-7/medtracker](https://pscode.lioncloud.net/sdecraft/trainings/leading-w-psai-products/batch-20/group-7/medtracker)

---

## Authors

- **Nagubilli Sadvika** – Project Lead & Architect
- **Sunil Regar** – Project Lead & Architect


---

*For questions or support, please open an issue in the GitLab repository.*
