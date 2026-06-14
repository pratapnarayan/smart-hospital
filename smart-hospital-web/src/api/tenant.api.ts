import { apiClient } from './client'
import type { ApiResponse } from '@/types'
import type { TenantClinicConfig } from '@/types/clinic.types'

export const tenantApi = {
  getClinicConfig: (): Promise<TenantClinicConfig> =>
    apiClient
      .get<ApiResponse<TenantClinicConfig>>('/v1/tenants/current/config')
      .then((r) => r.data.data),
}
