<template>
  <div class="navbar">
    <div class="navbar__left">
      <el-icon class="navbar__collapse" @click="appStore.toggleSidebar()">
        <Expand v-if="appStore.isCollapsed" />
        <Fold v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="(item, idx) in breadcrumbs" :key="idx">{{
          item
        }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="navbar__right">
      <!-- 租户切换：仅超管可见 -->
      <el-dropdown v-if="tenantStore.isSuperAdmin" @command="onSwitchTenant">
        <span class="navbar__tenant">
          <el-icon><OfficeBuilding /></el-icon>
          {{ currentTenantName }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="t in tenants" :key="t.id" :command="t.id">{{
              t.name
            }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <span v-else-if="tenantStore.currentTenantId" class="navbar__tenant">
        <el-icon><OfficeBuilding /></el-icon>
        {{ currentTenantName }}
      </span>

      <!-- 语言切换 -->
      <el-dropdown @command="onSwitchLang">
        <span class="navbar__lang">{{ languageLabel }}</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="zh-CN">简体中文</el-dropdown-item>
            <el-dropdown-item command="en-US">English</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- 用户菜单 -->
      <el-dropdown @command="onUserCommand">
        <span class="navbar__user">
          <el-avatar :size="28">{{ avatarText }}</el-avatar>
          <span>{{ userStore.profile?.name }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useTenantStore } from '@/stores/tenant'
import { tenantApi } from '@/api/identity/tenant'
import type { Tenant } from '@/api/identity/types'

const route = useRoute()
const router = useRouter()
const { locale } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const tenantStore = useTenantStore()

const tenants = ref<Tenant[]>([])

const breadcrumbs = computed(() => {
  // 骨架阶段简化：取路由 meta 的标题层级
  return route.matched.filter((r) => r.meta?.title).map((r) => (r.meta.title as string) ?? '')
})

const currentTenantName = computed(() => userStore.profile?.tenantName || '-')
const languageLabel = computed(() => (locale.value === 'en-US' ? 'EN' : '中文'))
const avatarText = computed(() => (userStore.profile?.name || 'U').slice(0, 1))

function loadTenants() {
  tenantApi
    .list({ page: 1, size: 100 })
    .then((res) => (tenants.value = res.list))
    .catch(() => {})
}

function onSwitchTenant(id: string) {
  tenantStore.switchTenant(id)
  userStore
    .fetchProfile()
    .then(() => ElMessage.success('已切换租户'))
    .catch(() => {})
}

function onSwitchLang(lang: string) {
  locale.value = lang
  appStore.setLanguage(lang)
  localStorage.setItem('lang', lang)
}

function onUserCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  if (tenantStore.isSuperAdmin) loadTenants()
})
</script>

<style scoped lang="scss">
.navbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--el-border-color-light);

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__collapse {
    font-size: 20px;
    cursor: pointer;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  &__tenant,
  &__lang,
  &__user {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
  }

  &__user {
    .el-avatar {
      vertical-align: middle;
    }
  }
}
</style>
