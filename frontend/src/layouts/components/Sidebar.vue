<template>
  <el-scrollbar class="sidebar">
    <el-menu
      class="sidebar__menu"
      :default-active="activeMenu"
      :collapse="isCollapsed"
      :collapse-transition="false"
      router
      unique-opened
    >
      <template v-for="item in menuTree" :key="item.path">
        <el-sub-menu v-if="item.children?.length" :index="item.path">
          <template #title>
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <span>{{ resolveTitle(item) }}</span>
          </template>
          <menu-item-node v-for="child in item.children" :key="child.path" :item="child" />
        </el-sub-menu>
        <el-menu-item v-else :index="item.path">
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          <template #title>{{ resolveTitle(item) }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </el-scrollbar>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import type { MenuItem } from '@/api/identity/auth'
import MenuItemNode from './MenuItem.vue'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

const isCollapsed = computed(() => appStore.isCollapsed)
const activeMenu = computed(() => route.path)

/** 按权限过滤菜单树：仅保留有子节点的组或叶子菜单 */
const menuTree = computed<MenuItem[]>(() => {
  const filter = (nodes: MenuItem[] | undefined): MenuItem[] => {
    return (nodes ?? [])
      .filter((n) => userStore.hasPermission(n.permission))
      .map((n) => ({ ...n, children: n.children ? filter(n.children) : undefined }))
      .filter((n) => !n.children || n.children.length > 0)
  }
  return filter(userStore.menus)
})

/** 路由标题：优先 i18n，缺失时用菜单名 */
function resolveTitle(item: MenuItem) {
  return titleResolver[item.path] ?? item.name
}

// 骨架阶段静态映射；后续权限菜单由后端统一驱动后可移除
const titleResolver: Record<string, string> = {
  '/dashboard': '工作台',
  '/system': '系统管理',
  '/system/tenant': '租户管理',
  '/system/user': '用户管理',
  '/system/role': '角色管理',
  '/system/permission': '权限菜单'
}
</script>

<style scoped lang="scss">
.sidebar {
  height: 100%;
  background-color: var(--el-bg-color);

  &__menu {
    border-right: none;
    // 显式声明浅色配色，不依赖画布色，避免深色模式下菜单发黑
    --el-menu-bg-color: var(--el-bg-color);
    --el-menu-text-color: var(--el-text-color-regular);
    --el-menu-hover-bg-color: var(--el-fill-color-light);
    --el-menu-active-color: var(--el-color-primary);
    --el-menu-item-height: 44px;
    --el-menu-sub-item-height: 40px;

    // 菜单项做成圆角块，视觉上更轻
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      margin: 4px 8px;
      border-radius: 6px;
    }

    // 选中态：主色浅底 + 加粗，优先级高于 hover
    :deep(.el-menu-item.is-active) {
      background-color: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      font-weight: 600;
    }

    // 折叠时去掉左右外边距，避免图标被挤偏
    &.el-menu--collapse {
      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        margin: 4px;
      }
    }
  }
}
</style>
