import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { frontOfficeApi } from '@/api'
import type { BookAppointmentPayload, IssueTokenPayload, AppointmentStatus, TokenStatus } from '@/types'

const KEYS = {
  appointments:  (date?: string) => ['frontoffice', 'appointments', date ?? 'today'] as const,
  appointment:   (id: string) => ['frontoffice', 'appointments', id] as const,
  upcoming:      ['frontoffice', 'appointments', 'upcoming'] as const,
  byPatient:     (id: string) => ['frontoffice', 'appointments', 'patient', id] as const,
  upcomingByPt:  (id: string) => ['frontoffice', 'appointments', 'patient', id, 'upcoming'] as const,
  tokens:        (date?: string, dept?: string) => ['frontoffice', 'tokens', date ?? 'today', dept ?? 'all'] as const,
  dashboard:     ['frontoffice', 'dashboard'] as const,
}

export function useFrontOfficeDashboard() {
  return useQuery({
    queryKey: KEYS.dashboard,
    queryFn: () => frontOfficeApi.getDashboard().then(r => r.data.data),
    refetchInterval: 30_000,
  })
}

export function useAppointments(date?: string) {
  return useQuery({
    queryKey: KEYS.appointments(date),
    queryFn: () => frontOfficeApi.listAppointments({ date, size: 50 }).then(r => r.data.data),
  })
}

export function useAppointment(id: string) {
  return useQuery({
    queryKey: KEYS.appointment(id),
    queryFn: () => frontOfficeApi.getAppointment(id).then(r => r.data.data),
    enabled: !!id,
  })
}

export function usePatientAppointments(patientId: string) {
  return useQuery({
    queryKey: KEYS.byPatient(patientId),
    queryFn: () => frontOfficeApi.listByPatient(patientId).then(r => r.data.data),
    enabled: !!patientId,
  })
}

export function useUpcomingAppointments() {
  return useQuery({
    queryKey: KEYS.upcoming,
    queryFn: () => frontOfficeApi.listUpcoming({ size: 100 }).then(r => r.data.data),
    staleTime: 30_000,
  })
}

export function usePatientUpcomingAppointments(patientId: string) {
  return useQuery({
    queryKey: KEYS.upcomingByPt(patientId),
    queryFn: () => frontOfficeApi.listUpcomingByPatient(patientId).then(r => r.data.data),
    enabled: !!patientId,
    staleTime: 30_000,
  })
}

export function useOpdTokens(date?: string, department?: string) {
  return useQuery({
    queryKey: KEYS.tokens(date, department),
    queryFn: () => frontOfficeApi.listTokens({ date, department }).then(r => r.data.data),
    refetchInterval: 15_000,
  })
}

export function useBookAppointment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: BookAppointmentPayload) =>
      frontOfficeApi.createAppointment(payload).then(r => r.data.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['frontoffice'] })
      message.success(`Appointment ${data.appointmentNumber} booked`)
    },
    onError: () => message.error('Failed to book appointment'),
  })
}

export function useUpdateAppointment(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: Partial<BookAppointmentPayload> & { status?: AppointmentStatus }) =>
      frontOfficeApi.updateAppointment(id, payload).then(r => r.data.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['frontoffice'] })
      message.success('Appointment updated')
    },
    onError: () => message.error('Failed to update appointment'),
  })
}

export function useCancelAppointment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => frontOfficeApi.cancelAppointment(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['frontoffice'] })
      message.success('Appointment cancelled')
    },
    onError: () => message.error('Failed to cancel appointment'),
  })
}

export function useIssueToken() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: IssueTokenPayload) =>
      frontOfficeApi.issueToken(payload).then(r => r.data.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['frontoffice', 'tokens'] })
      message.success(`Token ${data.tokenNumber} issued to ${data.patientName}`)
    },
    onError: () => message.error('Failed to issue token'),
  })
}

export function useUpdateTokenStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: TokenStatus }) =>
      frontOfficeApi.updateTokenStatus(id, status).then(r => r.data.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['frontoffice', 'tokens'] }),
    onError: () => message.error('Failed to update token status'),
  })
}
