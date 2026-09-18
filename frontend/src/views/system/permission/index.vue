<template>
  <PageContainer :title="t('menu.permission')">
    <el-alert
      type="info"
      :closable="false"
      style="margin-bottom: 12px"
      title="骨架阶段只读展示权限点树，供角色分配使用"
    />
    <el-tree
      :data="tree"
      node-key="id"
      :props="{ label: 'name', children: 'children' }"
      default-expand-all
    >
      <template #default="{ data }">
        <span class="perm-node">
          <el-tag :type="data.type === 'menu' ? 'primary' : 'success'" size="small">
            {{ data.type === 'menu' ? t('permission.menu') : t('permission.button') }}
          </el-tag>
          <span>{{ data.name }}</span>
          <span class="perm-node__code">{{ data.code }}</span>
        </span>
      </template>
    </el-tree>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PageContainer from '@/components/PageContainer/index.vue'
import { permissionApi } from '@/api/identity/role'
import type { PermissionNode } from '@/api/identity/types'

const { t } = useI18n()
const tree = ref<PermissionNode[]>([])

onMounted(async () => {
  tree.value = await permissionApi.tree()
})
</script>

<style scoped lang="scss">
.perm-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  &__code {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}
</style>
