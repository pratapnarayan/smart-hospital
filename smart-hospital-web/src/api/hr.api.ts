import { apiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type {
  HrDepartment, Designation, Employee, AttendanceRecord, LeaveRequest,
  HrDashboard, CreateEmployeePayload, ApplyLeavePayload, LeaveStatus,
  EmployeeGender,
} from '@/types'

export const hrApi = {
  // ── Departments ────────────────────────────────────────────────────────────
  listDepartments: () =>
    apiClient.get<ApiResponse<HrDepartment[]>>('/v1/hr/departments'),

  createDepartment: (payload: { name: string; code: string }) =>
    apiClient.post<ApiResponse<HrDepartment>>('/v1/hr/departments', payload),

  // ── Designations ───────────────────────────────────────────────────────────
  listDesignations: (departmentId?: string) =>
    apiClient.get<ApiResponse<Designation[]>>('/v1/hr/designations',
      { params: departmentId ? { departmentId } : {} }),

  createDesignation: (payload: { title: string; departmentId?: string }) =>
    apiClient.post<ApiResponse<Designation>>('/v1/hr/designations', payload),

  // ── Employees ──────────────────────────────────────────────────────────────
  createEmployee: (payload: CreateEmployeePayload) =>
    apiClient.post<ApiResponse<Employee>>('/v1/hr/employees', payload),

  getEmployee: (id: string) =>
    apiClient.get<ApiResponse<Employee>>(`/v1/hr/employees/${id}`),

  listEmployees: (params?: { departmentId?: string; search?: string; page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<Employee>>>('/v1/hr/employees', { params }),

  updateEmployee: (id: string, payload: Partial<CreateEmployeePayload> & { status?: string }) =>
    apiClient.patch<ApiResponse<Employee>>(`/v1/hr/employees/${id}`, payload),

  deleteEmployee: (id: string) =>
    apiClient.delete(`/v1/hr/employees/${id}`),

  uploadEmployeePhoto: (id: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient.post<ApiResponse<Employee>>(`/v1/hr/employees/${id}/photo`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  // ── Attendance ─────────────────────────────────────────────────────────────
  markAttendance: (payload: {
    employeeId: string; attendanceDate: string;
    checkIn?: string; checkOut?: string; status: string; notes?: string
  }) => apiClient.post<ApiResponse<AttendanceRecord>>('/v1/hr/attendance', payload),

  getAttendanceByDate: (date?: string) =>
    apiClient.get<ApiResponse<AttendanceRecord[]>>('/v1/hr/attendance',
      { params: date ? { date } : {} }),

  getEmployeeAttendance: (employeeId: string) =>
    apiClient.get<ApiResponse<AttendanceRecord[]>>(`/v1/hr/employees/${employeeId}/attendance`),

  // ── Leave ──────────────────────────────────────────────────────────────────
  applyLeave: (payload: ApplyLeavePayload) =>
    apiClient.post<ApiResponse<LeaveRequest>>('/v1/hr/leave', payload),

  listLeaves: (params?: { employeeId?: string; status?: LeaveStatus; page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<LeaveRequest>>>('/v1/hr/leave', { params }),

  approveLeave: (id: string, approverNotes?: string) =>
    apiClient.post<ApiResponse<LeaveRequest>>(`/v1/hr/leave/${id}/approve`, { approverNotes }),

  rejectLeave: (id: string, approverNotes?: string) =>
    apiClient.post<ApiResponse<LeaveRequest>>(`/v1/hr/leave/${id}/reject`, { approverNotes }),

  cancelLeave: (id: string) =>
    apiClient.delete(`/v1/hr/leave/${id}`),

  // ── Dashboard ──────────────────────────────────────────────────────────────
  getDashboard: () =>
    apiClient.get<ApiResponse<HrDashboard>>('/v1/hr/dashboard'),
}
