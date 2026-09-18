<template>
  <div class="tabs-bar">
    <el-tag
      v-for="tab in appStore.tabs"
      :key="tab.path"
      :class="{ 'is-active': isActive(tab.path) }"
      class="tabs-bar__item"
      size="small"
      closable
      effect="plain"
      @click="go(tab.path)"
      @close="close(tab.path)"
    >
      {{ tab.title }}
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

function getTitle(path: string): string {
  const matched = route.matched.filter((r) => r.meta?.title)
  return (matched[matched.length - 1]?.meta.title as string) ?? path
}

// 路由变化时追加标签页（首页/工作台默认常显，隐藏页不入标签）
watch(
  () => route.fullPath,
  () => {
    if (route.meta.hidden) return
    appStore.addTab({ path: route.fullPath, title: getTitle(route.fullPath) })
  },
  { immediate: true }
)

function isActive(path: string) {
  return route.fullPath === path
}

function go(path: string) {
  if (route.fullPath !== path) router.push(path)
}

function close(path: string) {
  const next = appStore.removeTab(path)
  if (isActive(path) && next) router.push(next)
}
</script>

<style scoped lang="scss">
.tabs-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  flex-wrap: wrap;

  &__item {
    cursor: pointer;

    &.is-active {
      background-color: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      border-color: var(--el-color-primary);
    }
  }
}
</style>
