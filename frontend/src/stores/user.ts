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
    /** 登录：后端仅返回 token+phone，无 me/menus 接口，本地合成档案确保进入系统 */
    async login(payload: LoginPayload) {
      const res = await authApi.login(payload)
      // setToken 第二参可选（后端无 refreshToken）
      this.setToken(res.token, res.refreshToken || '')
      // 本地合成用户档案：phone 即登录凭证
      this.profile = {
        id: res.phone || payload.phone,
        username: res.phone || payload.phone,
        name: res.phone || payload.phone,
        email: '',
        mobile: res.phone || payload.phone,
        tenantId: '',
        isAdmin: true,
        roles: [],
        permissions: []
      }
      this.permissions = []
      this.menus = []
      // 后端暂无租户概念，清空租户状态
      useTenantStore().clear()
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
    /** 拉取动态菜单（后端暂无，失败静默返回空） */
    async fetchMenus() {
      try {
        this.menus = await authApi.menus()
      } catch (e) {
        console.warn('后端未实现 /auth/menus，使用空菜单', e)
        this.menus = []
      }
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
