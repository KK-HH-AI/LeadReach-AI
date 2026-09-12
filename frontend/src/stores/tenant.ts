import { defineStore } from 'pinia'

interface TenantState {
  /** 当前租户 ID */
  currentTenantId: string
  /** 是否平台超管（可跨租户切换） */
  isSuperAdmin: boolean
}

const TENANT_KEY = 'tenant'

export const useTenantStore = defineStore('tenant', {
  state: (): TenantState => ({
    currentTenantId: localStorage.getItem(TENANT_KEY) || '',
    isSuperAdmin: false
  }),
  actions: {
    setCurrentTenant(id: string, isSuperAdmin = false) {
      this.currentTenantId = id
      this.isSuperAdmin = isSuperAdmin
      if (id) {
        localStorage.setItem(TENANT_KEY, id)
      } else {
        localStorage.removeItem(TENANT_KEY)
      }
    },
    /** 超管切换租户：调用后需重新拉取用户信息与菜单 */
    switchTenant(id: string) {
      this.setCurrentTenant(id, this.isSuperAdmin)
    },
    clear() {
      this.currentTenantId = ''
      this.isSuperAdmin = false
      localStorage.removeItem(TENANT_KEY)
    }
  }
})
