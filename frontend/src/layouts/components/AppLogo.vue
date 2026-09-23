<template>
  <div class="app-logo">
    <el-icon :size="collapsed ? 24 : 28" color="var(--el-color-primary)">
      <Ship />
    </el-icon>
    <span v-if="!collapsed" class="app-logo__text">{{ title }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ collapsed?: boolean }>()

const title = '获客运营平台'
</script>

<style scoped lang="scss">
.app-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);

  &__text {
    font-weight: 700;
    font-size: 16px;
    white-space: nowrap;
  }
}
</style>
