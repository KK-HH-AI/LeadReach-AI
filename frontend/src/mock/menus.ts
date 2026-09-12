import type { MenuItem } from '@/api/identity/auth'
import type { PermissionSeed } from './seed-types'

/**
 * 由后端 /auth/menus 返回的菜单树（mock 版）。
 * 与 router/routes.ts 静态路由的 path 保持一致，方便动态渲染。
 */
export function buildMenus(): MenuItem[] {
  return [
    {
      id: 'm-dashboard',
      name: '工作台',
      path: '/dashboard',
      icon: 'Odometer',
      order: 1,
      permission: 'dashboard'
    },
    {
      id: 'm-system',
      name: '系统管理',
      path: '/system',
      icon: 'Setting',
      order: 2,
      permission: 'system:*',
      children: [
        {
          id: 'm-tenant',
          name: '租户管理',
          path: '/system/tenant',
          order: 1,
          permission: 'system:tenant:*'
        },
        {
          id: 'm-user',
          name: '用户管理',
          path: '/system/user',
          order: 2,
          permission: 'system:user:*'
        },
        {
          id: 'm-role',
          name: '角色管理',
          path: '/system/role',
          order: 3,
          permission: 'system:role:*'
        },
        {
          id: 'm-perm',
          name: '权限菜单',
          path: '/system/permission',
          order: 4,
          permission: 'system:perm:*'
        }
      ]
    },
    { id: 'm-crm', name: '客户管理', path: '/crm', icon: 'Files', order: 3, permission: 'crm:*' },
    {
      id: 'm-channel',
      name: '获客渠道',
      path: '/channel',
      icon: 'Search',
      order: 4,
      permission: 'channel:*'
    },
    {
      id: 'm-email',
      name: '邮件中心',
      path: '/email',
      icon: 'Message',
      order: 5,
      permission: 'email:*'
    },
    {
      id: 'm-acq',
      name: '自动开发',
      path: '/acquisition',
      icon: 'MagicStick',
      order: 6,
      permission: 'acquisition:*'
    },
    {
      id: 'm-op',
      name: '运营系统',
      path: '/operation',
      icon: 'Guide',
      order: 7,
      permission: 'operation:*'
    },
    {
      id: 'm-agent',
      name: 'AI Agent',
      path: '/agent',
      icon: 'Cpu',
      order: 8,
      permission: 'agent:*'
    }
  ]
}

/**
 * 权限树（mock 版），供角色管理里的权限分配勾选。
 */
export function buildPermissionsTree(): PermissionSeed[] {
  const menus: PermissionSeed[] = [
    { id: 'p-dashboard', name: '工作台', code: 'dashboard', type: 'menu', parentId: null },
    {
      id: 'p-system',
      name: '系统管理',
      code: 'system',
      type: 'menu',
      parentId: null,
      children: [
        {
          id: 'p-system-tenant',
          name: '租户管理',
          code: 'system:tenant',
          type: 'menu',
          parentId: 'p-system',
          children: [
            {
              id: 'p-system-tenant-c',
              name: '新建租户',
              code: 'system:tenant:create',
              type: 'button',
              parentId: 'p-system-tenant'
            },
            {
              id: 'p-system-tenant-e',
              name: '编辑租户',
              code: 'system:tenant:update',
              type: 'button',
              parentId: 'p-system-tenant'
            }
          ]
        },
        {
          id: 'p-system-user',
          name: '用户管理',
          code: 'system:user',
          type: 'menu',
          parentId: 'p-system',
          children: [
            {
              id: 'p-system-user-c',
              name: '新建用户',
              code: 'system:user:create',
              type: 'button',
              parentId: 'p-system-user'
            },
            {
              id: 'p-system-user-a',
              name: '分配角色',
              code: 'system:user:assign',
              type: 'button',
              parentId: 'p-system-user'
            }
          ]
        },
        {
          id: 'p-system-role',
          name: '角色管理',
          code: 'system:role',
          type: 'menu',
          parentId: 'p-system',
          children: [
            {
              id: 'p-system-role-a',
              name: '分配权限',
              code: 'system:role:assign',
              type: 'button',
              parentId: 'p-system-role'
            }
          ]
        }
      ]
    },
    { id: 'p-profile', name: '个人中心', code: 'profile', type: 'menu', parentId: null }
  ]
  return menus
}
