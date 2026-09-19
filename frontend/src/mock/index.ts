/**
 * 阶段0 数据 mock。
 * 在 VITE_MOCK=true 时以 axios-mock-adapter 拦截 request 层的 HTTP 调用，
 * 使前端在 identity 服务交付前即可端到端联调。
 * 生产（VITE_MOCK=false）下此文件不会被加载。
 */
import MockAdapter from 'axios-mock-adapter'
import { axiosInstance, genTraceId } from '@/api/request'
import type { ApiResponse } from '@/api/types'
import type { UserPermission, TenantSeed, RoleSeed } from './seed-types'
import { buildPermissionsTree, buildMenus } from './menus'

/** 内存数据源（后续可在此扩展 CRUD 变更） */
const tenants: TenantSeed[] = [
  {
    id: 't-1',
    code: 'PLATFORM',
    name: '平台租户',
    status: 'enabled',
    plan: '平台',
    createdAt: '2026-09-01 10:00:00'
  },
  {
    id: 't-2',
    code: 'DEMO',
    name: '演示外贸公司',
    status: 'enabled',
    plan: '旗舰版',
    createdAt: '2026-09-05 14:30:00'
  }
]

const roles: RoleSeed[] = [
  {
    id: 'r-1',
    name: '超级管理员',
    code: 'SUPER_ADMIN',
    description: '平台超级管理员',
    status: 'enabled',
    permissions: '*',
    userCount: 1
  },
  {
    id: 'r-2',
    name: '租户管理员',
    code: 'TENANT_ADMIN',
    description: '租户管理员',
    status: 'enabled',
    permissions: ['dashboard', 'system:user:*', 'system:role:*', 'system:perm:*'],
    userCount: 1
  },
  {
    id: 'r-3',
    name: '业务员',
    code: 'SALESPERSON',
    description: '普通业务员',
    status: 'enabled',
    permissions: ['dashboard'],
    userCount: 1
  }
]

const users: Array<UserPermission & { password: string }> = [
  {
    id: 'u-1',
    username: 'admin',
    name: '平台管理员',
    email: 'admin@platform.local',
    roleCodes: ['SUPER_ADMIN'],
    tenantId: 't-1',
    tenantName: '平台租户',
    status: 'enabled',
    isAdmin: true,
    lastLoginAt: '2026-09-12 09:00:00',
    password: 'admin123'
  },
  {
    id: 'u-2',
    username: 'demo',
    name: '演示管理员',
    email: 'demo@demo.local',
    roleCodes: ['TENANT_ADMIN'],
    tenantId: 't-2',
    tenantName: '演示外贸公司',
    status: 'enabled',
    isAdmin: false,
    lastLoginAt: '2026-09-12 08:30:00',
    password: 'admin123'
  }
]

function permissionsFor(user?: (typeof users)[number]): string[] {
  if (!user) return []
  if (user.isAdmin)
    return [
      'dashboard',
      'system:tenant:*',
      'system:user:*',
      'system:role:*',
      'system:perm:*',
      'profile',
      'crm:*',
      'channel:*',
      'email:*',
      'acquisition:*',
      'operation:*',
      'agent:*'
    ]
  const merged = roles
    .filter((r) => user.roleCodes.includes(r.code))
    .flatMap((r) => (r.permissions === '*' ? ['*'] : r.permissions))
  return Array.from(new Set(merged))
}

function ok<T>(data: T): [number, ApiResponse<T>] {
  return [200, { code: 0, message: 'ok', data, traceId: genTraceId() }]
}

function fail<T = null>(message: string, code = 500): [number, ApiResponse<T>] {
  return [code, { code: -1, message, data: null as T, traceId: genTraceId() }]
}

/** 模拟短信验证码存储：手机号 → 验证码 */
const smsCodes: Record<string, string> = {}

