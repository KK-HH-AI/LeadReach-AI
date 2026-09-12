import { http } from '../request'
import type { PageResult } from '../types'
import type { Tenant, TenantQuery, TenantCreatePayload, TenantUpdatePayload } from './types'

export const tenantApi = {
  list: (params?: TenantQuery) => http.get<PageResult<Tenant>>('/tenants', params),
  create: (payload: TenantCreatePayload) => http.post<Tenant>('/tenants', payload),
  detail: (id: string) => http.get<Tenant>(`/tenants/${id}`),
  update: (id: string, payload: TenantUpdatePayload) => http.put<Tenant>(`/tenants/${id}`, payload),
  toggle: (id: string, status: 'enabled' | 'disabled') =>
    http.patch<Tenant>(`/tenants/${id}`, { status }),
  resetAdminPassword: (id: string) => http.post<void>(`/tenants/${id}/reset-password`)
}
