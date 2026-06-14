import { createBrowserRouter, Navigate } from 'react-router-dom'
import { PrivateRoute } from './PrivateRoute'
import { AppLayout } from '@/components/common/AppLayout'
import { LoginPage } from '@/pages/auth/LoginPage'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { PatientListPage } from '@/pages/patients/PatientListPage'
import { PatientDetailPage } from '@/pages/patients/PatientDetailPage'
import { OpdListPage } from '@/pages/opd/OpdListPage'
import { OpdVisitPage } from '@/pages/opd/OpdVisitPage'
import { StockPage } from '@/pages/pharmacy/StockPage'
import { PharmacyBillPage } from '@/pages/pharmacy/PharmacyBillPage'
import { IpdListPage } from '@/pages/ipd/IpdListPage'
import { IpdAdmissionPage } from '@/pages/ipd/IpdAdmissionPage'
import { AppointmentListPage } from '@/pages/frontoffice/AppointmentListPage'
import { TokenQueuePage } from '@/pages/frontoffice/TokenQueuePage'
import { EmployeeListPage } from '@/pages/hr/EmployeeListPage'
import { EmployeeDetailPage } from '@/pages/hr/EmployeeDetailPage'
import { AttendancePage } from '@/pages/hr/AttendancePage'
import { LeaveRequestPage } from '@/pages/hr/LeaveRequestPage'
import { PathologyListPage } from '@/pages/pathology/PathologyListPage'
import { LabOrderPage } from '@/pages/pathology/LabOrderPage'
import { FinanceDashboardPage } from '@/pages/finance/FinanceDashboardPage'
import { IncomeListPage } from '@/pages/finance/IncomeListPage'
import { ExpenseListPage } from '@/pages/finance/ExpenseListPage'
import { InventoryDashboardPage } from '@/pages/inventory/InventoryDashboardPage'
import { ItemListPage } from '@/pages/inventory/ItemListPage'
import { StockMovementPage } from '@/pages/inventory/StockMovementPage'
import { BloodBankDashboardPage } from '@/pages/bloodbank/BloodBankDashboardPage'
import { BloodUnitListPage } from '@/pages/bloodbank/BloodUnitListPage'
import { BloodRequestListPage } from '@/pages/bloodbank/BloodRequestListPage'
import { OtDashboardPage } from '@/pages/operation/OtDashboardPage'
import { OtScheduleListPage } from '@/pages/operation/OtScheduleListPage'
import { OtScheduleDetailPage } from '@/pages/operation/OtScheduleDetailPage'
import { RadiologyDashboardPage } from '@/pages/radiology/RadiologyDashboardPage'
import { RadiologyOrdersPage } from '@/pages/radiology/RadiologyOrdersPage'
import { DoctorDirectoryPage } from '@/pages/doctors/DoctorDirectoryPage'
import { DoctorProfilePage } from '@/pages/doctors/DoctorProfilePage'
import { SpecializationsPage } from '@/pages/doctors/SpecializationsPage'
import { DoctorSchedulePage } from '@/pages/doctors/DoctorSchedulePage'
import { ExecutiveDashboardPage } from '@/pages/analytics/ExecutiveDashboardPage'
import { FinancialAnalyticsPage } from '@/pages/analytics/FinancialAnalyticsPage'
import { PatientAnalyticsPage } from '@/pages/analytics/PatientAnalyticsPage'
import { DoctorAnalyticsPage } from '@/pages/analytics/DoctorAnalyticsPage'
import { AppointmentAnalyticsPage } from '@/pages/analytics/AppointmentAnalyticsPage'
import { PharmacyAnalyticsPage } from '@/pages/analytics/PharmacyAnalyticsPage'
import { LaboratoryAnalyticsPage } from '@/pages/analytics/LaboratoryAnalyticsPage'
import { InventoryAnalyticsPage } from '@/pages/analytics/InventoryAnalyticsPage'
import { ClinicDashboardPage } from '@/pages/clinic/ClinicDashboardPage'
import { HomeCollectionSchedulePage } from '@/pages/clinic/HomeCollectionSchedulePage'
import { VisitBillPage } from '@/pages/clinic/VisitBillPage'
import { PatientBillHistoryPage } from '@/pages/clinic/PatientBillHistoryPage'

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: (
      <PrivateRoute>
        <AppLayout />
      </PrivateRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },

      { path: 'dashboard', element: <DashboardPage /> },

      // ── Patients ──────────────────────────────────────────────────────────
      {
        path: 'patients',
        element: <PrivateRoute permission="PATIENT.VIEW"><PatientListPage /></PrivateRoute>,
      },
      {
        path: 'patients/:id',
        element: <PrivateRoute permission="PATIENT.VIEW"><PatientDetailPage /></PrivateRoute>,
      },

      // ── OPD ───────────────────────────────────────────────────────────────
      {
        path: 'opd',
        element: <PrivateRoute permission="OPD.VIEW"><OpdListPage /></PrivateRoute>,
      },
      {
        path: 'opd/:id',
        element: <PrivateRoute permission="OPD.VIEW"><OpdVisitPage /></PrivateRoute>,
      },

      // ── Pharmacy ──────────────────────────────────────────────────────────
      {
        path: 'pharmacy/stock',
        element: <PrivateRoute permission="PHARMACY.VIEW"><StockPage /></PrivateRoute>,
      },
      {
        path: 'pharmacy/bill',
        element: <PrivateRoute permission="PHARMACY.CREATE"><PharmacyBillPage /></PrivateRoute>,
      },

      // ── IPD ───────────────────────────────────────────────────────────────
      {
        path: 'ipd',
        element: <PrivateRoute permission="IPD.VIEW"><IpdListPage /></PrivateRoute>,
      },
      {
        path: 'ipd/:id',
        element: <PrivateRoute permission="IPD.VIEW"><IpdAdmissionPage /></PrivateRoute>,
      },

      // ── Front Office ──────────────────────────────────────────────────────
      {
        path: 'frontoffice/appointments',
        element: <PrivateRoute permission="FRONTOFFICE.VIEW"><AppointmentListPage /></PrivateRoute>,
      },
      {
        path: 'frontoffice/queue',
        element: <PrivateRoute permission="FRONTOFFICE.VIEW"><TokenQueuePage /></PrivateRoute>,
      },

      // ── HR ────────────────────────────────────────────────────────────────
      {
        path: 'hr/employees',
        element: <PrivateRoute permission="HR.VIEW"><EmployeeListPage /></PrivateRoute>,
      },
      {
        path: 'hr/:id',
        element: <PrivateRoute permission="HR.VIEW"><EmployeeDetailPage /></PrivateRoute>,
      },
      {
        path: 'hr/attendance',
        element: <PrivateRoute permission="HR.VIEW"><AttendancePage /></PrivateRoute>,
      },
      {
        path: 'hr/leave',
        element: <PrivateRoute permission="HR.VIEW"><LeaveRequestPage /></PrivateRoute>,
      },

      // ── Doctors ───────────────────────────────────────────────────────────
      {
        path: 'doctors',
        element: <PrivateRoute permission="DOCTOR.VIEW"><DoctorDirectoryPage /></PrivateRoute>,
      },
      {
        path: 'doctors/specializations',
        element: <PrivateRoute permission="DOCTOR.VIEW"><SpecializationsPage /></PrivateRoute>,
      },
      {
        path: 'doctors/:id',
        element: <PrivateRoute permission="DOCTOR.VIEW"><DoctorProfilePage /></PrivateRoute>,
      },
      {
        path: 'doctors/:id/schedule',
        element: <PrivateRoute permission="DOCTOR.EDIT"><DoctorSchedulePage /></PrivateRoute>,
      },

      // ── Pathology ─────────────────────────────────────────────────────────
      {
        path: 'pathology',
        element: <PrivateRoute permission="PATHOLOGY.VIEW"><PathologyListPage /></PrivateRoute>,
      },
      {
        path: 'pathology/:id',
        element: <PrivateRoute permission="PATHOLOGY.VIEW"><LabOrderPage /></PrivateRoute>,
      },

      // ── Operation Theatre ─────────────────────────────────────────────────
      {
        path: 'operation/dashboard',
        element: <PrivateRoute permission="OPERATION.VIEW"><OtDashboardPage /></PrivateRoute>,
      },
      {
        path: 'operation/schedules',
        element: <PrivateRoute permission="OPERATION.VIEW"><OtScheduleListPage /></PrivateRoute>,
      },
      {
        path: 'operation/schedules/:id',
        element: <PrivateRoute permission="OPERATION.VIEW"><OtScheduleDetailPage /></PrivateRoute>,
      },

      // ── Radiology ─────────────────────────────────────────────────────────
      {
        path: 'radiology/dashboard',
        element: <PrivateRoute permission="RADIOLOGY.VIEW"><RadiologyDashboardPage /></PrivateRoute>,
      },
      {
        path: 'radiology/orders',
        element: <PrivateRoute permission="RADIOLOGY.VIEW"><RadiologyOrdersPage /></PrivateRoute>,
      },

      // ── Blood Bank ────────────────────────────────────────────────────────
      {
        path: 'bloodbank/dashboard',
        element: <PrivateRoute permission="BLOODBANK.VIEW"><BloodBankDashboardPage /></PrivateRoute>,
      },
      {
        path: 'bloodbank/units',
        element: <PrivateRoute permission="BLOODBANK.VIEW"><BloodUnitListPage /></PrivateRoute>,
      },
      {
        path: 'bloodbank/requests',
        element: <PrivateRoute permission="BLOODBANK.VIEW"><BloodRequestListPage /></PrivateRoute>,
      },

      // ── Inventory ─────────────────────────────────────────────────────────
      {
        path: 'inventory/dashboard',
        element: <PrivateRoute permission="INVENTORY.VIEW"><InventoryDashboardPage /></PrivateRoute>,
      },
      {
        path: 'inventory/items',
        element: <PrivateRoute permission="INVENTORY.VIEW"><ItemListPage /></PrivateRoute>,
      },
      {
        path: 'inventory/movements',
        element: <PrivateRoute permission="INVENTORY.VIEW"><StockMovementPage /></PrivateRoute>,
      },

      // ── Finance ───────────────────────────────────────────────────────────
      {
        path: 'finance/dashboard',
        element: <PrivateRoute permission="FINANCE.VIEW"><FinanceDashboardPage /></PrivateRoute>,
      },
      {
        path: 'finance/income',
        element: <PrivateRoute permission="FINANCE.VIEW"><IncomeListPage /></PrivateRoute>,
      },
      {
        path: 'finance/expenses',
        element: <PrivateRoute permission="FINANCE.VIEW"><ExpenseListPage /></PrivateRoute>,
      },

      // ── Analytics ─────────────────────────────────────────────────────────
      {
        path: 'analytics',
        element: <Navigate to="/analytics/executive" replace />,
      },
      {
        path: 'analytics/executive',
        element: <PrivateRoute permission="REPORTS.VIEW"><ExecutiveDashboardPage /></PrivateRoute>,
      },
      {
        path: 'analytics/financial',
        element: <PrivateRoute permission="REPORTS.VIEW"><FinancialAnalyticsPage /></PrivateRoute>,
      },
      {
        path: 'analytics/patients',
        element: <PrivateRoute permission="REPORTS.VIEW"><PatientAnalyticsPage /></PrivateRoute>,
      },
      {
        path: 'analytics/doctors',
        element: <PrivateRoute permission="REPORTS.VIEW"><DoctorAnalyticsPage /></PrivateRoute>,
      },
      {
        path: 'analytics/appointments',
        element: <PrivateRoute permission="REPORTS.VIEW"><AppointmentAnalyticsPage /></PrivateRoute>,
      },
      {
        path: 'analytics/pharmacy',
        element: <PrivateRoute permission="REPORTS.VIEW"><PharmacyAnalyticsPage /></PrivateRoute>,
      },
      {
        path: 'analytics/laboratory',
        element: <PrivateRoute permission="REPORTS.VIEW"><LaboratoryAnalyticsPage /></PrivateRoute>,
      },
      {
        path: 'analytics/inventory',
        element: <PrivateRoute permission="REPORTS.VIEW"><InventoryAnalyticsPage /></PrivateRoute>,
      },

      // ── SmartClinic ───────────────────────────────────────────────────────
      {
        path: 'clinic/dashboard',
        element: <PrivateRoute><ClinicDashboardPage /></PrivateRoute>,
      },
      {
        path: 'clinic/home-collections',
        element: <PrivateRoute permission="CLINIC.HOME_COLLECTION.VIEW"><HomeCollectionSchedulePage /></PrivateRoute>,
      },
      {
        path: 'clinic/bills',
        element: <PrivateRoute permission="CLINIC.BILL.VIEW"><VisitBillPage /></PrivateRoute>,
      },
      {
        path: 'clinic/bills/patient/:patientId',
        element: <PrivateRoute permission="CLINIC.BILL.VIEW"><PatientBillHistoryPage /></PrivateRoute>,
      },

      // ── 403 ───────────────────────────────────────────────────────────────
      {
        path: '403',
        element: (
          <div className="flex items-center justify-center h-64 text-gray-500">
            <div className="text-center">
              <h2 className="text-2xl font-semibold mb-2">Access Denied</h2>
              <p>You don't have permission to view this page.</p>
            </div>
          </div>
        ),
      },
    ],
  },
])
