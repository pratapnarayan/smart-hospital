import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { doctorApi } from '@/api'
import type { DoctorProfileRequest, SpecializationRequest, DoctorScheduleRequest } from '@/types'

const KEYS = {
  specializations: ['doctor', 'specializations'] as const,
  doctors:         (search?: string, deptId?: string, specId?: string) =>
    ['doctor', 'doctors', search ?? '', deptId ?? '', specId ?? ''] as const,
  doctor:          (id: string) => ['doctor', 'doctors', id] as const,
  schedules:       (id: string) => ['doctor', 'schedules', id] as const,
  availability:    (id: string, from?: string, to?: string) =>
    ['doctor', 'availability', id, from ?? '', to ?? ''] as const,
  dashboard:       ['doctor', 'dashboard'] as const,
}

export function useSpecializations() {
  return useQuery({
    queryKey: KEYS.specializations,
    queryFn: () => doctorApi.listSpecializations().then(r => r.data.data),
  })
}

export function useCreateSpecialization() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: SpecializationRequest) =>
      doctorApi.createSpecialization(payload).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['doctor', 'specializations'] })
      message.success('Specialization created')
    },
    onError: () => message.error('Failed to create specialization'),
  })
}

export function useUpdateSpecialization(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: SpecializationRequest) =>
      doctorApi.updateSpecialization(id, payload).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['doctor', 'specializations'] })
      message.success('Specialization updated')
    },
    onError: () => message.error('Failed to update specialization'),
  })
}

export function useDoctors(search?: string, deptId?: string, specId?: string, page = 0) {
  return useQuery({
    queryKey: [...KEYS.doctors(search, deptId, specId), page],
    queryFn: () => doctorApi.listDoctors({ search, departmentId: deptId, specializationId: specId, page, size: 20 })
      .then(r => r.data.data),
  })
}

export function useDoctor(id: string) {
  return useQuery({
    queryKey: KEYS.doctor(id),
    queryFn: () => doctorApi.getDoctor(id).then(r => r.data.data),
    enabled: !!id,
  })
}

export function useDoctorByEmployee(employeeId: string) {
  return useQuery({
    queryKey: ['doctor', 'byEmployee', employeeId],
    queryFn: () => doctorApi.getDoctorByEmployee(employeeId).then(r => r.data.data),
    enabled: !!employeeId,
    retry: false,
  })
}

export function useCreateDoctor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: DoctorProfileRequest) =>
      doctorApi.createDoctor(payload).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['doctor', 'doctors'] })
      qc.invalidateQueries({ queryKey: ['doctor', 'dashboard'] })
      message.success('Doctor profile created')
    },
    onError: () => message.error('Failed to create doctor profile'),
  })
}

export function useUpdateDoctor(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: DoctorProfileRequest) =>
      doctorApi.updateDoctor(id, payload).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.doctor(id) })
      qc.invalidateQueries({ queryKey: ['doctor', 'doctors'] })
      qc.invalidateQueries({ queryKey: KEYS.dashboard })
      message.success('Doctor profile updated')
    },
    onError: () => message.error('Failed to update doctor profile'),
  })
}

export function useDoctorSchedules(doctorId: string) {
  return useQuery({
    queryKey: KEYS.schedules(doctorId),
    queryFn: () => doctorApi.getSchedules(doctorId).then(r => r.data.data),
    enabled: !!doctorId,
  })
}

export function useSaveDoctorSchedules(doctorId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: DoctorScheduleRequest[]) =>
      doctorApi.saveSchedules(doctorId, payload).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.schedules(doctorId) })
      message.success('Schedules saved')
    },
    onError: () => message.error('Failed to save schedules'),
  })
}

export function useDoctorAvailability(doctorId: string, from?: string, to?: string) {
  return useQuery({
    queryKey: KEYS.availability(doctorId, from, to),
    queryFn: () => doctorApi.getAvailability(doctorId, from, to).then(r => r.data.data),
    enabled: !!doctorId,
  })
}

export function useUploadDoctorPhoto(doctorId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => doctorApi.uploadDoctorPhoto(doctorId, file).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.doctor(doctorId) })
      qc.invalidateQueries({ queryKey: ['doctor', 'doctors'] })
      message.success('Photo updated')
    },
    onError: () => message.error('Failed to upload photo'),
  })
}

export function useDoctorDashboard() {
  return useQuery({
    queryKey: KEYS.dashboard,
    queryFn: () => doctorApi.getDashboard().then(r => r.data.data),
    refetchInterval: 60_000,
  })
}
