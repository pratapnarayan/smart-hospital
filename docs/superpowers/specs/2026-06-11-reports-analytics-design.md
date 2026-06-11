# Reports & Analytics Module — Design Spec
**Date:** 2026-06-11  
**Branch:** feature/report-and-analytics  
**Status:** Approved — ready for implementation

---

## 1. Scope

Implement a world-class **Reports & Analytics** module for SmartHospital HMS. This is a primary USP showcased during client demos and must deliver an enterprise healthcare intelligence platform feel.

**In scope (Phase 1):**
- 8 analytical dashboards with KPI cards, ApexCharts visualisations, date-range filters, and PDF/Excel export
- Executive Dashboard as the default landing page aggregating all modules
- Demo Mode (frontend mock data when API returns empty/zero values)
- Role-based access control integrated with existing RBAC system
- New sidebar menu group in AppLayout

**Out of scope (Phase 2 — documented below):**
- Custom Report Builder
- Scheduled email reports

**Non-negotiable:** Zero changes to existing module behaviour. All existing endpoints, services, and UI pages remain untouched.

---

## 2. Technology Decisions

| Concern | Choice | Rationale |
|---|---|---|
| Chart library | `react-apexcharts` + `apexcharts` | Best visual impact (animations, heatmaps, gradients) for demo requirements; ~400KB |
| PDF export | Apache iText (backend) | Mature, handles hospital branding, headers/footers |
| Excel export | Apache POI (backend) | Standard; already common in Spring ecosystem |
| Demo data | Frontend mock data | No DB pollution risk; demo always works offline |
| API structure | Module-scoped analytics endpoints under `/api/analytics/` | Matches existing module pattern |

---

## 3. Backend Architecture

### 3.1 New Analytics Services

One `*AnalyticsService` added per module. These query **existing JPA repositories only** — no schema changes.

| Service | Module package | Primary data sources |
|---|---|---|
| `FinanceAnalyticsService` | `modules.finance` | `IncomeEntryRepository`, `ExpenseEntryRepository` |
| `PatientAnalyticsService` | `modules.patient` | `PatientRepository` |
| `DoctorAnalyticsService` | `modules.doctor` | `DoctorProfileRepository`, `AppointmentRepository` |
| `AppointmentAnalyticsService` | `modules.frontoffice` | `AppointmentRepository` |
| `PharmacyAnalyticsService` | `modules.pharmacy` | `PharmacySaleRepository`, `StockRepository` |
| `PathologyAnalyticsService` | `modules.pathology` | `LabOrderRepository` |
| `InventoryAnalyticsService` | `modules.inventory` | `InventoryItemRepository`, `StockMovementRepository` |

### 3.2 Executive Aggregator

`ExecutiveDashboardService` in `modules.analytics` fans out to all 7 analytics services using `CompletableFuture.allOf(...)` and assembles `ExecutiveDashboardResponse`. This is the **only** class that crosses module boundaries.

### 3.3 New API Endpoints

Single `AnalyticsController` at `/api/analytics/`. All endpoints accept `?from=YYYY-MM-DD&to=YYYY-MM-DD` params. All require `ANALYTICS.VIEW` permission.

```
GET  /api/analytics/executive           → ExecutiveDashboardResponse
GET  /api/analytics/finance             → FinanceAnalyticsResponse
GET  /api/analytics/patients            → PatientAnalyticsResponse
GET  /api/analytics/doctors             → DoctorAnalyticsResponse
GET  /api/analytics/appointments        → AppointmentAnalyticsResponse
GET  /api/analytics/pharmacy            → PharmacyAnalyticsResponse
GET  /api/analytics/laboratory          → LaboratoryAnalyticsResponse
GET  /api/analytics/inventory           → InventoryAnalyticsResponse
GET  /api/analytics/export/{section}    → Binary (PDF or Excel)
     query params: ?format=pdf|excel&from=&to=
```

### 3.4 Export Utilities

New `core/export/` package:
- `ExcelExportUtil` — Apache POI, produces branded `.xlsx` with hospital name, timestamp, generated-by, header row styling
- `PdfExportUtil` — iText, produces branded `.pdf` with logo placeholder, header, footer, page numbers

### 3.5 Security — Role to Permission Mapping

New permission `ANALYTICS.VIEW` added to `Permission` enum. Role assignments:

| Role | Sections accessible |
|---|---|
| `SUPER_ADMIN`, `ADMIN` | All sections |
| `ACCOUNTANT` | Finance only |
| `DOCTOR` | Doctor + Appointment |
| `RECEPTIONIST` | Appointment only |
| `NURSE`, `PHARMACIST`, `PATHOLOGIST` | No access (no analytics permission) |

---

## 4. Frontend Architecture

### 4.1 New Files

