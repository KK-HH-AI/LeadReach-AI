<template>
  <PageContainer title="公共线索池">
    <template #description>
      未分配的线索都在这里。销售专员可领取，领取后 15 天内未跟进会自动退回本池。
    </template>

    <el-form :inline="true" class="crm-pool__filter">
      <el-form-item label="关键字">
        <el-input
          v-model="query.keyword"
          placeholder="线索标题 / 公司名"
          clearable
          class="crm-pool__input"
          @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable class="crm-pool__select">
          <el-option v-for="s in LEAD_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <ProTable
      :data="rows"
      :total="total"
      :loading="loading"
      :page="query.page"
      :size="query.size"
      empty-text="公共池暂无线索"
      @change="onPageChange"
    >
      <el-table-column label="线索 / 公司" min-width="240">
        <template #default="{ row }">
          <div class="crm-pool__title">{{ row.title || '-' }}</div>
          <div class="crm-pool__sub">
            {{ row.companyName || '-' }}{{ row.countryCode ? ` · ${row.countryCode}` : '' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="联系人" min-width="160">
        <template #default="{ row }">
          <div>{{ row.contactName || '-' }}</div>
          <div class="crm-pool__sub">{{ row.contactEmail || '' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入池时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as LeadVO)">详情</el-button>
          <el-button v-if="canClaim" link type="success" @click="onClaim(row as LeadVO)">领取</el-button>
        </template>
      </el-table-column>
    </ProTable>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import ProTable from '@/components/ProTable/index.vue'
import { leadApi } from '@/api/crm/lead'
import { LEAD_STATUS_OPTIONS, type LeadVO, type TagType } from '@/api/crm/types'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 仅销售专员能领取，与后端 claim 接口的校验保持一致 */
const canClaim = computed(() => userStore.profile?.position === 'SALES_REP')

const rows = ref<LeadVO[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

async function load() {
  loading.value = true
  try {
    const res = await leadApi.pool({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
    rows.value = res.list ?? []
    total.value = res.total ?? 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  query.keyword = ''
  query.status = ''
  onSearch()
}

function onPageChange(p: { page: number; size: number }) {
  query.page = p.page
  query.size = p.size
  load()
}

function openDetail(row: LeadVO) {
  router.push(`/crm/leads/${row.id}`)
}

async function onClaim(row: LeadVO) {
  try {
    await ElMessageBox.confirm(`领取线索「${row.title}」？领取后将归你负责。`, '领取线索', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await leadApi.claim(row.id)
    ElMessage.success('领取成功，已加入我的线索')
    await load()
  } catch (e) {
    console.error(e)
    // 并发场景下可能被别人抢先领走，刷新列表让用户看到最新状态
    await load()
  }
}

function statusLabel(status: string) {
  return LEAD_STATUS_OPTIONS.find((s) => s.value === status)?.label ?? status
}

function statusTag(status: string): TagType {
  return LEAD_STATUS_OPTIONS.find((s) => s.value === status)?.tag ?? 'info'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<style scoped lang="scss">
.crm-pool {
  &__filter {
    margin-bottom: 4px;
  }

  &__input {
    width: 220px;
  }

  &__select {
    width: 150px;
  }

  &__title {
    font-weight: 500;
  }

  &__sub {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}
</style>