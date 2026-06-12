import type {
  ExecutiveDashboard, FinanceAnalytics, PatientAnalytics,
  DoctorAnalytics, AppointmentAnalytics, PharmacyAnalytics,
  LaboratoryAnalytics, InventoryAnalytics,
} from '@/types'

// Returns true if the data appears to be all-zeros (empty database)
function isEmptyData(data: Record<string, unknown>): boolean {
  return Object.values(data).every(v =>
    v === 0 || v === null || v === undefined ||
    (Array.isArray(v) && v.length === 0) ||
    (typeof v === 'number' && v === 0)
  )
}

export const DEMO_EXECUTIVE: ExecutiveDashboard = {
  todayRevenue: 142500,
  monthRevenue: 3850000,
  totalPatients: 4821,
  todayAppointments: 47,
  pendingPayments: 285000,
  doctorsAvailableToday: 12,
  currentAdmissions: 23,
  labTestsToday: 34,
  medicineSalesToday: 68400,
  inventoryAlerts: 7,
  todayRevenueTrend: 12.4,
  monthRevenueTrend: 8.2,
  totalPatientsTrend: 5.1,
  todayAppointmentsTrend: -3.2,
  revenueTrend: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`,
    value: Math.round(Math.random() * 100000 + 80000),
  })),
  patientGrowth: Array.from({ length: 12 }, (_, i) => ({
    label: ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][i],
    value: Math.round(Math.random() * 200 + 300),
  })),
  revenueBySource: [
    { name: 'OPD', value: 1200000 },
    { name: 'IPD', value: 980000 },
    { name: 'Pharmacy', value: 650000 },
    { name: 'Laboratory', value: 420000 },
    { name: 'Radiology', value: 380000 },
    { name: 'Other', value: 220000 },
  ],
  topDoctors: [
    { name: 'Dr. Rajesh Kumar', value: 380000 },
    { name: 'Dr. Priya Sharma', value: 340000 },
    { name: 'Dr. Amit Patel', value: 295000 },
    { name: 'Dr. Sunita Joshi', value: 270000 },
    { name: 'Dr. Vikram Singh', value: 245000 },
  ],
  departmentRevenue: [
    { name: 'Cardiology', value: 920000 },
    { name: 'Orthopaedics', value: 780000 },
    { name: 'Neurology', value: 650000 },
    { name: 'ENT', value: 430000 },
    { name: 'Dermatology', value: 320000 },
  ],
}

export const DEMO_FINANCE: FinanceAnalytics = {
  totalRevenue: 3850000,
  totalExpenses: 2100000,
  netProfit: 1750000,
  collectionEfficiencyPct: 78.4,
  dailyRevenue: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`, value: Math.round(Math.random() * 80000 + 60000),
  })),
  revenueBySource: DEMO_EXECUTIVE.revenueBySource,
  revenueByDoctor: DEMO_EXECUTIVE.topDoctors,
  monthlyComparison: ['Jan','Feb','Mar','Apr','May','Jun'].map(m => ({
    name: m, value: Math.round(Math.random() * 600000 + 400000),
  })),
  expenseTrend: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`, value: Math.round(Math.random() * 50000 + 30000),
  })),
}

export const DEMO_PATIENTS: PatientAnalytics = {
  totalPatients: 4821,
  newPatientsThisPeriod: 342,
  returningPatients: 4479,
  retentionRatePct: 92.9,
  registrationTrend: ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'].map(m => ({
    label: m,
    value: Math.round(Math.random() * 200 + 250),
  })),
  genderDistribution: [{ name: 'Male', value: 2650 }, { name: 'Female', value: 2010 }, { name: 'Other', value: 161 }],
  ageDistribution: ['0-10','11-20','21-30','31-40','41-50','51-60','61-70','71+'].map((b, i) => ({
    name: b, value: [280, 420, 890, 1050, 930, 680, 420, 151][i],
  })),
  bloodGroupDistribution: [
    { name: 'O+', value: 1450 }, { name: 'A+', value: 1230 }, { name: 'B+', value: 980 },
    { name: 'AB+', value: 420 }, { name: 'O-', value: 310 }, { name: 'A-', value: 220 },
    { name: 'B-', value: 150 }, { name: 'AB-', value: 61 },
  ],
  patientsByDepartment: [],
}

export const DEMO_DOCTORS: DoctorAnalytics = {
  totalDoctors: 24,
  activeDoctors: 21,
  leaderboard: [
    { doctorName: 'Dr. Rajesh Kumar', specialization: 'Cardiology', appointmentsCompleted: 145, revenueGenerated: 380000, utilizationPct: 87 },
    { doctorName: 'Dr. Priya Sharma', specialization: 'Neurology', appointmentsCompleted: 132, revenueGenerated: 340000, utilizationPct: 82 },
    { doctorName: 'Dr. Amit Patel', specialization: 'Orthopaedics', appointmentsCompleted: 121, revenueGenerated: 295000, utilizationPct: 76 },
    { doctorName: 'Dr. Sunita Joshi', specialization: 'Gynaecology', appointmentsCompleted: 118, revenueGenerated: 270000, utilizationPct: 73 },
    { doctorName: 'Dr. Vikram Singh', specialization: 'ENT', appointmentsCompleted: 98, revenueGenerated: 245000, utilizationPct: 68 },
  ],
  revenueByDoctor: DEMO_EXECUTIVE.topDoctors,
  appointmentsByDoctor: [
    { name: 'Dr. Rajesh Kumar', value: 145 },
    { name: 'Dr. Priya Sharma', value: 132 },
    { name: 'Dr. Amit Patel', value: 121 },
    { name: 'Dr. Sunita Joshi', value: 118 },
    { name: 'Dr. Vikram Singh', value: 98 },
  ],
}

export const DEMO_APPOINTMENTS: AppointmentAnalytics = {
  totalAppointments: 1240,
  completed: 980,
  cancelled: 148,
  noShow: 72,
  rescheduled: 40,
  dailyTrend: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`, value: Math.round(Math.random() * 25 + 30),
  })),
  statusDistribution: [
    { name: 'Completed', value: 980 }, { name: 'Cancelled', value: 148 },
    { name: 'No Show', value: 72 }, { name: 'Rescheduled', value: 40 },
  ],
  byDoctor: DEMO_DOCTORS.appointmentsByDoctor,
  byDepartment: [
    { name: 'Cardiology', value: 210 }, { name: 'Orthopaedics', value: 185 },
    { name: 'Neurology', value: 162 }, { name: 'ENT', value: 145 }, { name: 'Dermatology', value: 118 },
  ],
  peakHoursHeatmap: [],
}

