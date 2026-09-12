import { http } from '../request'
import type { LoginResult, AuthMeResult, UserProfile } from './types'

export interface LoginPayload {
  /** 超管可留空 */
  tenantCode?: string
  username: string
  password: string
  remember?: boolean
}

export const authApi = {
  login: (payload: LoginPayload) => http.post<LoginResult>('/auth/login', payload),
  logout: () => http.post<void>('/auth/logout'),
  refresh: (refreshToken: string) => http.post<LoginResult>('/auth/refresh', { refreshToken }),
  me: () => http.get<AuthMeResult>('/auth/me'),
  menus: () => http.get<MenuItem[]>('/auth/menus')
}

/** 后端返回的菜单项（前端按图标/排序映射渲染） */
export interface MenuItem {
  id: string
  name: string
  path: string
  icon?: string
  /** 排序 */
  order?: number
  /** 所需权限点，前端过滤用 */
  permission?: string
  children?: MenuItem[]
}

export type { UserProfile }
