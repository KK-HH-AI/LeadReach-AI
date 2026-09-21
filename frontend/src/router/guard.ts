import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTenantStore } from '@/stores/tenant'
import { WHITE_LIST } from './routes'

/**
 * 全局守卫：
 * 1. 白名单放行（登录页 / 异常页）
 * 2. 未登录 → 跳登录（带 redirect）
 * 3. 已登录但未拉取用户信息 → 拉取 me + 租户 + 菜单
 * 4. 已登录访问 /login → 跳 /dashboard
 * 5. meta.permission 按钮/路由级权限检查，无权限 → /403
 */
export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const tenantStore = useTenantStore()

    // 白名单：登录、异常页无需鉴权
    if (WHITE_LIST.includes(to.path)) {
      return next()
    }

    // 未登录
    if (!userStore.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    // 已登录但尚未拉取用户信息（刷新页面场景）
    // 优先用本地持久化档案恢复；后端无 /auth/me 时拉取失败也保留档案。
    // 若本地也没有档案（如旧版本残留的 token），视为未登录，回登录页
    if (!userStore.profile) {
      await userStore.fetchProfile()
      if (!userStore.profile) {
        userStore.logout()
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    // 已登录访问登录页 → 回首页
    if (to.path === '/login') {
      return next({ path: '/dashboard' })
    }

    // 老板未创建公司 → 强制引导创建公司（创建公司页本身除外）
    if (userStore.isBoss && !userStore.hasCompany && to.path !== '/company/create') {
      return next({ path: '/company/create' })
    }

    // 权限检查
    const required = to.meta?.permission as string | undefined
    if (required && !userStore.hasPermission(required)) {
      return next('/403')
    }

    // 超级管理员切租户刷信息等场景，保证 x-tenant 头有值
    if (!tenantStore.currentTenantId) {
      tenantStore.setCurrentTenant(userStore.profile?.tenantId ?? '')
    }

    return next()
  })
}