```
src/
├── api/
│   └── analytics.api.ts              # 9 API call functions
├── hooks/
│   └── useAnalytics.ts               # React Query hooks (one per endpoint)
├── types/
│   └── analytics.types.ts            # All response type definitions
├── pages/
│   └── analytics/
│       ├── ExecutiveDashboardPage.tsx
│       ├── FinancialAnalyticsPage.tsx
│       ├── PatientAnalyticsPage.tsx
│       ├── DoctorAnalyticsPage.tsx
│       ├── AppointmentAnalyticsPage.tsx
│       ├── PharmacyAnalyticsPage.tsx
│       ├── LaboratoryAnalyticsPage.tsx
│       └── InventoryAnalyticsPage.tsx
└── components/
    └── analytics/
        ├── KpiCard.tsx               # Value + trend arrow + % change + colour
        ├── AnalyticsFilter.tsx       # Date range + doctor/dept filters → URL params
        ├── EmptyChart.tsx            # Friendly empty state illustration
        └── ExportToolbar.tsx         # PDF / Excel / Print + Demo badge
```

### 4.2 Chart Theme

`src/components/analytics/chartTheme.ts` sets global ApexCharts defaults:
- Primary colour: `#1677ff` (Ant Design blue)
- Font: system-ui (matches Ant Design)
- Grid: subtle `#f0f2f5`
- Animations: `easing: 'easeinout'`, duration 600ms — subtle and professional

### 4.3 Demo Mode

`src/hooks/useDemoData.ts` — a utility that accepts a real API response and returns either the real data (if non-zero values present) or pre-baked mock data of the same type. Each analytics page passes its API response through this hook. When mock data is active, `ExportToolbar` renders an amber **Demo Data** badge.

### 4.4 Filters

`AnalyticsFilter.tsx` syncs state to URL query params (`?from=&to=&doctor=&department=`). React Query hooks read from URL params, so filters are bookmarkable. Default range: last 30 days.

### 4.5 Sidebar

`AppLayout.tsx` gains a new `Reports & Analytics` menu group (`BarChartOutlined` icon) with 8 submenus. Visibility gated on `ANALYTICS.VIEW` permission check matching the pattern used by other menu items.

### 4.6 Routing

8 new routes added under the existing `PrivateRoute` + `AppLayout` wrapper, all requiring `ANALYTICS.VIEW` permission:

```
/analytics/executive
/analytics/financial
/analytics/patients
/analytics/doctors
/analytics/appointments
/analytics/pharmacy
/analytics/laboratory
/analytics/inventory
```

Default redirect: `/analytics` → `/analytics/executive`

---

## 5. Per-Dashboard Specification

### 5.1 Executive Dashboard (`/analytics/executive`)

**Section 1 — KPI Cards (10 cards, 5 per row on desktop)**
Each card: large value, trend arrow (▲/▼), % change vs previous period, colour (green ≥0%, red <0%, amber within 2%).

| Card | Source |
|---|---|
| Today's Revenue | FinanceAnalytics |
| Month Revenue | FinanceAnalytics |
| Total Patients | PatientAnalytics |
| Today's Appointments | AppointmentAnalytics |
| Pending Payments | FinanceAnalytics |
| Doctors Available Today | DoctorAnalytics |
| Current Admissions (IPD) | IPD count |
| Lab Tests Today | PathologyAnalytics |
| Medicine Sales Today | PharmacyAnalytics |
| Inventory Alerts | InventoryAnalytics |

**Section 2 — Revenue Trend:** Line chart, last 30 days daily, toggle: Today/Week/Month/Quarter/Year. Smooth curve, tooltips with ₹ formatting.

**Section 3 — Patient Growth:** Area chart, New vs Returning patients, monthly, growth % annotation.

**Section 4 — Revenue Distribution:** Donut chart — OPD / IPD / Pharmacy / Lab / Radiology / Other. Interactive legend.

**Section 5 — Top Performing Doctors:** Horizontal bar, top 10 by revenue. Shows doctor name + revenue + patients seen.

**Section 6 — Department Performance:** Grouped bar — Revenue + Patients per department.

**Section 7 — Appointment Insights:** 4 mini stat cards (Completed / Cancelled / No Show / Rescheduled) + small trend sparkline.

**Section 8 — Quick Actions:** Export Dashboard (PDF), Download Excel, Generate Custom Report (disabled, Phase 2), Schedule Email (disabled, Phase 2).

---

### 5.2 Financial Analytics (`/analytics/financial`)
- Daily revenue line chart (30/90/365 day toggle)
- Revenue by source donut (OPD/IPD/Pharmacy/Lab/Radiology)
- Revenue by doctor horizontal bar (top 10)
- Monthly comparison stacked bar (income vs expense)
- Collection efficiency gauge (0–100%)

### 5.3 Patient Analytics (`/analytics/patients`)
- Monthly registration trend area chart
- Gender distribution pie
- Age pyramid (mirrored horizontal bar, 10-year buckets)
- Top cities horizontal bar
- Department distribution donut
- New vs returning stacked bar

### 5.4 Doctor Analytics (`/analytics/doctors`)
- Leaderboard cards (top 10, ranked with medal icons)
- Revenue per doctor bar
- Appointments completed per doctor bar
- Utilization % gauge (per doctor)
- Follow-up rate trend line

