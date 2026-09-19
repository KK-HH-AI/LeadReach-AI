import { http } from '../request'
import type { LoginResult, AuthMeResult, UserProfile } from './types'

export interface LoginPayload {
  phone: string
  code: string
}

export const authApi = {
  /** 发送短信验证码（后端 AuthController.sendCode，网关前缀 /api/v1） */
  sendCode: (phone: string) => http.post<null>('/v1/auth/send-code', { phone }),
  /** 手机号+验证码登录（后端 AuthController.login） */
  login: (payload: LoginPayload) => http.post<LoginResult>('/v1/auth/login', payload),
  logout: () => http.post<void>('/v1/auth/logout'),
  refresh: (refreshToken: string) => http.post<LoginResult>('/v1/auth/refresh', { refreshToken }),
  me: () => http.get<AuthMeResult>('/v1/auth/me'),
  menus: () => http.get<MenuItem[]>('/v1/auth/menus')
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
