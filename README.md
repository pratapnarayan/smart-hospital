# SmartHospital

A full-stack, modular-monolith Hospital Management System built with **Spring Boot 3** and **React 19**.  
Designed as a production-grade replacement for legacy CodeIgniter/MariaDB hospital software.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Modules](#modules)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [Running the Application](#running-the-application)
- [Default Dev Credentials](#default-dev-credentials)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)
- [Roles & Permissions](#roles--permissions)
- [Security Notes](#security-notes)

---

## Overview

SmartHospital is a comprehensive Hospital Management System covering the full clinical and administrative workflow of a hospital — from patient registration and outpatient consultations, through inpatient admissions, pharmacy, pathology, radiology, HR, finance, inventory, blood bank, and operation theatre management.

**Key design goals:**
- Modular Monolith MVP (designed for eventual microservices extraction)
- Schema-per-tenant multi-tenancy (one PostgreSQL database, one schema per hospital)
- Stateless JWT authentication with role-based and permission-based access control
- All database schema managed by Flyway — no Hibernate auto-DDL ever

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
│  Modules (each owns its own domain/dto/repo/service/controller): │
│  auth · patient · opd · pharmacy · ipd · frontoffice · hr        │
│  pathology · radiology · finance · inventory · bloodbank         │
│  operation · setup                                               │
└────────────────────────────┬────────────────────────────────────┘
                             │ JDBC (search_path per tenant)
┌────────────────────────────▼────────────────────────────────────┐
│                  PostgreSQL 16                                    │
│   public schema  →  tenant registry + super-admin user           │
│   hospital_001   →  all clinical & operational tables            │
│   hospital_002   →  (next tenant, same tables, separate data)    │
└─────────────────────────────────────────────────────────────────┘
```

**Multi-tenancy** is implemented via a `TenantAwareDataSource` JDBC proxy that sets `search_path = <tenantId>` on every connection before executing queries. The tenant is resolved from the JWT `tenant_id` claim on every request.

---

## Tech Stack

### Backend (`smart-hospital-api/`)

| Concern            | Technology                                   |
|--------------------|----------------------------------------------|
| Language           | Java 21                                      |
| Framework          | Spring Boot 3.3.5                            |
| Security           | Spring Security 6 + jjwt 0.12.6 (HMAC-SHA256) |
| Persistence        | Spring Data JPA + Hibernate                  |
| Database           | PostgreSQL 16                                |
| Migrations         | Flyway 10 (V1–V16)                           |
| Build              | Maven 3                                      |
| API Docs           | SpringDoc OpenAPI 2.6 (Swagger UI)           |
| Rate Limiting      | Bucket4j 8.10                                |
| Metrics            | Micrometer + Prometheus                      |
| Token Store        | In-memory (dev) / Redis (prod)               |

### Frontend (`smart-hospital-web/`)

| Concern            | Technology                                   |
|--------------------|----------------------------------------------|
| Language           | TypeScript 6                                 |
| Framework          | React 19                                     |
| Build Tool         | Vite 8                                       |
| UI Library         | Ant Design 5                                 |
| State Management   | Zustand 4 (with localStorage persistence)    |
| Server State       | TanStack React Query 5                       |
| HTTP Client        | Axios 1 (with JWT interceptor + auto-refresh)|
| Routing            | React Router DOM 6                           |
| Styling            | Tailwind CSS 3                               |
| Forms              | React Hook Form 7 + Zod 3                   |

---

## Modules

| # | Module | Description |
|---|--------|-------------|
| 1 | **Auth** | Login, token refresh, logout, JWT issuance, RBAC |
| 2 | **Patient** | Registration, search (PostgreSQL FTS), demographics |
| 3 | **OPD** | Outpatient visits, charges, prescriptions, billing |
| 4 | **Pharmacy** | Medicine catalogue, stock batches, dispensing bills |
| 5 | **IPD** | Wards, beds, admissions, daily charges, discharge |
| 6 | **Front Office** | Appointments, OPD token queue management |
| 7 | **HR** | Departments, designations, employees, attendance, leave |
| 8 | **Pathology** | Lab test catalogue, orders, sample collection, results |
| 9 | **Radiology** | Imaging modalities, studies, orders, reports |
| 10 | **Finance** | Income entries, expense entries, expense categories, dashboard |
| 11 | **Inventory** | Item categories, item catalogue, stock receipts, stock issues |
| 12 | **Blood Bank** | Donors, blood units (PENDING_TESTING → AVAILABLE lifecycle), requests, FEFO issue |
| 13 | **Operation Theatre** | OT catalogue, schedules (SCHEDULED → IN_PROGRESS → COMPLETED), post-op records, consumable deduction |
| 14 | **Setup** | Tenant provisioning, dev data seeder (dev profile only) |

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

If you need additional tenant schemas, create them manually:

```sql
\c smarthospital
CREATE SCHEMA IF NOT EXISTS hospital_002;
```

---

## Backend Setup

### 1. Clone and configure

No additional configuration is required for local development — the `dev` profile uses sensible defaults:

| Setting | Dev Default |
|---------|-------------|
| DB URL | `jdbc:postgresql://localhost:5432/smarthospital` |
| DB User | `postgres` |
| DB Password | `postgres` |
| JWT Secret | `dev-secret-key-change-in-prod-min32c` |
| Token Store | In-memory |

To override any default, create `smart-hospital-api/src/main/resources/.env` or set environment variables (see [Environment Variables](#environment-variables)).

### 2. Run from IntelliJ

1. Open the `smart-hospital-api/` directory as a Maven project
2. Edit the Run Configuration for `SmartHospitalApplication`
3. Set **Active profiles** to `dev`
4. Click **Run**

> **Note:** Do not start the backend from the terminal with `mvn spring-boot:run` as IntelliJ's dev profile injection works most reliably via the IDE Run button.

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

Other commands:

```bash
npm run build    # Production build → dist/
npm run lint     # ESLint check
npm run preview  # Preview the production build locally
```

---

## Running the Application

Start **both** services:

| Service | Command / Action | URL |
|---------|-----------------|-----|
| Backend | Run `SmartHospitalApplication` in IntelliJ with `dev` profile | http://localhost:8080 |
| Frontend | `npm run dev` in `smart-hospital-web/` | http://localhost:3000 |

Open **http://localhost:3000** in your browser. The login page will appear with dev credentials pre-filled.

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

All endpoints are grouped by module. The Swagger UI includes request/response schemas for every endpoint.

The raw OpenAPI spec is available at: **http://localhost:8080/v3/api-docs**

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
│       │   ├── audit/                           ← AuditEntity base class
│       │   ├── config/                          ← SecurityConfig, JpaConfig, etc.
│       │   ├── exception/                       ← ApiException, GlobalExceptionHandler
│       │   ├── security/                        ← JwtTokenProvider, JwtAuthFilter, RbacEvaluator
│       │   ├── tenant/                          ← TenantContext, TenantFilter, TenantAwareDataSource
│       │   └── pagination/                      ← PageResponse wrapper
│       └── modules/
│           ├── auth/                            ← Login, refresh, logout, JWT, RBAC permissions
│           ├── patient/                         ← Patient CRUD + full-text search
│           ├── opd/                             ← OPD visits, charges, prescriptions, billing
│           ├── pharmacy/                        ← Medicines, batches, bills, stock
│           ├── ipd/                             ← Wards, beds, admissions, charges, discharge
│           ├── frontoffice/                     ← Appointments, token queue
│           ├── hr/                              ← Departments, employees, attendance, leave
│           ├── pathology/                       ← Lab categories, tests, orders, results
│           ├── radiology/                       ← Modalities, studies, orders, reports
│           ├── finance/                         ← Expenses, income, dashboard
│           ├── inventory/                       ← Items, categories, stock receipts/issues
│           ├── bloodbank/                       ← Donors, units, requests, FEFO issue
│           ├── operation/                       ← OT theatres, schedules, post-op, consumables
│           └── setup/                           ← Tenant provisioning, DevDataSeeder
│
└── smart-hospital-web/                          ← React frontend
    ├── vite.config.ts
    ├── package.json
    └── src/
        ├── api/                                 ← Axios API modules (one per backend module)
        ├── components/
        │   └── common/                          ← AppLayout, PageHeader, shared UI
        ├── hooks/                               ← React Query hooks (one per module)
        ├── pages/                               ← Page components (mirrors module structure)
        │   ├── auth/
        │   ├── dashboard/
        │   ├── patients/
        │   ├── opd/
        │   ├── pharmacy/
        │   ├── ipd/
        │   ├── frontoffice/
        │   ├── hr/
        │   ├── pathology/
        │   ├── finance/
        │   ├── inventory/
        │   ├── bloodbank/
        │   └── operation/
        ├── router/                              ← React Router config + PrivateRoute guard
        ├── store/                               ← Zustand stores (auth, ui)
        └── types/                               ← TypeScript types (one file per module)
```

### Flyway Migration History

| Version | Description |
|---------|-------------|
| V1 | Tenant registry |
| V2 | Patient tables |
| V3 | Pharmacy tables |
| V4 | Users table |
| V5 | OPD tables |
| V6 | Audit columns on medicine batches |
| V7 | Patient name on pharmacy bills |
| V8 | IPD tables (wards, beds, admissions) |
| V9 | Front Office tables (appointments, tokens) |
| V10 | HR tables (departments, employees, attendance, leave) |
| V11 | Pathology tables (categories, tests, orders, results) |
| V12 | Radiology tables (modalities, studies, orders, reports) |
| V13 | Finance tables (income, expenses, categories) |
| V14 | Inventory tables (items, categories, receipts, issues) |
| V15 | Blood Bank tables (donors, units, requests) |
| V16 | Operation Theatre tables (theatres, schedules, consumables) |

---

## Environment Variables

The backend reads the following environment variables (with dev defaults shown):

| Variable | Dev Default | Description |
|----------|-------------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/smarthospital` | JDBC connection URL |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | `dev-secret-key-change-in-prod-min32c` | HMAC signing secret (≥ 32 chars) |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed CORS origins |

For production, set these via OS environment variables or a secrets manager. **Never commit production secrets.**

---

## Roles & Permissions

Each JWT token carries both a `roles` claim and a granular `permissions` claim. The backend uses Spring Security's `@PreAuthorize("hasPermission(null, 'MODULE.ACTION')")` for method-level access control.

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

### Permission format

Permissions follow the pattern `MODULE.ACTION`:

```
PATIENT.VIEW      PATIENT.CREATE     PATIENT.EDIT
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

- **JWT secret** — Change `JWT_SECRET` before any non-local deployment. The secret must be at least 32 characters.
- **Token lifetimes** — Access tokens expire in **15 minutes**; refresh tokens expire in **7 days**. The frontend auto-refreshes on every app load and on 401 responses.
- **HTTPS** — Always run behind TLS in production. The Vite proxy and CORS config assume `localhost` for development only.
- **Tenant isolation** — Every query executes with `search_path = <tenantId>` set at the JDBC level. Cross-tenant data access is architecturally prevented.
- **No Hibernate auto-DDL** — `ddl-auto: validate` is set in all profiles. Schema changes go through Flyway only.
- **Production profile** — The `application-prod.yml` file is excluded from version control. Create it separately with production database credentials and JWT secret.
