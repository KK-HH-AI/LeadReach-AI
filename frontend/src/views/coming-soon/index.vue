<template>
  <div class="coming-soon">
    <el-empty :description="t('comingSoon.desc')" :image-size="120">
      <template #image>
        <el-icon :size="120" color="#c0c4cc"><Tools /></el-icon>
      </template>
    </el-empty>
    <div class="coming-soon__title">{{ t('comingSoon.title') }}</div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Tools } from '@element-plus/icons-vue'

const route = useRoute()
const { t } = useI18n()
void route
</script>

<style scoped lang="scss">
.coming-soon {
  height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &__title {
    font-size: 18px;
    color: var(--el-text-color-secondary);
  }
}
</style>
