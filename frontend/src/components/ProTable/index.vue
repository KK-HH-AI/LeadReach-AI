<template>
  <div class="pro-table">
    <div v-if="$slots.toolbar" class="pro-table__toolbar">
      <slot name="toolbar" />
    </div>
    <el-table v-loading="loading" v-bind="$attrs" :data="data as unknown as any[]" :border="border">
      <slot />
      <template #empty>
        <el-empty :description="emptyText" :image-size="60" />
      </template>
    </el-table>
    <div v-if="showPagination" class="pro-table__pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="onChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts" generic="T">
import { ref, watch } from 'vue'

/**
 * 通用表格：为后续 CRM / 渠道 / 邮件等表格页预留。
 * 用法：<ProTable :data="rows" :total="total" :loading="loading" @change="onQuery">
 */
const props = withDefaults(
  defineProps<{
    data: T[]
    total?: number
    loading?: boolean
    showPagination?: boolean
    border?: boolean
    emptyText?: string
    page?: number
    size?: number
  }>(),
  {
    data: () => [],
    total: 0,
    loading: false,
    showPagination: true,
    border: false,
    emptyText: '暂无数据',
    page: 1,
    size: 10
  }
)

const emit = defineEmits<{
  change: [query: { page: number; size: number }]
}>()

const page = ref(props.page)
const size = ref(props.size)

watch(
  () => [props.page, props.size],
  ([np, ns]) => {
    page.value = np
    size.value = ns
  }
)

function onChange(p: number) {
  emit('change', { page: p, size: size.value })
}
function onSizeChange(s: number) {
  size.value = s
  emit('change', { page: 1, size: s })
}
</script>

<style scoped lang="scss">
.pro-table {
  &__toolbar {
    margin-bottom: 12px;
  }

  &__pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
