import { http } from '../request'
import type { LoginResult, AuthMeResult, UserProfile } from './types'

export interface LoginPayload {
  phone: string
  password: string
}

export interface RegisterPayload {
  phone: string
  code: string
  /** 登录密码（6-20 位） */
  password: string
  /** 身份角色：BOSS(老板) / EMPLOYEE(员工) */
  role: 'BOSS' | 'EMPLOYEE'
}

export const authApi = {
  /**
   * 发送短信验证码（后端 AuthController.sendCode）
   * 模拟阶段后端直接返回验证码 smsCode，前端展示；生产环境接真实短信后无此字段
   */
  sendCode: (phone: string) =>
    http.post<{ phone: string; smsCode: string }>('/v1/auth/send-code', { phone }),
  /** 手机号+验证码登录（后端 AuthController.login） */
  login: (payload: LoginPayload) => http.post<LoginResult>('/v1/auth/login', payload),
  /** 注册账号（老板/员工），注册时未加入任何公司 */
  register: (payload: RegisterPayload) => http.post<null>('/v1/auth/register', payload),
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
