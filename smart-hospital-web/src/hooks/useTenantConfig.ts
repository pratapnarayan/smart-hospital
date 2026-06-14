import { useQuery } from '@tanstack/react-query'
import { tenantApi } from '@/api/tenant.api'
import type { TenantClinicConfig } from '@/types/clinic.types'

export function useTenantConfig() {
  return useQuery<TenantClinicConfig>({
    queryKey: ['tenant', 'config'],
    queryFn: tenantApi.getClinicConfig,
    staleTime: 10 * 60 * 1000,
  })
}
