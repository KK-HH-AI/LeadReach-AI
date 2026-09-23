<template>
  <PageContainer title="线索看板">
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </template>

    <el-radio-group v-model="view" @change="load">
      <el-radio-button value="my">我的线索</el-radio-button>
      <el-radio-button v-if="canViewTeam" value="team">团队看板</el-radio-button>
      <el-radio-button v-if="isBoss" value="overview">公司总览</el-radio-button>
    </el-radio-group>

    <div v-loading="loading" class="crm-dash__body">
      <el-row :gutter="16" class="crm-dash__cards">
        <el-col :span="6">
          <el-card shadow="never">
            <div class="crm-dash__label">线索总量</div>
            <div class="crm-dash__value">{{ stats?.totalCount ?? 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="crm-dash__label">公共池待领取</div>
            <div class="crm-dash__value">{{ stats?.poolCount ?? 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="crm-dash__label">今日新增</div>
            <div class="crm-dash__value">{{ stats?.todayNewCount ?? 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="crm-dash__label">今日跟进</div>
            <div class="crm-dash__value">{{ stats?.todayFollowCount ?? 0 }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><b>状态分布</b></template>
            <div v-for="item in statusRows" :key="item.value" class="crm-dash__row">
              <span class="crm-dash__row-label">{{ item.label }}</span>
              <el-progress
                :percentage="item.percent"
                :stroke-width="14"
                :show-text="false"
                class="crm-dash__progress"
              />
              <span class="crm-dash__row-count">{{ item.count }}</span>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="never">
            <template #header><b>成员线索量排行</b></template>
            <el-table :data="stats?.memberRanking ?? []" empty-text="暂无数据">
              <el-table-column label="排名" width="70">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column label="成员" min-width="140">
                <template #default="{ row }">{{ row.userName || row.userId }}</template>
              </el-table-column>
              <el-table-column label="团队" prop="teamName" min-width="120" />
              <el-table-column label="线索数" prop="count" width="100" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-card v-if="showExpiring" shadow="never" class="crm-dash__expiring">
        <template #header>
          <b>即将超出保护期</b>
          <span class="crm-dash__sub">（{{ expiringSoonDays }} 天内，共 {{ stats?.expiringSoon?.length ?? 0 }} 条）</span>
        </template>
        <el-table :data="stats?.expiringSoon ?? []" empty-text="没有即将超期的线索">
          <el-table-column label="线索" min-width="220">
            <template #default="{ row }">
              <el-link type="primary" @click="goDetail(row.leadId)">{{ row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="负责人" min-width="140">
            <template #default="{ row }">{{ row.ownerName || row.ownerUserId }}</template>
          </el-table-column>
          <el-table-column label="保护期截止" width="180">
            <template #default="{ row }">{{ formatTime(row.protectedUntil) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-alert
        v-if="!stats && !loading"
        type="warning"
        :closable="false"
        title="看板数据加载失败，请确认后端 crm-service 是否已启动"
      />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { dashboardApi } from '@/api/crm/dashboard'
import { LEAD_STATUS_OPTIONS, type DashboardStats } from '@/api/crm/types'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const jobCode = computed(() => userStore.profile?.position ?? '')
const isBoss = computed(() => jobCode.value === 'BOSS')
const canViewTeam = computed(() => isBoss.value || jobCode.value === 'SALES_MANAGER')

const view = ref<'my' | 'team' | 'overview'>('my')
const stats = ref<DashboardStats | null>(null)
const loading = ref(false)

/** 保护期窗口与后端 crm.lead.expiring-soon-days 保持一致 */
const expiringSoonDays = 3
/** 只有团队视角下后端才返回排行与超期预警 */
const showExpiring = computed(() => view.value !== 'overview')

async function load() {
  loading.value = true
  try {
    const api = view.value === 'team' ? dashboardApi.team : view.value === 'overview' ? dashboardApi.overview : dashboardApi.my
    stats.value = await api()
  } catch (e) {
    console.error(e)
    stats.value = null
  } finally {
    loading.value = false
  }
}

/** 状态分布：按总量算百分比，用于进度条展示 */
const statusRows = computed(() => {
  const counts = stats.value?.statusCounts ?? {}
  const total = Object.values(counts).reduce((a, b) => a + b, 0)
  return LEAD_STATUS_OPTIONS.map((s) => {
    const count = counts[s.value] ?? 0
    return {
      value: s.value,
      label: s.label,
      count,
      percent: total ? Math.round((count / total) * 100) : 0
    }
  })
})

function goDetail(leadId: number) {
  router.push(`/crm/leads/${leadId}`)
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<style scoped lang="scss">
.crm-dash {
  &__body {
    margin-top: 16px;
    min-height: 200px;
  }

  &__cards {
    margin-bottom: 16px;
  }

  &__label {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  &__value {
    margin-top: 8px;
    font-size: 26px;
    font-weight: 600;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
  }

  &__row-label {
    width: 90px;
    font-size: 13px;
  }

  &__progress {
    flex: 1;
  }

  &__row-count {
    width: 48px;
    text-align: right;
    font-size: 13px;
  }

  &__expiring {
    margin-top: 16px;
  }

  &__sub {
    margin-left: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}
</style>