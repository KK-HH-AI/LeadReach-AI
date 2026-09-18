<template>
  <el-button v-permission="permission" v-bind="$attrs">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
/** 带按钮级权限控制的按钮，无权限自动移除 */
defineProps<{
  permission?: string
}>()
</script>
