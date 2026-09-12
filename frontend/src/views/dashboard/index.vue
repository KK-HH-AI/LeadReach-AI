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
          <span class="value">{{ userStore.profile?.tenantName || '-' }}</span>
        </div>
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.currentUser') }}</span>
          <span class="value">{{ userStore.profile?.email }}</span>
        </div>
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.role') }}</span>
          <span class="value">{{ userStore.roleCodes.join(', ') }}</span>
        </div>
        <div class="dashboard__info-item">
          <span class="label">{{ t('dashboard.loginTime') }}</span>
          <span class="value">{{ userStore.profile?.lastLoginAt || '-' }}</span>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="dashboard__cards">
      <el-col v-for="(k, i) in statKeys" :key="i" :span="6">
        <el-card shadow="hover" class="dashboard__stat">
          <div class="stat-value">-</div>
          <div class="stat-label">{{ t(k) }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-alert type="info" :closable="false" :title="t('dashboard.placeholderNote')" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer/index.vue'
import { Refresh } from '@element-plus/icons-vue'

const { t } = useI18n()
const userStore = useUserStore()

// 预留首页刷新，后续阶段接管真实统计
function forceRefresh() {}

const statKeys = ref([
  'dashboard.leads',
  'dashboard.customers',
  'dashboard.todaySent',
  'dashboard.followUp'
])
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

  &__cards {
    margin: 16px 0;
  }

  &__stat {
    text-align: center;

    .stat-value {
      font-size: 26px;
      font-weight: 700;
      color: var(--el-color-primary);
    }
    .stat-label {
      margin-top: 6px;
      color: var(--el-text-color-secondary);
      font-size: 13px;
    }
  }
}
</style>
