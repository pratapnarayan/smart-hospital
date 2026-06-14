import { apiClient } from './client'
import type { ApiResponse } from '@/types'
import type {
  HomeCollection,
  HomeCollectionSummary,
  HomeCollectionCreateRequest,
  HomeCollectionUpdateRequest,
  HomeCollectionStatusRequest,
  ClinicBill,
  QuickRegisterRequest,
} from '@/types/clinic.types'
import type { Patient } from '@/types'

export const clinicApi = {
  quickRegister: (data: QuickRegisterRequest): Promise<Patient> =>
    apiClient
      .post<ApiResponse<Patient>>('/v1/clinic/patients/quick-register', data)
      .then((r) => r.data.data),

  createHomeCollection: (data: HomeCollectionCreateRequest): Promise<HomeCollection> =>
    apiClient
      .post<ApiResponse<HomeCollection>>('/v1/clinic/home-collections', data)
      .then((r) => r.data.data),

  getHomeCollectionsByDate: (date: string): Promise<HomeCollectionSummary> =>
    apiClient
      .get<ApiResponse<HomeCollectionSummary>>('/v1/clinic/home-collections', { params: { date } })
      .then((r) => r.data.data),

  getMySchedule: (technicianId: string, date: string): Promise<HomeCollectionSummary> =>
    apiClient
      .get<ApiResponse<HomeCollectionSummary>>('/v1/clinic/home-collections/my-schedule', {
        params: { technicianId, date },
      })
      .then((r) => r.data.data),

  getHomeCollection: (id: string): Promise<HomeCollection> =>
    apiClient
      .get<ApiResponse<HomeCollection>>(`/v1/clinic/home-collections/${id}`)
      .then((r) => r.data.data),

  rescheduleHomeCollection: (id: string, data: HomeCollectionUpdateRequest): Promise<HomeCollection> =>
    apiClient
      .patch<ApiResponse<HomeCollection>>(`/v1/clinic/home-collections/${id}/reschedule`, data)
      .then((r) => r.data.data),

  updateHomeCollectionStatus: (
    id: string,
    data: HomeCollectionStatusRequest,
  ): Promise<HomeCollection> =>
    apiClient
      .patch<ApiResponse<HomeCollection>>(`/v1/clinic/home-collections/${id}/status`, data)
      .then((r) => r.data.data),

  getPatientHomeCollections: (patientId: string): Promise<HomeCollection[]> =>
    apiClient
      .get<ApiResponse<HomeCollection[]>>(`/v1/clinic/home-collections/patient/${patientId}`)
      .then((r) => r.data.data),

  generateBill: (opdVisitId: string): Promise<ClinicBill> =>
    apiClient
      .post<ApiResponse<ClinicBill>>('/v1/clinic/bills', { opdVisitId })
      .then((r) => r.data.data),

  getBillsByVisit: (opdVisitId: string): Promise<ClinicBill[]> =>
    apiClient
      .get<ApiResponse<ClinicBill[]>>(`/v1/clinic/bills/visit/${opdVisitId}`)
      .then((r) => r.data.data),

  finalizeBill: (id: string): Promise<ClinicBill> =>
    apiClient
      .patch<ApiResponse<ClinicBill>>(`/v1/clinic/bills/${id}/finalize`)
      .then((r) => r.data.data),

  cancelBill: (id: string): Promise<ClinicBill> =>
    apiClient
      .patch<ApiResponse<ClinicBill>>(`/v1/clinic/bills/${id}/cancel`)
      .then((r) => r.data.data),

  getPatientBills: (patientId: string): Promise<ClinicBill[]> =>
    apiClient
      .get<ApiResponse<ClinicBill[]>>(`/v1/clinic/bills/patient/${patientId}`)
      .then((r) => r.data.data),
}