### 5.5 Appointment Analytics (`/analytics/appointments`)
- Status breakdown donut
- Peak hours heatmap (hour 0–23 × Mon–Sun)
- Daily trend line
- Doctor-wise bar
- Department-wise bar

### 5.6 Pharmacy Analytics (`/analytics/pharmacy`)
- Top 10 medicines by revenue horizontal bar
- Stock health donut (In Stock / Low / Out of Stock)
- Category revenue bar
- Expiry alerts count cards
- Inventory turnover line

### 5.7 Laboratory Analytics (`/analytics/laboratory`)
- Daily tests trend area
- Top 10 tests horizontal bar
- Revenue trend line
- Pending vs completed donut
- Department referral bar

### 5.8 Inventory Analytics (`/analytics/inventory`)
- Stock value trend area
- Low stock items table with alert badges
- Fast vs slow movers horizontal bar
- Vendor performance bar
- Purchase trend line

---

## 6. Empty States

When real data is empty and demo mode is off (e.g. fresh install), each chart renders `EmptyChart.tsx` — a centred illustration with a friendly message like `"No data found for selected period."` No broken chart frames, no zero-value axes.

---

## 7. Performance Considerations

- All analytics queries use `@Query` JPQL with date-range params — no full table scans
- `ExecutiveDashboardService` uses `CompletableFuture` fan-out for parallel module queries
- React Query cache time: 5 minutes for analytics data (stale-while-revalidate)
- Export endpoints stream binary response directly — no in-memory buffering of full datasets

---

## 8. Phase 2 — Future Extension Points

### 8.1 Custom Report Builder
**Documented stub:** `pages/analytics/CustomReportPage.tsx` scaffold with `// TODO: Phase 2 — Custom Report Builder` comment. Backend stub `GET /api/analytics/custom` returns `501 Not Implemented`.

**Intended design (Phase 2):** User selects Module → Fields → Filters → Grouping → Generate. Backend dynamically builds JPQL from a `ReportDefinition` DTO. Frontend uses a multi-step wizard with a drag-and-drop field selector (candidate library: `@dnd-kit/core`).

### 8.2 Scheduled Reports
**Documented stub:** `ScheduledReportConfig.java` JPA entity (commented out) in `modules.analytics.domain`. Fields: `cronExpression`, `reportType`, `recipientEmails`, `format`, `tenantId`.

**Intended design (Phase 2):** Spring `@Scheduled` task reads active `ScheduledReportConfig` rows, calls existing export endpoints, and delivers via Spring Mail (`spring-boot-starter-mail`). UI in `pages/analytics/ScheduledReportsPage.tsx`.

---

## 9. File Change Summary

### Backend — New Files
```
modules/analytics/AnalyticsController.java
modules/analytics/ExecutiveDashboardService.java
modules/analytics/dto/ExecutiveDashboardResponse.java
modules/analytics/dto/FinanceAnalyticsResponse.java
modules/analytics/dto/PatientAnalyticsResponse.java
modules/analytics/dto/DoctorAnalyticsResponse.java
modules/analytics/dto/AppointmentAnalyticsResponse.java
modules/analytics/dto/PharmacyAnalyticsResponse.java
modules/analytics/dto/LaboratoryAnalyticsResponse.java
modules/analytics/dto/InventoryAnalyticsResponse.java
modules/finance/service/FinanceAnalyticsService.java
modules/patient/service/PatientAnalyticsService.java
modules/doctor/service/DoctorAnalyticsService.java
modules/frontoffice/service/AppointmentAnalyticsService.java
modules/pharmacy/service/PharmacyAnalyticsService.java
modules/pathology/service/PathologyAnalyticsService.java
modules/inventory/service/InventoryAnalyticsService.java
core/export/ExcelExportUtil.java
core/export/PdfExportUtil.java
```

### Backend — Modified Files
```
modules/auth/domain/Permission.java         (add ANALYTICS.VIEW)
pom.xml                                     (add Apache POI + iText)
```

### Frontend — New Files
```
src/api/analytics.api.ts
src/hooks/useAnalytics.ts
src/hooks/useDemoData.ts
src/types/analytics.types.ts
src/components/analytics/KpiCard.tsx
src/components/analytics/AnalyticsFilter.tsx
src/components/analytics/EmptyChart.tsx
src/components/analytics/ExportToolbar.tsx
src/components/analytics/chartTheme.ts
src/pages/analytics/ExecutiveDashboardPage.tsx
src/pages/analytics/FinancialAnalyticsPage.tsx
src/pages/analytics/PatientAnalyticsPage.tsx
src/pages/analytics/DoctorAnalyticsPage.tsx
src/pages/analytics/AppointmentAnalyticsPage.tsx
src/pages/analytics/PharmacyAnalyticsPage.tsx
src/pages/analytics/LaboratoryAnalyticsPage.tsx
src/pages/analytics/InventoryAnalyticsPage.tsx
src/pages/analytics/CustomReportPage.tsx    (Phase 2 stub)
```

### Frontend — Modified Files
```
src/router/index.tsx                        (8 new routes)
src/components/common/AppLayout.tsx         (new sidebar group)
package.json                                (add react-apexcharts + apexcharts)
```
