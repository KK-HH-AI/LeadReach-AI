import { defineStore } from 'pinia'
import { authApi, type LoginPayload, type MenuItem } from '@/api/identity/auth'
import type { UserProfile } from '@/api/identity/types'
import { useTenantStore } from './tenant'

const TOKEN_KEY = 'access_token'
const REFRESH_KEY = 'refresh_token'

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
    profile: null,
    permissions: [],
    menus: [],
    language: localStorage.getItem('lang') || 'zh-CN'
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    roleCodes: (s) => s.profile?.roles ?? [],
    allPermissions: (s) => s.permissions
  },
  actions: {
    setToken(token: string, refreshToken: string) {
      this.token = token
      this.refreshToken = refreshToken
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(REFRESH_KEY, refreshToken)
    },
    /** 登录：登录成功后拉取用户信息 + 租户 + 菜单 */
    async login(payload: LoginPayload) {
      const res = await authApi.login(payload)
      this.setToken(res.token, res.refreshToken)
      const tenantStore = useTenantStore()
      tenantStore.setCurrentTenant(res.tenantId)
      await this.fetchProfile()
    },
    /** 拉取当前用户信息与权限 */
    async fetchProfile() {
      const tenantStore = useTenantStore()
      const me = await authApi.me()
      this.profile = me.user
      this.permissions = me.permissions
      if (me.user.tenantId) {
        tenantStore.setCurrentTenant(me.user.tenantId, !!me.user.isAdmin)
      }
      await this.fetchMenus()
    },
    /** 拉取动态菜单 */
    async fetchMenus() {
      this.menus = await authApi.menus()
    },
    /** 刷新 token，成功返回 true */
    async tryRefresh(): Promise<boolean> {
      if (!this.refreshToken) return false
      try {
        const res = await authApi.refresh(this.refreshToken)
        this.setToken(res.token, res.refreshToken)
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
      useTenantStore().clear()
    }
  }
})
