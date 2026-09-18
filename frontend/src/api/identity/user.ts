import { http } from '../request'
import type { PageResult } from '../types'
import type { User, UserQuery, UserCreatePayload } from './types'

export const userApi = {
  list: (params?: UserQuery) => http.get<PageResult<User>>('/users', params),
  create: (payload: UserCreatePayload) => http.post<User>('/users', payload),
  detail: (id: string) => http.get<User>(`/users/${id}`),
  update: (id: string, payload: Partial<UserCreatePayload>) =>
    http.put<User>(`/users/${id}`, payload),
  toggle: (id: string, status: 'enabled' | 'disabled') =>
    http.patch<User>(`/users/${id}`, { status }),
  assignRoles: (id: string, roleIds: string[]) => http.put<User>(`/users/${id}/roles`, { roleIds }),
  resetPassword: (id: string, password: string) =>
    http.post<void>(`/users/${id}/reset-password`, { password }),
  kick: (id: string) => http.post<void>(`/users/${id}/kick`)
}
