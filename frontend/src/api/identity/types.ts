import type { PageQuery } from '../types'

/** 登录返回（后端 AuthController.login 实际结构：token + userId + phone + role + position + tenantId） */
export interface LoginResult {
  token: string
  refreshToken?: string
  /** 当前用户ID */
  userId?: number
  /** 身份角色：BOSS(老板) / EMPLOYEE(员工) */
  role?: string
  /** 岗位：BOSS / SALES_MANAGER / SALES_REP / OPERATOR */
  position?: string
  /** 所属团队ID，空 = 未分团队 */
  teamId?: number
  /** 所属公司ID，空 = 尚未加入公司 */
  tenantId?: string
  expiresIn?: number
  phone?: string
}

/** 当前用户 / 认证信息 */
export interface UserProfile {
  id: string
  username: string
  name: string
  email: string
  mobile?: string
  avatar?: string
  tenantId: string
  tenantName?: string
  /** 身份角色：BOSS / EMPLOYEE */
  role?: string
  /** 岗位：BOSS / SALES_MANAGER / SALES_REP / OPERATOR */
  position?: string
  /** 所属团队ID，空 = 未分团队 */
  teamId?: number
  isAdmin?: boolean
  roles: string[]
  permissions: string[]
  lastLoginAt?: string
}

/** 认证信息（登录成功后拉取的 me） */
export interface AuthMeResult {
  user: UserProfile
  permissions: string[]
}

export interface Tenant {
  id: string
  code: string
  name: string
  status: 'enabled' | 'disabled'
  plan?: string
  adminAccount?: string
  adminEmail?: string
  createdAt: string
}

export interface TenantQuery extends PageQuery {
  status?: 'enabled' | 'disabled'
}

export interface TenantCreatePayload {
  code: string
  name: string
  adminAccount: string
  adminEmail: string
  initialPassword: string
  plan: string
}

export interface TenantUpdatePayload {
  name?: string
  status?: 'enabled' | 'disabled'
  plan?: string
}

export interface User {
  id: string
  username: string
  name: string
  email: string
  mobile?: string
  roles: string[]
  tenantId?: string
  tenantName?: string
  status: 'enabled' | 'disabled'
  lastLoginAt?: string
  createdAt?: string
}

export interface UserQuery extends PageQuery {
  status?: 'enabled' | 'disabled'
  role?: string
}

export interface UserCreatePayload {
  username: string
  name: string
  email: string
  mobile?: string
  roleIds: string[]
  initialPassword: string
}

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  userCount: number
  status: 'enabled' | 'disabled'
}

export interface PermissionNode {
  id: string
  name: string
  code: string
  type: 'menu' | 'button'
  parentId?: string | null
  children?: PermissionNode[]
}
