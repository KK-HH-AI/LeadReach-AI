/** mock 用到的结构化数据描述 */

export interface TenantSeed {
  id: string
  code: string
  name: string
  status: 'enabled' | 'disabled'
  plan: string
  createdAt: string
}

export interface UserSeed {
  id: string
  username: string
  name: string
  email: string
  roleCodes: string[]
  tenantId: string
  tenantName: string
  status: 'enabled' | 'disabled'
  isAdmin: boolean
  lastLoginAt: string
}

export interface UserPermission extends UserSeed {}

export interface RoleSeed {
  id: string
  name: string
  code: string
  description: string
  status: 'enabled' | 'disabled'
  /** '*' 表示全权限 */
  permissions: string[] | '*'
  userCount: number
}

/** 权限树节点 */
export interface PermissionSeed {
  id: string
  name: string
  code: string
  type: 'menu' | 'button'
  parentId: string | null
  children?: PermissionSeed[]
}
