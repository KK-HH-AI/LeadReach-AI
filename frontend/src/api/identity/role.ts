import { http } from '../request'
import type { Role, PermissionNode } from './types'

export interface RoleCreatePayload {
  name: string
  code: string
  description?: string
  permissionIds?: string[]
}

export const roleApi = {
  list: () => http.get<Role[]>('/roles'),
  create: (payload: RoleCreatePayload) => http.post<Role>('/roles', payload),
  detail: (id: string) => http.get<Role>(`/roles/${id}`),
  update: (id: string, payload: Partial<RoleCreatePayload>) =>
    http.put<Role>(`/roles/${id}`, payload),
  remove: (id: string) => http.delete<void>(`/roles/${id}`),
  assignPermissions: (id: string, permissionIds: string[]) =>
    http.put<void>(`/roles/${id}/permissions`, { permissionIds })
}

export const permissionApi = {
  tree: () => http.get<PermissionNode[]>('/permissions/tree')
}