export function setupMock() {
  const mock = new MockAdapter(axiosInstance, { delayResponse: 200 })

  // ---------- 认证 ----------
  // 模拟发送验证码：验证码固定 123456，存入内存
  mock.onPost('/api/v1/auth/send-code').reply((config) => {
    const body = JSON.parse(config.data || '{}')
    if (!body.phone) return fail('手机号不能为空', 400)
    smsCodes[body.phone] = '123456'
    return ok(null)
  })

  mock.onPost('/api/v1/auth/login').reply((config) => {
    const body = JSON.parse(config.data || '{}')
    const expected = smsCodes[body.phone]
    if (!expected || expected !== body.code) return fail('验证码错误或已过期', 401)
    const user = users.find((u) => u.username === body.phone) || users[0]
    delete smsCodes[body.phone]
    return ok({
      token: `mock-token-${user.id}`,
      refreshToken: `mock-refresh-${user.id}`,
      tenantId: user.tenantId ?? '',
      expiresIn: 7200,
      phone: body.phone
    })
  })

  mock.onPost('/api/v1/auth/logout').reply(() => ok(null))
  mock.onPost('/api/v1/auth/refresh').reply(() =>
    ok({
      token: 'mock-token-refreshed',
      refreshToken: 'mock-refresh-new',
      tenantId: 't-2',
      expiresIn: 7200
    })
  )

  mock.onGet('/api/v1/auth/me').reply((config) => {
    const auth = config.headers?.Authorization as string | undefined
    const uid = auth?.split('-').pop()?.replace('Bearer mock-token-', '') ?? ''
    const user = users.find((u) => u.id.includes(uid)) || users[0]
    const profile = {
      id: user.id,
      username: user.username,
      name: user.name,
      email: user.email,
      mobile: '',
      tenantId: user.tenantId ?? '',
      tenantName: user.tenantName,
      isAdmin: user.isAdmin,
      roles: user.roleCodes,
      permissions: permissionsFor(user),
      lastLoginAt: user.lastLoginAt
    }
    return ok({ user: profile, permissions: profile.permissions })
  })

  mock.onGet('/api/v1/auth/menus').reply(() => ok(buildMenus()))

  // ---------- 租户 ----------
  mock.onGet('/api/tenants').reply((config) => {
    const params = config.params || {}
    let list = tenants
    if (params.name) list = list.filter((t) => t.name.includes(params.name))
    if (params.status) list = list.filter((t) => t.status === params.status)
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    return ok({ list: list.slice(start, start + size), total: list.length, page, size })
  })

  mock.onPost('/api/tenants').reply((config) => {
    const body = JSON.parse(config.data || '{}')
    const t: TenantSeed = {
      id: `t-${Date.now()}`,
      code: body.code,
      name: body.name,
      status: 'enabled',
      plan: body.plan || '标准版',
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19)
    }
    tenants.unshift(t)
    return ok(t)
  })

  mock.onPut(/\/api\/tenants\/(t-[\w-]+)$/).reply((config) => {
    const id = (config.url || '').match(/(t-\d+)$/)?.[1]
    const t = tenants.find((x) => x.id === id)
    if (!t) return fail('租户不存在', 404)
    Object.assign(t, JSON.parse(config.data || '{}'))
    return ok(t)
  })
  mock.onPost(/\/api\/tenants\/[^/]+\/reset-password$/).reply(() => ok(null))

  // ---------- 用户 ----------
  mock.onGet('/api/users').reply((config) => {
    const params = config.params || {}
    const page = params.page || 1
    const size = params.size || 10
    const start = (page - 1) * size
    const safe = users.map((u) => {
      const { password, ...rest } = u
      void password
      return rest
    })
    return ok({ list: safe.slice(start, start + size), total: safe.length, page, size })
  })
  mock.onPost('/api/users').reply((config) => {
    const body = JSON.parse(config.data || '{}')
    const u: UserPermission & { password: string } = {
      id: `u-${Date.now()}`,
      username: body.username,
      name: body.name,
      email: body.email,
      roleCodes: ['SALESPERSON'],
      tenantId: 't-2',
      tenantName: '演示外贸公司',
      status: 'enabled',
      isAdmin: false,
      lastLoginAt: '',
      password: body.initialPassword || 'admin123'
    }
    users.push(u)
    return ok(u)
  })
  mock.onPatch(/\/api\/users\/u-\d+$/).reply((config) => {
    const id = (config.url || '').match(/(u-\d+)$/)?.[1]
    const u = users.find((x) => x.id === id)
    if (!u) return fail('用户不存在', 404)
    Object.assign(u, JSON.parse(config.data || '{}'))
    return ok(u)
  })
  mock.onPut(/\/api\/users\/u-\d+$/).reply(() => ok(null))
  mock.onPut(/\/api\/users\/u-\d+\/roles$/).reply(() => ok(null))
  mock.onPost(/\/api\/users\/u-\d+\/reset-password$/).reply((config) => {
    const body = JSON.parse(config.data || '{}')
    const id = (config.url || '').match(/(u-\d+)$/)?.[1]
    const u = users.find((x) => x.id === id)
    if (!u) return fail('用户不存在', 404)
    void body
    return ok(null)
  })
  mock.onPost(/\/api\/users\/u-\d+\/kick$/).reply(() => ok(null))

  // ---------- 角色 ----------
  mock.onGet('/api/roles').reply(() => ok(roles))
  mock.onPost('/api/roles').reply(() => ok(null))
  mock.onPut(/\/api\/roles\/r-\d+$/).reply(() => ok(null))
  mock.onDelete(/\/api\/roles\/r-\d+$/).reply(() => ok(null))
  mock.onPut(/\/api\/roles\/r-\d+\/permissions$/).reply(() => ok(null))

  // ---------- 权限 ----------
  mock.onGet('/api/permissions/tree').reply(() => ok(buildPermissionsTree()))
}
