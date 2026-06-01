import { useQuery } from '@tanstack/react-query'
import { radiologyApi } from '@/api'
import type { RadiologyOrderStatus } from '@/types'

export const RAD_KEYS = {
  dashboard: ['radiology', 'dashboard']                          as const,
  orders:    (status?: string, page = 0) =>
               ['radiology', 'orders', status, page]             as const,
  order:     (id: string) => ['radiology', 'order', id]          as const,
}

export function useRadiologyDashboard() {
  return useQuery({
    queryKey: RAD_KEYS.dashboard,
    queryFn: () => radiologyApi.getDashboard().then((r) => r.data.data),
    staleTime: 30_000,
  })
}

export function useRadiologyOrders(status?: RadiologyOrderStatus, page = 0) {
  return useQuery({
    queryKey: RAD_KEYS.orders(status, page),
    queryFn: () =>
      radiologyApi.listOrders({ status, page, size: 20 }).then((r) => r.data.data),
    placeholderData: (prev) => prev,
    staleTime: 15_000,
  })
}

export function useRadiologyOrder(id: string) {
  return useQuery({
    queryKey: RAD_KEYS.order(id),
    queryFn: () => radiologyApi.getOrder(id).then((r) => r.data.data),
    enabled: !!id,
    staleTime: 15_000,
  })
}
