import { apiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type {
  Appointment, OpdToken, FrontOfficeDashboard,
  BookAppointmentPayload, IssueTokenPayload,
  AppointmentStatus, TokenStatus,
} from '@/types'

export const frontOfficeApi = {
  // ── Appointments ───────────────────────────────────────────────────────────
  createAppointment: (payload: BookAppointmentPayload) =>
    apiClient.post<ApiResponse<Appointment>>('/v1/frontoffice/appointments', payload),

  getAppointment: (id: string) =>
    apiClient.get<ApiResponse<Appointment>>(`/v1/frontoffice/appointments/${id}`),

  listAppointments: (params: { date?: string; page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<Appointment>>>('/v1/frontoffice/appointments', { params }),

  listUpcoming: (params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<Appointment>>>('/v1/frontoffice/appointments/upcoming', { params }),

  listByPatient: (patientId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<Appointment>>>(
      `/v1/frontoffice/appointments/patient/${patientId}`, { params }),

  listUpcomingByPatient: (patientId: string) =>
    apiClient.get<ApiResponse<Appointment[]>>(
      `/v1/frontoffice/appointments/patient/${patientId}/upcoming`),

  listByDoctor: (doctorId: string, params?: { date?: string; page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<Appointment>>>(
      `/v1/frontoffice/appointments/doctor/${doctorId}`, { params }),

  updateAppointment: (id: string, payload: Partial<BookAppointmentPayload> & { status?: AppointmentStatus }) =>
    apiClient.patch<ApiResponse<Appointment>>(`/v1/frontoffice/appointments/${id}`, payload),

  cancelAppointment: (id: string) =>
    apiClient.delete(`/v1/frontoffice/appointments/${id}`),

  // ── Tokens ─────────────────────────────────────────────────────────────────
  issueToken: (payload: IssueTokenPayload) =>
    apiClient.post<ApiResponse<OpdToken>>('/v1/frontoffice/tokens', payload),

  getToken: (id: string) =>
    apiClient.get<ApiResponse<OpdToken>>(`/v1/frontoffice/tokens/${id}`),

  listTokens: (params: { date?: string; department?: string }) =>
    apiClient.get<ApiResponse<OpdToken[]>>('/v1/frontoffice/tokens', { params }),

  updateTokenStatus: (id: string, status: TokenStatus) =>
    apiClient.patch<ApiResponse<OpdToken>>(`/v1/frontoffice/tokens/${id}/status`, null, { params: { status } }),

  // ── Dashboard ──────────────────────────────────────────────────────────────
  getDashboard: () =>
    apiClient.get<ApiResponse<FrontOfficeDashboard>>('/v1/frontoffice/dashboard'),
}
