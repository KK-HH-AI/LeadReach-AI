<template>
  <div class="page-container">
    <div class="page-container__header">
      <el-page-header v-if="showBack" @back="$emit('back')">
        <template #content>
          <span class="page-container__title">{{ title }}</span>
        </template>
      </el-page-header>
      <span v-else class="page-container__title">{{ title }}</span>
      <div class="page-container__actions">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.description" class="page-container__desc">
      <slot name="description" />
    </div>
    <div class="page-container__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  showBack?: boolean
}>()

defineEmits<{
  back: []
}>()
</script>

<style scoped lang="scss">
.page-container {
  padding: 16px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
  }

  &__actions {
    display: flex;
    gap: 8px;
  }

  &__desc {
    margin-bottom: 12px;
    color: var(--el-text-color-secondary);
  }

  &__body {
    background: var(--el-bg-color);
    border-radius: 6px;
    padding: 16px;
  }
}
</style>
