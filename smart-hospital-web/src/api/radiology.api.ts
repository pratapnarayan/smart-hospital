import { apiClient } from './client'
import type {
  ApiResponse, PageResponse,
  RadiologyOrder, RadiologyDashboard,
} from '@/types'

export const radiologyApi = {
  getDashboard: () =>
    apiClient.get<ApiResponse<RadiologyDashboard>>('/v1/radiology/dashboard'),

  listOrders: (params?: { status?: string; page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<RadiologyOrder>>>('/v1/radiology/orders', { params }),

  getOrder: (id: string) =>
    apiClient.get<ApiResponse<RadiologyOrder>>(`/v1/radiology/orders/${id}`),

  listByPatient: (patientId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<RadiologyOrder>>>(
      `/v1/radiology/orders/patient/${patientId}`, { params }),
}
