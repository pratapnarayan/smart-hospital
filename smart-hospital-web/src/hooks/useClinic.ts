import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { clinicApi } from '@/api/clinic.api'
import type {
  HomeCollectionCreateRequest,
  HomeCollectionUpdateRequest,
  HomeCollectionStatusRequest,
  QuickRegisterRequest,
} from '@/types/clinic.types'

export function useQuickRegisterPatient() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: QuickRegisterRequest) => clinicApi.quickRegister(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['patients'] }),
  })
}

export function useHomeCollectionsByDate(date: string) {
  return useQuery({
    queryKey: ['clinic', 'home-collections', 'date', date],
    queryFn: () => clinicApi.getHomeCollectionsByDate(date),
    enabled: !!date,
  })
}

export function useCreateHomeCollection() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: HomeCollectionCreateRequest) => clinicApi.createHomeCollection(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clinic', 'home-collections'] }),
  })
}

export function useUpdateHomeCollectionStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: HomeCollectionStatusRequest }) =>
      clinicApi.updateHomeCollectionStatus(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clinic', 'home-collections'] }),
  })
}

export function useRescheduleHomeCollection() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: HomeCollectionUpdateRequest }) =>
      clinicApi.rescheduleHomeCollection(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clinic', 'home-collections'] }),
  })
}

export function useGenerateBill(opdVisitId?: string) {
  const qc = useQueryClient()
  const existingBills = useQuery({
    queryKey: ['clinic', 'bills', 'visit', opdVisitId],
    queryFn: () => clinicApi.getBillsByVisit(opdVisitId!),
    enabled: !!opdVisitId,
  })
  const generate = useMutation({
    mutationFn: (visitId: string) => clinicApi.generateBill(visitId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clinic', 'bills'] }),
  })
  const finalize = useMutation({
    mutationFn: (billId: string) => clinicApi.finalizeBill(billId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clinic', 'bills'] }),
  })
  const cancel = useMutation({
    mutationFn: (billId: string) => clinicApi.cancelBill(billId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clinic', 'bills'] }),
  })
  return { existingBills, generate, finalize, cancel }
}

export function usePatientBills(patientId?: string) {
  return useQuery({
    queryKey: ['clinic', 'bills', 'patient', patientId],
    queryFn: () => clinicApi.getPatientBills(patientId!),
    enabled: !!patientId,
  })
}
