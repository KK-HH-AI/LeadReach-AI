<template>
  <PageContainer :title="t('menu.dashboard')">
    <template #actions>
      <el-button :icon="Refresh" @click="forceRefresh">刷新</el-button>
    </template>

    <el-card shadow="never">
      <h2 class="dashboard__welcome">
        {{ t('dashboard.welcome') }}，<el-tag type="primary">{{ userStore.profile?.name }}</el-tag>
      </h2>
      <div class="dashboard__info">
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.currentTenant') }}</span>
          <span class="value">{{ userStore.profile?.tenantName || userStore.profile?.tenantId || '-' }}</span>
        </div>
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.currentUser') }}</span>
          <span class="value">{{ userStore.profile?.mobile || userStore.profile?.email }}</span>
        </div>
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.role') }}</span>
          <span class="value">{{ userStore.profile?.role || '-' }}</span>
        </div>
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.loginTime') }}</span>
          <span class="value">{{ userStore.profile?.lastLoginAt || '-' }}</span>
        </div>
      </div>
    </el-card>

    <!-- 老板看板：团队概况（user-service 团队成员列表） -->
    <el-card v-if="userStore.isBoss" shadow="never" class="dashboard__panel">
      <template #header>
        <div class="dashboard__panel-header">
          <b>{{ t('dashboard.bossTeamTitle') }}</b>
          <el-button type="primary" link @click="router.push('/team')">
            {{ t('dashboard.goToTeam') }}
          </el-button>
        </div>
      </template>
      <el-alert
        v-if="teamLoadError"
        type="warning"
        :closable="false"
        :title="t('dashboard.bossTeamError')"
      />
      <el-table
        v-else
        :data="members"
        v-loading="teamLoading"
        :empty-text="t('dashboard.bossTeamEmpty')"
      >
        <el-table-column label="手机号" prop="phone" min-width="140" />
        <el-table-column label="岗位" min-width="120">
          <template #default="{ row }">{{ positionLabel(row.position) }}</template>
        </el-table-column>
        <el-table-column label="所属团队" min-width="140">
          <template #default="{ row }">{{ row.teamName || '-' }}</template>
        </el-table-column>
        <el-table-column label="加入时间" min-width="160">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 员工看板：我的岗位与团队归属 -->
    <el-card v-else shadow="never" class="dashboard__panel">
      <template #header>
        <div class="dashboard__panel-header">
          <b>{{ t('dashboard.employeeTitle') }}</b>
          <el-button type="primary" link @click="router.push('/profile')">
            {{ t('dashboard.goToProfile') }}
          </el-button>
        </div>
      </template>
      <el-alert
        v-if="!userStore.hasCompany"
        type="info"
        :closable="false"
        :title="t('dashboard.noTeamHint')"
      />
      <div v-else class="dashboard__info">
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.myPosition') }}</span>
          <span class="value">{{ positionLabel(userStore.profile?.position) }}</span>
        </div>
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.myTeam') }}</span>
          <span class="value">{{ userStore.profile?.teamId ?? '-' }}</span>
        </div>
      </div>
    </el-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer/index.vue'
import { teamApi, POSITION_OPTIONS, type TeamMember } from '@/api/identity/team'
import { Refresh } from '@element-plus/icons-vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const members = ref<TeamMember[]>([])
const teamLoading = ref(false)
const teamLoadError = ref(false)

function positionLabel(position?: string) {
  return POSITION_OPTIONS.find((p) => p.value === position)?.label || position || '-'
}

/** 老板加载团队成员概况；未创建公司等场景降级为提示 */
async function loadTeam() {
  teamLoading.value = true
  teamLoadError.value = false
  try {
    members.value = await teamApi.list()
  } catch {
    teamLoadError.value = true
  } finally {
    teamLoading.value = false
  }
}

function forceRefresh() {
  if (userStore.isBoss) loadTeam()
}

onMounted(() => {
  if (userStore.isBoss && userStore.hasCompany) loadTeam()
})
</script>

<style scoped lang="scss">
.dashboard {
  &__welcome {
    margin: 0 0 20px;
  }

  &__info {
    display: flex;
    gap: 32px;
    flex-wrap: wrap;
  }

  &__info-item {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .label {
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }
    .value {
      font-size: 15px;
      font-weight: 500;
    }
  }

  &__panel {
    margin-top: 16px;
  }

  &__panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}
</style>
