import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { analyticsApi } from '@/api/analytics.api'

const STALE = 5 * 60 * 1000  // 5 minutes

function useDateRange() {
  const [params] = useSearchParams()
  const from = params.get('from') ?? undefined
  const to = params.get('to') ?? undefined
  return { from, to }
}

export function useExecutiveDashboard() {
  return useQuery({
    queryKey: ['analytics', 'executive'],
    queryFn: () => analyticsApi.getExecutive().then(r => r.data.data),
    staleTime: STALE,
  })
}

export function useFinanceAnalytics() {
  const { from, to } = useDateRange()
  return useQuery({
    queryKey: ['analytics', 'finance', from, to],
    queryFn: () => analyticsApi.getFinance(from, to).then(r => r.data.data),
    staleTime: STALE,
  })
}

export function usePatientAnalytics() {
  const { from, to } = useDateRange()
  return useQuery({
    queryKey: ['analytics', 'patients', from, to],
    queryFn: () => analyticsApi.getPatients(from, to).then(r => r.data.data),
    staleTime: STALE,
  })
}

export function useDoctorAnalytics() {
  const { from, to } = useDateRange()
  return useQuery({
    queryKey: ['analytics', 'doctors', from, to],
    queryFn: () => analyticsApi.getDoctors(from, to).then(r => r.data.data),
    staleTime: STALE,
  })
}

export function useAppointmentAnalytics() {
  const { from, to } = useDateRange()
  return useQuery({
    queryKey: ['analytics', 'appointments', from, to],
    queryFn: () => analyticsApi.getAppointments(from, to).then(r => r.data.data),
    staleTime: STALE,
  })
}

export function usePharmacyAnalytics() {
  const { from, to } = useDateRange()
  return useQuery({
    queryKey: ['analytics', 'pharmacy', from, to],
    queryFn: () => analyticsApi.getPharmacy(from, to).then(r => r.data.data),
    staleTime: STALE,
  })
}

export function useLaboratoryAnalytics() {
  const { from, to } = useDateRange()
  return useQuery({
    queryKey: ['analytics', 'laboratory', from, to],
    queryFn: () => analyticsApi.getLaboratory(from, to).then(r => r.data.data),
    staleTime: STALE,
  })
}

export function useInventoryAnalytics() {
  const { from, to } = useDateRange()
  return useQuery({
    queryKey: ['analytics', 'inventory', from, to],
    queryFn: () => analyticsApi.getInventory(from, to).then(r => r.data.data),
    staleTime: STALE,
  })
}