export const DEMO_PHARMACY: PharmacyAnalytics = {
  totalMedicineRevenue: 650000,
  totalBillsIssued: 1842,
  lowStockAlerts: 12,
  expiryAlerts: 5,
  topMedicinesByRevenue: [
    { name: 'Metformin 500mg', value: 48000 }, { name: 'Amlodipine 5mg', value: 42000 },
    { name: 'Atorvastatin 10mg', value: 38000 }, { name: 'Pantoprazole 40mg', value: 35000 },
    { name: 'Azithromycin 500mg', value: 31000 },
  ],
  stockHealthDistribution: [
    { name: 'In Stock', value: 218 }, { name: 'Low Stock', value: 12 }, { name: 'Out of Stock', value: 3 },
  ],
  revenueByCategory: [
    { name: 'Antibiotics', value: 185000 }, { name: 'Cardiovascular', value: 162000 },
    { name: 'Analgesics', value: 128000 }, { name: 'Vitamins', value: 98000 }, { name: 'Other', value: 77000 },
  ],
  revenueTrend: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`, value: Math.round(Math.random() * 15000 + 10000),
  })),
}

export const DEMO_LABORATORY: LaboratoryAnalytics = {
  totalTestsPerformed: 2840,
  totalRevenue: 420000,
  pendingReports: 18,
  dailyTestsTrend: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`, value: Math.round(Math.random() * 60 + 60),
  })),
  topTests: [
    { name: 'Complete Blood Count', value: 420 }, { name: 'Lipid Profile', value: 380 },
    { name: 'Blood Sugar (Fasting)', value: 350 }, { name: 'Thyroid Profile', value: 290 },
    { name: 'Liver Function Test', value: 265 },
  ],
  revenueTrend: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`, value: Math.round(Math.random() * 10000 + 8000),
  })),
  statusDistribution: [
    { name: 'Completed', value: 2822 }, { name: 'Pending', value: 18 },
  ],
  byDepartmentReferral: [
    { name: 'OPD', value: 1680 }, { name: 'IPD', value: 840 }, { name: 'Walk-in', value: 320 },
  ],
}

export const DEMO_INVENTORY: InventoryAnalytics = {
  totalStockValue: 4250000,
  lowStockItems: 12,
  outOfStockItems: 3,
  totalItems: 284,
  stockValueTrend: Array.from({ length: 30 }, (_, i) => ({
    label: `Day ${i + 1}`, value: Math.round(Math.random() * 200000 + 4000000),
  })),
  fastMovingItems: [
    { name: 'Surgical Gloves (M)', value: 2400 }, { name: 'Syringes 5ml', value: 1850 },
    { name: 'IV Cannula 22G', value: 1620 }, { name: 'Bandage 4"', value: 1380 },
    { name: 'Paracetamol 500mg', value: 1240 },
  ],
  slowMovingItems: [
    { name: 'Cardiac Monitor Lead', value: 2 }, { name: 'Pulse Oximeter Probe', value: 4 },
    { name: 'Foley Catheter 18Fr', value: 6 },
  ],
  stockByCategory: [
    { name: 'Consumables', value: 1850000 }, { name: 'Medicines', value: 1200000 },
    { name: 'Equipment', value: 750000 }, { name: 'Linen', value: 280000 }, { name: 'Other', value: 170000 },
  ],
  lowStockList: [
    { itemName: 'IV Cannula 18G', category: 'Consumables', currentStock: 15, reorderLevel: 50 },
    { itemName: 'Surgical Mask N95', category: 'Consumables', currentStock: 8, reorderLevel: 100 },
    { itemName: 'Povidone Iodine 500ml', category: 'Medicines', currentStock: 3, reorderLevel: 20 },
  ],
}

// Helper: returns demo data when real data is all-zeros OR ?demo=true is in the URL.
// Pass isLoading=true to suppress auto-fallback while the query is in-flight, preventing
// a flash of demo data before the real response arrives.
export function withDemoFallback<T extends object>(
  data: T | undefined,
  demoData: T,
  isLoading = false
): { data: T; isDemo: boolean } {
  const forceDemo = new URLSearchParams(window.location.search).get('demo') === 'true'
  if (forceDemo) return { data: demoData, isDemo: true }
  if (isLoading) return { data: demoData, isDemo: false }
  if (!data || isEmptyData(data as Record<string, unknown>)) return { data: demoData, isDemo: true }
  return { data, isDemo: false }
}
