import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { pharmacyApi } from '@/api'
import type { BillCreateRequest } from '@/types'

export const PHARMA_KEYS = {
  categories: ['pharma', 'categories']              as const,
  medicines:  (q?: string, page = 0) => ['pharma', 'medicines', q, page] as const,
  medicine:   (id: string) => ['pharma', 'medicine', id]     as const,
  stock:      (id: string) => ['pharma', 'stock', id]        as const,
  expiring:   (days: number) => ['pharma', 'expiring', days] as const,
  lowStock:   ['pharma', 'low-stock']               as const,
}

export function useMedicineCategories() {
  return useQuery({
    queryKey: PHARMA_KEYS.categories,
    queryFn: () => pharmacyApi.listCategories().then((r) => r.data.data),
    staleTime: 5 * 60_000,
  })
}

export function useMedicines(query?: string, page = 0) {
  return useQuery({
    queryKey: PHARMA_KEYS.medicines(query, page),
    queryFn: () => pharmacyApi.searchMedicines({ query, page, size: 20 }).then((r) => r.data.data),
    placeholderData: (prev) => prev,
    staleTime: 30_000,
  })
}

export function useStockSummary(medicineId: string) {
  return useQuery({
    queryKey: PHARMA_KEYS.stock(medicineId),
    queryFn: () => pharmacyApi.getStock(medicineId).then((r) => r.data.data),
    enabled: !!medicineId,   // don't fire until a medicine is actually selected
    staleTime: 15_000,
  })
}

export function useLowStockMedicines() {
  return useQuery({
    queryKey: PHARMA_KEYS.lowStock,
    queryFn: () => pharmacyApi.getLowStock().then((r) => r.data.data),
    staleTime: 60_000,
  })
}

export function useExpiringBatches(days = 30) {
  return useQuery({
    queryKey: PHARMA_KEYS.expiring(days),
    queryFn: () => pharmacyApi.getExpiring(days).then((r) => r.data.data),
    staleTime: 60_000,
  })
}

export function useAddBatch() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: Omit<import('@/types').MedicineBatch, 'id' | 'medicineName' | 'expired' | 'lowStock'>) =>
      pharmacyApi.addBatch(body).then((r) => r.data.data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: PHARMA_KEYS.stock(vars.medicineId) })
      qc.invalidateQueries({ queryKey: ['pharma', 'medicines'] })
      qc.invalidateQueries({ queryKey: PHARMA_KEYS.lowStock })
      qc.invalidateQueries({ queryKey: ['pharma', 'expiring'] })
      message.success('Batch added — stock updated')
    },
    onError: () => { message.error('Failed to add batch') },
  })
}

export function useCreateBill() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: BillCreateRequest) =>
      pharmacyApi.createBill(body).then((r) => r.data.data),
    onSuccess: () => {
      // Invalidate stock queries since quantities changed
      qc.invalidateQueries({ queryKey: ['pharma', 'stock'] })
      qc.invalidateQueries({ queryKey: PHARMA_KEYS.lowStock })
      message.success('Bill created and stock deducted')
    },
    onError: () => { message.error('Failed to create bill') },
  })
}
