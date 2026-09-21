import type { RouteRecordRaw } from 'vue-router'

/**
 * 静态路由（骨架阶段全部为静态）。
 * 动态菜单项通过 MenuItem.path 与下方 componentMap 关联后，
 * 在添加动态路由时复用对应组件。
 */
export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true, ignoreAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', hidden: true, ignoreAuth: true }
  },
  {
    path: '/',
    name: 'Root',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer', permission: 'dashboard' }
      },
      {
        path: 'team',
        name: 'Team',
        component: () => import('@/views/team/index.vue'),
        meta: { title: '团队管理', icon: 'User', permission: 'team' }
      },
      {
        path: 'company/create',
        name: 'CompanyCreate',
        component: () => import('@/views/company/create.vue'),
        meta: { title: '创建公司', hidden: true }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/layouts/components/AppMain.vue'),
        meta: { title: '系统管理', icon: 'Setting' },
        redirect: '/system/tenant',
        children: [
          {
            path: 'tenant',
            name: 'SystemTenant',
            component: () => import('@/views/system/tenant/index.vue'),
            meta: { title: '租户管理', permission: 'system:tenant:*' }
          },
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/views/system/user/index.vue'),
            meta: { title: '用户管理', permission: 'system:user:*' }
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role/index.vue'),
            meta: { title: '角色管理', permission: 'system:role:*' }
          },
          {
            path: 'permission',
            name: 'SystemPermission',
            component: () => import('@/views/system/permission/index.vue'),
            meta: { title: '权限菜单', permission: 'system:perm:*' }
          }
        ]
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', hidden: true, permission: 'profile' }
      },
      // 后续阶段占位：路由 + 菜单可见，页面走 ComingSoon
      {
        path: 'crm',
        name: 'CrmPlaceholder',
        component: () => import('@/views/coming-soon/index.vue'),
        meta: { title: '客户管理', icon: 'Files' }
      },
      {
        path: 'channel',
        name: 'ChannelPlaceholder',
        component: () => import('@/views/coming-soon/index.vue'),
        meta: { title: '获客渠道', icon: 'Search' }
      },
      {
        path: 'email',
        name: 'EmailPlaceholder',
        component: () => import('@/views/coming-soon/index.vue'),
        meta: { title: '邮件中心', icon: 'Message' }
      },
      {
        path: 'acquisition',
        name: 'AcquisitionPlaceholder',
        component: () => import('@/views/coming-soon/index.vue'),
        meta: { title: '自动开发', icon: 'MagicStick' }
      },
      {
        path: 'operation',
        name: 'OperationPlaceholder',
        component: () => import('@/views/coming-soon/index.vue'),
        meta: { title: '运营系统', icon: 'Guide' }
      },
      {
        path: 'agent',
        name: 'AgentPlaceholder',
        component: () => import('@/views/coming-soon/index.vue'),
        meta: { title: 'AI Agent', icon: 'Cpu' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', hidden: true, ignoreAuth: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true, ignoreAuth: true }
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('@/views/error/500.vue'),
    meta: { title: '服务异常', hidden: true, ignoreAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { hidden: true }
  }
]

/** 登录白名单 */
export const WHITE_LIST = ['/login', '/register', '/403', '/404', '/500']
