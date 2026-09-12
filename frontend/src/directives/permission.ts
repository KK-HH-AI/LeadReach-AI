import type { Directive, App } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限指令：v-permission="'system:user:create'"
 * 无权限则移除该元素。
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    if (userStore.profile?.isAdmin) return
    const codes = binding.value
    const list = Array.isArray(codes) ? codes : [codes]
    const allowed = list.some((c) => userStore.hasPermission(c))
    if (!allowed) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default function initDirectives(app: App) {
  app.directive('permission', permission)
}
