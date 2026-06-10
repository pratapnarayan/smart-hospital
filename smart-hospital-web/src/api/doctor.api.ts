import { apiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type {
  DoctorProfile, DoctorProfileRequest,
  DoctorSchedule, DoctorScheduleRequest,
  Specialization, SpecializationRequest,
  AvailableSlot, DoctorDashboard,
} from '@/types'

export const doctorApi = {
  // Specializations
  listSpecializations: () =>
    apiClient.get<ApiResponse<Specialization[]>>('/v1/doctor/specializations'),
  createSpecialization: (payload: SpecializationRequest) =>
    apiClient.post<ApiResponse<Specialization>>('/v1/doctor/specializations', payload),
  updateSpecialization: (id: string, payload: SpecializationRequest) =>
    apiClient.patch<ApiResponse<Specialization>>(`/v1/doctor/specializations/${id}`, payload),

  // Doctor profiles
  listDoctors: (params?: { search?: string; departmentId?: string; specializationId?: string; page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<DoctorProfile>>>('/v1/doctor/doctors', { params }),
  getDoctor: (id: string) =>
    apiClient.get<ApiResponse<DoctorProfile>>(`/v1/doctor/doctors/${id}`),
  getDoctorByEmployee: (employeeId: string) =>
    apiClient.get<ApiResponse<DoctorProfile>>(`/v1/doctor/doctors/by-employee/${employeeId}`),
  createDoctor: (payload: DoctorProfileRequest) =>
    apiClient.post<ApiResponse<DoctorProfile>>('/v1/doctor/doctors', payload),
  updateDoctor: (id: string, payload: DoctorProfileRequest) =>
    apiClient.patch<ApiResponse<DoctorProfile>>(`/v1/doctor/doctors/${id}`, payload),

  // Schedules
  getSchedules: (doctorId: string) =>
    apiClient.get<ApiResponse<DoctorSchedule[]>>(`/v1/doctor/doctors/${doctorId}/schedules`),
  saveSchedules: (doctorId: string, payload: DoctorScheduleRequest[]) =>
    apiClient.put<ApiResponse<DoctorSchedule[]>>(`/v1/doctor/doctors/${doctorId}/schedules`, payload),

  // Availability
  getAvailability: (doctorId: string, from?: string, to?: string) =>
    apiClient.get<ApiResponse<AvailableSlot[]>>(`/v1/doctor/doctors/${doctorId}/availability`, {
      params: { from, to }
    }),

  // Dashboard
  getDashboard: () =>
    apiClient.get<ApiResponse<DoctorDashboard>>('/v1/doctor/dashboard'),

  // Photo
  uploadDoctorPhoto: (doctorId: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient.post<ApiResponse<DoctorProfile>>(`/v1/doctor/doctors/${doctorId}/photo`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
