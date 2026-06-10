import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { hrApi } from '@/api'
import type { CreateEmployeePayload, ApplyLeavePayload, LeaveStatus } from '@/types'

const KEYS = {
  departments:  ['hr', 'departments'] as const,
  designations: (deptId?: string) => ['hr', 'designations', deptId ?? 'all'] as const,
  employees:    (deptId?: string, search?: string) => ['hr', 'employees', deptId ?? 'all', search ?? ''] as const,
  employee:     (id: string) => ['hr', 'employees', id] as const,
  attendance:   (date?: string) => ['hr', 'attendance', date ?? 'today'] as const,
  empAttendance:(id: string) => ['hr', 'attendance', 'emp', id] as const,
  leaves:       (status?: LeaveStatus) => ['hr', 'leave', status ?? 'all'] as const,
  dashboard:    ['hr', 'dashboard'] as const,
}

export function useHrDepartments() {
  return useQuery({ queryKey: KEYS.departments, queryFn: () => hrApi.listDepartments().then(r => r.data.data) })
}

export function useDesignations(departmentId?: string) {
  return useQuery({
    queryKey: KEYS.designations(departmentId),
    queryFn: () => hrApi.listDesignations(departmentId).then(r => r.data.data),
  })
}

export function useEmployees(departmentId?: string, search?: string, page = 0) {
  return useQuery({
    queryKey: [...KEYS.employees(departmentId, search), page],
    queryFn: () => hrApi.listEmployees({ departmentId, search, page, size: 20 }).then(r => r.data.data),
  })
}

export function useEmployee(id: string) {
  return useQuery({
    queryKey: KEYS.employee(id),
    queryFn: () => hrApi.getEmployee(id).then(r => r.data.data),
    enabled: !!id,
  })
}

export function useHrDashboard() {
  return useQuery({
    queryKey: KEYS.dashboard,
    queryFn: () => hrApi.getDashboard().then(r => r.data.data),
    refetchInterval: 60_000,
  })
}

export function useAttendanceByDate(date?: string) {
  return useQuery({
    queryKey: KEYS.attendance(date),
    queryFn: () => hrApi.getAttendanceByDate(date).then(r => r.data.data),
  })
}

export function useEmployeeAttendance(employeeId: string) {
  return useQuery({
    queryKey: KEYS.empAttendance(employeeId),
    queryFn: () => hrApi.getEmployeeAttendance(employeeId).then(r => r.data.data),
    enabled: !!employeeId,
  })
}

export function useEmployeeLeaves(employeeId: string, page = 0) {
  return useQuery({
    queryKey: ['hr', 'leave', 'emp', employeeId, page],
    queryFn: () => hrApi.listLeaves({ employeeId, page, size: 20 }).then(r => r.data.data),
    enabled: !!employeeId,
  })
}

export function useLeaves(status?: LeaveStatus, page = 0) {
  return useQuery({
    queryKey: [...KEYS.leaves(status), page],
    queryFn: () => hrApi.listLeaves({ status, page, size: 20 }).then(r => r.data.data),
  })
}

export function useCreateEmployee() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateEmployeePayload) => hrApi.createEmployee(payload).then(r => r.data.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['hr', 'employees'] })
      qc.invalidateQueries({ queryKey: KEYS.dashboard })
      message.success(`Employee ${data.employeeCode} added`)
    },
    onError: () => message.error('Failed to add employee'),
  })
}

export function useUpdateEmployee(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: Partial<CreateEmployeePayload> & { status?: string }) =>
      hrApi.updateEmployee(id, payload).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.employee(id) })
      qc.invalidateQueries({ queryKey: ['hr', 'employees'] })
      message.success('Employee updated')
    },
    onError: () => message.error('Failed to update employee'),
  })
}

export function useMarkAttendance() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: hrApi.markAttendance,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['hr', 'attendance'] })
      message.success('Attendance marked')
    },
    onError: () => message.error('Failed to mark attendance'),
  })
}

export function useUploadEmployeePhoto(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => hrApi.uploadEmployeePhoto(id, file).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.employee(id) })
      message.success('Photo updated')
    },
    onError: () => message.error('Failed to upload photo'),
  })
}

export function useApplyLeave() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: ApplyLeavePayload) => hrApi.applyLeave(payload).then(r => r.data.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['hr', 'leave'] })
      message.success(`Leave ${data.leaveNumber} submitted`)
    },
    onError: () => message.error('Failed to apply for leave'),
  })
}

export function useApproveLeave() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, notes }: { id: string; notes?: string }) => hrApi.approveLeave(id, notes).then(r => r.data.data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['hr', 'leave'] }); message.success('Leave approved') },
    onError: () => message.error('Failed to approve leave'),
  })
}

export function useRejectLeave() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, notes }: { id: string; notes?: string }) => hrApi.rejectLeave(id, notes).then(r => r.data.data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['hr', 'leave'] }); message.success('Leave rejected') },
    onError: () => message.error('Failed to reject leave'),
  })
}
