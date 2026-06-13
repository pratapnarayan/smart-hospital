# SmartHospital

A full-stack, modular-monolith Hospital Management System built with **Spring Boot 3** and **React 19**.  
Designed as a production-grade hospital software — 15 clinical and administrative modules, schema-per-tenant multi-tenancy, granular RBAC, and a rich analytics layer with Excel and PDF export.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Modules](#modules)
- [Reports & Analytics](#reports--analytics)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [Running the Application](#running-the-application)
- [Default Dev Credentials](#default-dev-credentials)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Flyway Migration History](#flyway-migration-history)
- [Environment Variables](#environment-variables)
- [Roles & Permissions](#roles--permissions)
- [Security Notes](#security-notes)

---

## Overview

SmartHospital is a comprehensive Hospital Management System covering the full clinical and administrative workflow of a hospital — from patient registration and outpatient consultations, through inpatient admissions, pharmacy, pathology, radiology, HR, finance, inventory, blood bank, and operation theatre management — topped by a dedicated **Reports & Analytics** layer that aggregates data from all modules into interactive dashboards with one-click Excel and PDF export.

**Key design goals:**

- **Modular Monolith MVP** — clean domain boundaries designed for eventual microservices extraction
- **Schema-per-tenant multi-tenancy** — one PostgreSQL database, one schema per hospital; cross-tenant data access is architecturally impossible
- **Stateless JWT authentication** — identical auth contract for web and (future) mobile clients
- **Granular RBAC** — role + permission claim in every JWT; `@PreAuthorize` guards at method level
- **Flyway-only schema management** — `ddl-auto: validate`; Hibernate never touches DDL
- **Production-ready observability** — Micrometer + Prometheus metrics, Spring Actuator, structured logging

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser (SPA)                            │
│              React 19 + TypeScript + Ant Design 5               │
│              Vite dev server on  http://localhost:3000           │
│              /api  →  proxied to  http://localhost:8080         │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP / JSON (JWT in Authorization header)
┌────────────────────────────▼────────────────────────────────────┐
│                   Spring Boot 3.3 API                            │
│                    http://localhost:8080                          │
│                                                                   │
│  Filter chain:  TenantFilter → JwtAuthFilter → Spring Security   │
│                                                                   │
│  Modules (each owns domain / dto / repo / service / controller): │
│  auth · patient · doctor · opd · pharmacy · ipd · frontoffice    │
│  hr · pathology · radiology · finance · inventory · bloodbank    │
│  operation · setup · analytics                                   │
└────────────────────────────┬────────────────────────────────────┘
                             │ JDBC (search_path per tenant)
┌────────────────────────────▼────────────────────────────────────┐
│                  PostgreSQL 16                                    │
│   public schema  →  tenant registry + super-admin user           │
│   hospital_001   →  all clinical & operational tables            │
│   hospital_002   →  (next tenant, same tables, separate data)    │
└─────────────────────────────────────────────────────────────────┘
```

**Multi-tenancy** is implemented via a `TenantAwareDataSource` JDBC proxy that sets `search_path = <tenantId>` on every connection before executing queries. The tenant is resolved from the `tenant_id` claim embedded in the JWT — no extra DB call per request.

**Security filter chain:**

1. `TenantFilter` — extracts tenant from JWT and writes it to `TenantContext` (thread-local)
2. `JwtAuthFilter` — validates the token and populates the Spring Security `Authentication`
3. Spring Security — method-level `@PreAuthorize` checks the permission claim

---

## Tech Stack

### Backend (`smart-hospital-api/`)

| Concern | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security 6 + jjwt 0.12.6 (HMAC-SHA256) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway 10 (V1–V16) |
| Build | Maven 3 |
| API Docs | SpringDoc OpenAPI 2.6 (Swagger UI) |
| Rate Limiting | Bucket4j 8.10 |
| Metrics | Micrometer + Prometheus |
| Token Store | In-memory (dev) / Redis (prod) |
| Excel Export | Apache POI 5.2.5 (multi-sheet XLSX with styled headers) |
| PDF Export | iText 7 (KPI grids, bar charts, trend tables) |

### Frontend (`smart-hospital-web/`)

| Concern | Technology |
|---|---|
| Language | TypeScript 6 |
| Framework | React 19 |
| Build Tool | Vite 8 |
| UI Library | Ant Design 5 |
| Charts | ApexCharts 5 + react-apexcharts 2 |
| State Management | Zustand 4 (with localStorage persistence) |
| Server State | TanStack React Query 5 |
| HTTP Client | Axios 1 (with JWT interceptor + auto-refresh) |
| Routing | React Router DOM 6 |
| Styling | Tailwind CSS 3 |
| Forms | React Hook Form 7 + Zod 3 |
| Tables | TanStack Table 8 |

---

## Modules

| # | Module | Description |
|---|--------|-------------|
| 1 | **Auth** | Login, token refresh, logout, JWT issuance, RBAC |
| 2 | **Patient** | Registration, search (PostgreSQL FTS), demographics |
| 3 | **Doctor** | Specializations, doctor profiles, schedule, availability slots, photo upload |
| 4 | **OPD** | Outpatient visits, charges, prescriptions, billing |
| 5 | **Pharmacy** | Medicine catalogue, stock batches, dispensing bills |
| 6 | **IPD** | Wards, beds, admissions, daily charges, discharge |
| 7 | **Front Office** | Appointments, OPD token queue management |
| 8 | **HR** | Departments, designations, employees, attendance, leave |
| 9 | **Pathology** | Lab test catalogue, orders, sample collection, results |
| 10 | **Radiology** | Imaging modalities, studies, orders, reports |
| 11 | **Finance** | Income entries, expense entries, expense categories, dashboard |
| 12 | **Inventory** | Item categories, item catalogue, stock receipts, stock issues |
| 13 | **Blood Bank** | Donors, blood units (PENDING_TESTING → AVAILABLE lifecycle), requests, FEFO issue |
| 14 | **Operation Theatre** | OT catalogue, schedules (SCHEDULED → IN_PROGRESS → COMPLETED), post-op records, consumable deduction |
| 15 | **Setup** | Tenant provisioning, dev data seeder (dev profile only) |
| 16 | **Reports & Analytics** | Executive KPI dashboard + 7 module analytics pages, Excel and PDF export |

---

## Reports & Analytics

The analytics layer is a dedicated cross-cutting module that aggregates data from all clinical and administrative modules into interactive dashboards. Every dashboard page supports a configurable date range and renders live charts, KPI cards, and trend tables.

### Analytics Dashboards

| Dashboard | Key Metrics |
|---|---|
| **Executive Dashboard** | Revenue vs. expenses, total patients, OPD visits, bed occupancy, top-performing doctors |
| **Financial Analytics** | Revenue breakdown by category, expense trends, net income chart, month-over-month comparison |
| **Patient Analytics** | New vs. returning patients, registration trend, gender distribution, age distribution, blood group breakdown |
| **Doctor Analytics** | Consultations per doctor, revenue attribution, specialization breakdown, top-performing doctors table |
| **Appointment Analytics** | Booking trends, cancellation rate, peak hour heatmap, doctor-wise appointment counts |
| **Pharmacy Analytics** | Top-selling medicines, dispensing revenue trend, stock movement, category-wise sales |
| **Laboratory Analytics** | Test order volume, pending vs. completed ratio, revenue by test category, turnaround time |
| **Inventory Analytics** | Stock level alerts, consumption trend, category-wise usage, item reorder summary |

### Export

Every dashboard includes one-click export for both formats:

- **Excel (XLSX)** — Multi-sheet workbook matching the dashboard sections: KPI summary sheet, trend data sheet, and breakdown tables. Styled with branded headers, alternating row colors, and formatted number cells using Apache POI.
- **PDF** — Structured report with KPI grid, rendered bar chart data, and trend tables using iText 7. JWT-authenticated via the API client (no `window.open` workaround needed).

### Shared Analytics Components

| Component | Purpose |
|---|---|
| `KpiCard` | Displays a single metric with icon, value, trend indicator, and delta percentage |
| `AnalyticsFilter` | Date range picker with quick-select presets (last 7d / 30d / 90d / custom) |
| `ExportToolbar` | Buttons for Excel and PDF export with loading state |
| `EmptyChart` | Consistent empty-state placeholder when no data is available |

### Demo Mode

Append `?demo=true` to any analytics URL to force demo data regardless of the backend response. Useful for presentations or UI development without a connected backend.

---

## Prerequisites

| Tool | Minimum Version | Notes |
|------|-----------------|-------|
| Java JDK | 21 | Eclipse Temurin or Oracle JDK |
| Maven | 3.9+ | Or use `mvnw` wrapper included |
| PostgreSQL | 16 | Running locally on port 5432 |
| Node.js | 20 LTS | 22+ also works |
| npm | 10+ | Comes with Node.js 20 |
| IntelliJ IDEA | 2024.x | Recommended for backend; any IDE for frontend |

---

## Database Setup

Run the provided SQL script **once** as the PostgreSQL superuser to create the database and the first tenant schema:

```bash
psql -U postgres -f smart-hospital-api/scripts/db-setup-dev.sql
```

What it does:
1. Creates the `smarthospital` database (skips if already exists)
2. Creates the `hospital_001` schema inside it
3. Flyway will create all tables on first application startup

To add more tenant schemas:

```sql
\c smarthospital
CREATE SCHEMA IF NOT EXISTS hospital_002;
```

---

## Backend Setup

### 1. Dev configuration defaults

No additional configuration is required for local development — the `dev` profile uses:

| Setting | Dev Default |
|---------|-------------|
| DB URL | `jdbc:postgresql://localhost:5432/smarthospital` |
| DB User | `postgres` |
| DB Password | `postgres` |
| JWT Secret | `dev-secret-key-change-in-prod-min32c` |
| Token Store | In-memory |

### 2. Run from IntelliJ

1. Open the `smart-hospital-api/` directory as a Maven project
2. Edit the Run Configuration for `SmartHospitalApplication`
3. Set **Active profiles** to `dev`
4. Click **Run**

> **Note:** Do not start the backend from the terminal with `mvn spring-boot:run` — IntelliJ's dev profile injection works most reliably via the IDE Run button.

### 3. What happens on first startup

- Flyway runs all 16 migrations (`V1` through `V16`) on the `hospital_001` schema
- The `DevDataSeeder` (active only with `dev` profile) creates:
  - A super-admin user in the `public` schema
  - A tenant admin user in the `hospital_001` schema
  - Sample data for Pathology, Finance, Inventory, and Blood Bank

---

## Frontend Setup

```bash
cd smart-hospital-web
npm install
npm run dev
```

The dev server starts on **http://localhost:3000** and proxies all `/api/**` requests to `http://localhost:8080`.

```bash
npm run build    # Production build → dist/
npm run lint     # ESLint check
npm run preview  # Preview the production build locally
```

---

## Running the Application

| Service | Command / Action | URL |
|---------|-----------------|-----|
| Backend | Run `SmartHospitalApplication` in IntelliJ with `dev` profile | http://localhost:8080 |
| Frontend | `npm run dev` in `smart-hospital-web/` | http://localhost:3000 |

Open **http://localhost:3000** in your browser. The login page will appear.

---

## Default Dev Credentials

> These accounts are created automatically by `DevDataSeeder` on first startup (dev profile only).

### Tenant Admin — day-to-day testing

| Field | Value |
|-------|-------|
| Email | `admin@hospital001.com` |
| Password | `Admin@1234` |
| Tenant ID | `hospital_001` |
| Access | All modules |

### Super Admin — platform operations only

| Field | Value |
|-------|-------|
| Email | `superadmin@smarthospital.com` |
| Password | `SuperAdmin@1234` |
| Tenant ID | *(leave blank)* |
| Access | `POST /api/platform/tenants` only |

---

## API Documentation

With the backend running, open:

**http://localhost:8080/swagger-ui.html**

All endpoints are grouped by module tag. The Swagger UI includes request/response schemas for every endpoint and supports try-it-out with JWT authorization.

Raw OpenAPI spec: **http://localhost:8080/v3/api-docs**

---

## Project Structure

```
SmartHospital/
├── Documents/
│   └── SmartHospital_Architecture_Report.docx   ← Full architecture document
├── smart-hospital-api/                           ← Spring Boot backend
│   ├── pom.xml
│   ├── scripts/
│   │   └── db-setup-dev.sql                     ← One-time DB init script
│   └── src/main/java/com/smarthospital/
│       ├── SmartHospitalApplication.java
│       ├── core/
│       │   ├── audit/          ← AuditEntity base class (createdAt/updatedAt/createdBy/updatedBy)
│       │   ├── config/         ← SecurityConfig, JpaConfig, OpenApiConfig, TomcatServerConfig
│       │   ├── exception/      ← ApiException, GlobalExceptionHandler, ErrorResponse
│       │   ├── export/         ← ExcelExportUtil (Apache POI), PdfExportUtil (iText 7)
│       │   ├── security/       ← JwtTokenProvider, JwtAuthFilter, RbacEvaluator
│       │   ├── tenant/         ← TenantContext, TenantFilter, TenantAwareDataSource
│       │   ├── token/          ← RefreshTokenStore, InMemoryRefreshTokenStore
│       │   ├── notification/   ← NotificationService
│       │   └── pagination/     ← PageResponse wrapper
│       └── modules/
│           ├── analytics/      ← AnalyticsController, ExecutiveDashboardService, analytics DTOs
│           ├── auth/           ← Login, refresh, logout, JWT, RBAC permissions
│           ├── patient/        ← Patient CRUD, full-text search, PatientAnalyticsService
│           ├── doctor/         ← Specializations, profiles, schedules, availability, photo upload
│           ├── opd/            ← OPD visits, charges, prescriptions
│           ├── pharmacy/       ← Medicines, batches, bills, PharmacyAnalyticsService
│           ├── ipd/            ← Wards, beds, admissions, charges, discharge
│           ├── frontoffice/    ← Appointments, token queue, AppointmentAnalyticsService
│           ├── hr/             ← Departments, employees, attendance, leave
│           ├── pathology/      ← Lab categories, tests, orders, results, PathologyAnalyticsService
│           ├── radiology/      ← Modalities, studies, orders, reports
│           ├── finance/        ← Expenses, income, FinanceAnalyticsService
│           ├── inventory/      ← Items, categories, receipts, issues, InventoryAnalyticsService
│           ├── bloodbank/      ← Donors, units, requests, FEFO issue
│           ├── operation/      ← OT theatres, schedules, post-op, consumables
│           └── setup/          ← Tenant provisioning, DevDataSeeder
│
└── smart-hospital-web/                          ← React frontend
    ├── vite.config.ts
    ├── package.json
    └── src/
        ├── api/                ← Axios API modules (one per backend module, + analytics.api.ts)
        ├── components/
        │   ├── analytics/      ← KpiCard, AnalyticsFilter, ExportToolbar, EmptyChart, chartTheme
        │   └── common/         ← AppLayout, PageHeader, PatientSearchSelect, PhotoUpload
        ├── hooks/              ← TanStack React Query hooks (one per module)
        ├── pages/
        │   ├── analytics/      ← 8 dashboard pages (Executive, Financial, Patient, Doctor,
        │   │                      Appointment, Pharmacy, Laboratory, Inventory)
        │   ├── auth/
        │   ├── dashboard/
        │   ├── doctors/
        │   ├── patients/
        │   ├── opd/
        │   ├── pharmacy/
        │   ├── ipd/
        │   ├── frontoffice/
        │   ├── hr/
        │   ├── pathology/
        │   ├── radiology/
        │   ├── finance/
        │   ├── inventory/
        │   ├── bloodbank/
        │   ├── operation/
        │   └── reports/
        ├── router/             ← React Router config, PrivateRoute guard, 8 analytics routes
        ├── store/              ← Zustand stores (authStore, uiStore)
        ├── types/              ← TypeScript types (one file per module)
        └── utils/
```

---

## Flyway Migration History

| Version | Description |
|---------|-------------|
| V1 | Tenant registry (`public.tenants`, `public.super_admin_users`) |
| V2 | Patient tables (with PostgreSQL FTS `tsvector` index) |
| V3 | Pharmacy tables (medicines, categories, batches, bills) |
| V4 | Users table |
| V5 | OPD tables (visits, charges, prescriptions, prescription items) |
| V6 | Audit columns on medicine batches |
| V7 | Patient name columns on pharmacy bills |
| V8 | IPD tables (wards, beds, admissions, daily charges) |
| V9 | Front Office tables (appointments, OPD tokens) |
| V10 | HR tables (departments, designations, employees, attendance, leave) |
| V11 | Pathology tables (categories, tests, orders, samples, results) |
| V12 | Radiology tables (modalities, studies, orders, reports) |
| V13 | Finance tables (income, expenses, expense categories) |
| V14 | Inventory tables (item categories, items, receipts, issues) |
| V15 | Blood Bank tables (donors, blood units, requests) |
| V16 | Operation Theatre tables (theatres, schedules, post-op, consumables) |

---

## Environment Variables

| Variable | Dev Default | Description |
|----------|-------------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/smarthospital` | JDBC connection URL |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | `dev-secret-key-change-in-prod-min32c` | HMAC signing secret (≥ 32 chars enforced at startup) |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed CORS origins |

For production, set these via OS environment variables or a secrets manager. **Never commit production secrets.**

---

## Roles & Permissions

Each JWT token carries both a `role` claim and a granular `permissions` claim. The backend uses `@PreAuthorize("hasAuthority('MODULE.ACTION')")` for method-level access control — roles determine the permission set granted at login.

### Roles

| Role | Primary Scope |
|------|---------------|
| `SUPER_ADMIN` | Platform management (tenant provisioning) only |
| `ADMIN` | Full access to all modules within their tenant |
| `DOCTOR` | Patient, OPD, IPD, Pathology, Radiology, Operation Theatre |
| `NURSE` | Patient (read), OPD (read), Pathology/Radiology (read), OT (read) |
| `PHARMACIST` | Patient (read), Pharmacy |
| `RECEPTIONIST` | Patient (create/read), Front Office |
| `ACCOUNTANT` | Finance, Reports |
| `PATHOLOGIST` | Patient (read), Pathology (full) |
| `RADIOLOGIST` | Patient (read), Radiology (full) |
| `PATIENT` | Own records only |

### Permission Format

Permissions follow `MODULE.ACTION`:

```
PATIENT.VIEW      PATIENT.CREATE     PATIENT.EDIT
DOCTOR.VIEW       DOCTOR.MANAGE
OPD.VIEW          OPD.CREATE         OPD.EDIT
PHARMACY.VIEW     PHARMACY.CREATE
IPD.VIEW          IPD.CREATE         IPD.EDIT         IPD.MANAGE
HR.VIEW           HR.CREATE          HR.EDIT          HR.MANAGE
FRONTOFFICE.VIEW  FRONTOFFICE.CREATE FRONTOFFICE.EDIT
PATHOLOGY.VIEW    PATHOLOGY.CREATE   PATHOLOGY.EDIT   PATHOLOGY.MANAGE
RADIOLOGY.VIEW    RADIOLOGY.CREATE   RADIOLOGY.EDIT   RADIOLOGY.MANAGE
FINANCE.VIEW      FINANCE.CREATE     FINANCE.MANAGE
INVENTORY.VIEW    INVENTORY.CREATE   INVENTORY.MANAGE
BLOODBANK.VIEW    BLOODBANK.CREATE   BLOODBANK.EDIT
OPERATION.VIEW    OPERATION.CREATE   OPERATION.EDIT   OPERATION.MANAGE
REPORTS.VIEW
```

`SUPER_ADMIN` receives a wildcard `*` permission that bypasses all checks.

---

## Security Notes

- **JWT secret** — Change `JWT_SECRET` before any non-local deployment. A secret shorter than 32 characters causes a hard startup failure (`IllegalStateException`).
- **Token lifetimes** — Access tokens expire in **15 minutes**; refresh tokens expire in **7 days**. The frontend auto-refreshes on every app load and on 401 responses via the Axios interceptor.
- **HTTPS** — Always run behind TLS in production. The Vite proxy and CORS config assume `localhost` for development only.
- **Tenant isolation** — Every query executes with `search_path = <tenantId>` set at the JDBC level. Cross-tenant data access is architecturally prevented.
- **No Hibernate auto-DDL** — `ddl-auto: validate` is set in all profiles. Schema changes go through Flyway only.
- **Export authentication** — Analytics Excel/PDF export endpoints require a valid JWT. The frontend uses the Axios API client (blob response type) rather than `window.open` to ensure the Authorization header is sent.
- **Production profile** — `application-prod.yml` is excluded from version control. Create it separately with production credentials and JWT secret.
