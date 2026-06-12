import { apiClient } from './client'
import type { ApiResponse } from '@/types'
import type {
  ExecutiveDashboard, FinanceAnalytics, PatientAnalytics,
  DoctorAnalytics, AppointmentAnalytics, PharmacyAnalytics,
  LaboratoryAnalytics, InventoryAnalytics,
} from '@/types'

const base = '/v1/analytics'

export const analyticsApi = {
  getExecutive: () =>
    apiClient.get<ApiResponse<ExecutiveDashboard>>(`${base}/executive`),

  getFinance: (from?: string, to?: string) =>
    apiClient.get<ApiResponse<FinanceAnalytics>>(`${base}/finance`, { params: { from, to } }),

  getPatients: (from?: string, to?: string) =>
    apiClient.get<ApiResponse<PatientAnalytics>>(`${base}/patients`, { params: { from, to } }),

  getDoctors: (from?: string, to?: string) =>
    apiClient.get<ApiResponse<DoctorAnalytics>>(`${base}/doctors`, { params: { from, to } }),

  getAppointments: (from?: string, to?: string) =>
    apiClient.get<ApiResponse<AppointmentAnalytics>>(`${base}/appointments`, { params: { from, to } }),

  getPharmacy: (from?: string, to?: string) =>
    apiClient.get<ApiResponse<PharmacyAnalytics>>(`${base}/pharmacy`, { params: { from, to } }),

  getLaboratory: (from?: string, to?: string) =>
    apiClient.get<ApiResponse<LaboratoryAnalytics>>(`${base}/laboratory`, { params: { from, to } }),

  getInventory: (from?: string, to?: string) =>
    apiClient.get<ApiResponse<InventoryAnalytics>>(`${base}/inventory`, { params: { from, to } }),
}
