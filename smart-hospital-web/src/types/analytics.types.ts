export interface TrendPoint {
  label: string
  value: number
}

export interface NameValuePoint {
  name: string
  value: number
}

export interface ExecutiveDashboard {
  todayRevenue: number
  monthRevenue: number
  totalPatients: number
  todayAppointments: number
  pendingPayments: number
  doctorsAvailableToday: number
  currentAdmissions: number
  labTestsToday: number
  medicineSalesToday: number
  inventoryAlerts: number
  todayRevenueTrend: number | null
  monthRevenueTrend: number | null
  totalPatientsTrend: number | null
  todayAppointmentsTrend: number | null
  revenueTrend: TrendPoint[]
  patientGrowth: TrendPoint[]
  revenueBySource: NameValuePoint[]
  topDoctors: NameValuePoint[]
  departmentRevenue: NameValuePoint[]
}

export interface FinanceAnalytics {
  totalRevenue: number
  totalExpenses: number
  netProfit: number
  collectionEfficiencyPct: number
  dailyRevenue: TrendPoint[]
  revenueBySource: NameValuePoint[]
  revenueByDoctor: NameValuePoint[]
  monthlyComparison: NameValuePoint[]
  expenseTrend: TrendPoint[]
}

export interface PatientAnalytics {
  totalPatients: number
  newPatientsThisPeriod: number
  returningPatients: number
  retentionRatePct: number
  registrationTrend: TrendPoint[]
  genderDistribution: NameValuePoint[]
  ageDistribution: NameValuePoint[]
  bloodGroupDistribution: NameValuePoint[]
  patientsByDepartment: NameValuePoint[]
}

export interface DoctorStatEntry {
  doctorName: string
  specialization: string
  appointmentsCompleted: number
  revenueGenerated: number
  utilizationPct: number
}

export interface DoctorAnalytics {
  totalDoctors: number
  activeDoctors: number
  leaderboard: DoctorStatEntry[]
  revenueByDoctor: NameValuePoint[]
  appointmentsByDoctor: NameValuePoint[]
}

export interface HeatmapCell {
  hour: number
  weekday: string
  count: number
}

export interface AppointmentAnalytics {
  totalAppointments: number
  completed: number
  cancelled: number
  noShow: number
  rescheduled: number
  dailyTrend: TrendPoint[]
  statusDistribution: NameValuePoint[]
  byDoctor: NameValuePoint[]
  byDepartment: NameValuePoint[]
  peakHoursHeatmap: HeatmapCell[]
}

export interface PharmacyAnalytics {
  totalMedicineRevenue: number
  totalBillsIssued: number
  lowStockAlerts: number
  expiryAlerts: number
  topMedicinesByRevenue: NameValuePoint[]
  stockHealthDistribution: NameValuePoint[]
  revenueByCategory: NameValuePoint[]
  revenueTrend: TrendPoint[]
}

export interface LaboratoryAnalytics {
  totalTestsPerformed: number
  totalRevenue: number
  pendingReports: number
  dailyTestsTrend: TrendPoint[]
  topTests: NameValuePoint[]
  revenueTrend: TrendPoint[]
  statusDistribution: NameValuePoint[]
  byDepartmentReferral: NameValuePoint[]
}

export interface LowStockEntry {
  itemName: string
  category: string
  currentStock: number
  reorderLevel: number
}

export interface InventoryAnalytics {
  totalStockValue: number
  lowStockItems: number
  outOfStockItems: number
  totalItems: number
  stockValueTrend: TrendPoint[]
  fastMovingItems: NameValuePoint[]
  slowMovingItems: NameValuePoint[]
  stockByCategory: NameValuePoint[]
  lowStockList: LowStockEntry[]
}
