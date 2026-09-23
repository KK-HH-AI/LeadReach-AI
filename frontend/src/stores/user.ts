import { defineStore } from 'pinia'
import { authApi, type LoginPayload, type RegisterPayload, type MenuItem } from '@/api/identity/auth'
import type { UserProfile } from '@/api/identity/types'
import { useTenantStore } from './tenant'

const TOKEN_KEY = 'access_token'
const REFRESH_KEY = 'refresh_token'
/** 登录档案持久化：后端暂无 /auth/me 接口，刷新页面时直接恢复本地档案，避免权限判断失效 */
const PROFILE_KEY = 'user_profile'

function loadProfile(): UserProfile | null {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY) || 'null')
  } catch {
    return null
  }
}

/**
 * 本地菜单兜底：真实后端暂未实现 /auth/menus，
 * 拉取失败时用本地静态菜单，保证侧边栏可用
 */
const FALLBACK_MENUS: MenuItem[] = [
  { id: 'dashboard', name: '工作台', path: '/dashboard', icon: 'Odometer', permission: 'dashboard' },
  { id: 'team', name: '团队管理', path: '/team', icon: 'User', permission: 'team' },
  {
    id: 'crm',
    name: '客户管理',
    path: '/crm',
    icon: 'Files',
    permission: 'crm:*',
    children: [
      { id: 'crm-leads', name: '线索列表', path: '/crm/leads', permission: 'crm:*' },
      { id: 'crm-pool', name: '公共线索池', path: '/crm/pool', permission: 'crm:*' },
      { id: 'crm-dashboard', name: '线索看板', path: '/crm/dashboard', permission: 'crm:*' }
    ]
  },
  {
    id: 'system',
    name: '系统管理',
    path: '/system',
    icon: 'Setting',
    permission: 'system:tenant:*',
    children: [
      { id: 'tenant', name: '租户管理', path: '/system/tenant', permission: 'system:tenant:*' },
      { id: 'user', name: '用户管理', path: '/system/user', permission: 'system:user:*' },
      { id: 'role', name: '角色管理', path: '/system/role', permission: 'system:role:*' },
      { id: 'permission', name: '权限菜单', path: '/system/permission', permission: 'system:perm:*' }
    ]
  }
]

interface UserState {
  token: string
  refreshToken: string
  profile: UserProfile | null
  permissions: string[]
  menus: MenuItem[]
  language: string
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_KEY) || '',
    profile: loadProfile(),
    permissions: [],
    menus: [],
    language: localStorage.getItem('lang') || 'zh-CN'
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    roleCodes: (s) => s.profile?.roles ?? [],
    allPermissions: (s) => s.permissions,
    /** 身份角色：BOSS / EMPLOYEE */
    role: (s) => s.profile?.role ?? '',
    /** 是否已加入公司（有租户上下文） */
    hasCompany: (s) => !!s.profile?.tenantId,
    /** 是否老板 */
    isBoss: (s) => s.profile?.role === 'BOSS'
  },
  actions: {
    setToken(token: string, refreshToken: string) {
      this.token = token
      this.refreshToken = refreshToken
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(REFRESH_KEY, refreshToken)
    },
    /** 登录：保存 token + 身份角色 + 租户，供工作台/路由守卫使用 */
    async login(payload: LoginPayload) {
      const res = await authApi.login(payload)
      // setToken 第二参可选（后端无 refreshToken）
      this.setToken(res.token, res.refreshToken || '')
      // 本地合成用户档案：phone 即登录凭证，userId/role/position/tenantId 来自登录返回
      this.profile = {
        id: String(res.userId ?? res.phone ?? payload.phone),
        username: res.phone || payload.phone,
        name: res.phone || payload.phone,
        email: '',
        mobile: res.phone || payload.phone,
        tenantId: res.tenantId ?? '',
        role: res.role ?? '',
        position: res.position ?? '',
        teamId: res.teamId,
        isAdmin: true,
        roles: [],
        permissions: []
      }
      this.permissions = []
      this.menus = []
      useTenantStore().setCurrentTenant(res.tenantId ?? '')
      // 持久化档案，刷新页面后无需 /auth/me 即可恢复登录态
      localStorage.setItem(PROFILE_KEY, JSON.stringify(this.profile))
    },
    /** 注册账号（老板/员工）：注册成功后在登录页提示去登录 */
    async register(payload: RegisterPayload) {
      await authApi.register(payload)
    },
    /** 拉取当前用户信息与权限（后端暂无 me/menus 接口，失败时保留本地档案，不抛出） */
    async fetchProfile() {
      const tenantStore = useTenantStore()
      try {
        const me = await authApi.me()
        this.profile = me.user
        this.permissions = me.permissions
        if (me.user.tenantId) {
          tenantStore.setCurrentTenant(me.user.tenantId, !!me.user.isAdmin)
        }
        await this.fetchMenus()
      } catch (e) {
        console.warn('后端未实现 /auth/me，使用本地档案', e)
      }
    },
    /** 拉取动态菜单（后端暂无，失败回退到本地静态菜单） */
    async fetchMenus() {
      try {
        this.menus = await authApi.menus()
        if (!this.menus.length) this.menus = FALLBACK_MENUS
      } catch (e) {
        console.warn('后端未实现 /auth/menus，使用本地菜单', e)
        this.menus = FALLBACK_MENUS
      }
    },
    /** 刷新 token，成功返回 true */
    async tryRefresh(): Promise<boolean> {
      if (!this.refreshToken) return false
      try {
        const res = await authApi.refresh(this.refreshToken)
        this.setToken(res.token, res.refreshToken || '')
        return true
      } catch {
        return false
      }
    },
    hasPermission(code?: string): boolean {
      if (!code) return true
      if (this.profile?.isAdmin) return true
      return this.permissions.includes(code)
    },
    logout() {
      this.token = ''
      this.refreshToken = ''
      this.profile = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(PROFILE_KEY)
      useTenantStore().clear()
    }
  }
})
