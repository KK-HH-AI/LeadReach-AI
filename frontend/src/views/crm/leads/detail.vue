<template>
  <PageContainer title="线索详情" show-back @back="router.back()">
    <template #actions>
      <el-button v-if="canFollow" type="primary" :icon="Plus" @click="openFollow">记录跟进</el-button>
      <el-button v-if="canClaim" type="success" :loading="claiming" @click="onClaim">领取线索</el-button>
      <el-button v-if="canConvert" type="success" @click="openConvert">转商机</el-button>
      <el-button v-if="canRecycle" type="danger" @click="onRecycle">退回公海</el-button>
    </template>

    <div v-loading="loading">
      <template v-if="lead">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="线索标题">{{ lead.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(lead.status)">{{ statusLabel(lead.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="线索池">
            <el-tag :type="lead.poolType === 'PUBLIC' ? 'info' : 'primary'">
              {{ lead.poolType === 'PUBLIC' ? '公共池' : '私有池' }}
            </el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="公司">{{ lead.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="国家">{{ lead.countryCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="行业">{{ lead.industryName || '-' }}</el-descriptions-item>

          <el-descriptions-item label="官网">
            <el-link v-if="lead.website" :href="lead.website" target="_blank" type="primary">
              {{ lead.website }}
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ lead.address || '-' }}</el-descriptions-item>

          <el-descriptions-item label="负责人">
            {{ lead.ownerName || '公共池未分配' }}
            <span v-if="lead.ownerTeamName">（{{ lead.ownerTeamName }}）</span>
          </el-descriptions-item>
          <el-descriptions-item label="保护期至">{{ formatTime(lead.protectedUntil) }}</el-descriptions-item>
          <el-descriptions-item label="转为商机时间">{{ formatTime(lead.convertedAt) }}</el-descriptions-item>

          <el-descriptions-item label="来源">
            {{ sourceLabel(lead.sourceType) }}
            <el-link v-if="lead.sourceUrl" :href="lead.sourceUrl" target="_blank" type="primary">
              原始链接
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(lead.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="最近更新">{{ formatTime(lead.updatedAt) }}</el-descriptions-item>

          <el-descriptions-item label="备注" :span="3">{{ lead.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="tab" class="crm-detail__tabs">
          <el-tab-pane :label="`联系人 (${lead.contacts?.length ?? 0})`" name="contacts">
            <el-table :data="lead.contacts ?? []" empty-text="暂无联系人">
              <el-table-column label="姓名" min-width="120">
                <template #default="{ row }">
                  {{ row.name || '-' }}
                  <el-tag v-if="row.isKp" type="warning" size="small">关键人</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="职位" prop="jobTitle" min-width="120" />
              <el-table-column label="部门" prop="department" min-width="120" />
              <el-table-column label="邮箱" prop="email" min-width="180" />
              <el-table-column label="电话" prop="phone" min-width="140" />
              <el-table-column label="LinkedIn" min-width="160">
                <template #default="{ row }">
                  <el-link v-if="row.linkedinUrl" :href="row.linkedinUrl" target="_blank" type="primary">
                    主页
                  </el-link>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`跟进记录 (${lead.activities?.length ?? 0})`" name="activities">
            <el-timeline v-if="lead.activities?.length">
              <el-timeline-item
                v-for="a in lead.activities"
                :key="a.id"
                :timestamp="formatTime(a.activityAt)"
                placement="top"
              >
                <el-tag size="small">{{ followLabel(a.activityType) }}</el-tag>
                <span class="crm-detail__owner">{{ a.ownerName || '' }}</span>
                <div class="crm-detail__content">{{ a.content || '-' }}</div>
                <div v-if="a.result" class="crm-detail__sub">结果：{{ a.result }}</div>
                <div v-if="a.nextFollowAt" class="crm-detail__sub">
                  下次跟进：{{ formatTime(a.nextFollowAt) }}
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="还没有跟进记录" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane :label="`流转日志 (${lead.assignLogs?.length ?? 0})`" name="logs">
            <el-table :data="lead.assignLogs ?? []" empty-text="暂无流转记录">
              <el-table-column label="动作" width="110">
                <template #default="{ row }">
                  <el-tag :type="actionTag(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="从" min-width="120">
                <template #default="{ row }">{{ row.fromOwnerName || '公共池' }}</template>
              </el-table-column>
              <el-table-column label="到" min-width="120">
                <template #default="{ row }">{{ row.toOwnerName || '公共池' }}</template>
              </el-table-column>
              <el-table-column label="原因" prop="reason" min-width="160" />
              <el-table-column label="时间" width="170">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-empty v-else-if="!loading" description="线索不存在" />
    </div>

    <!-- 记录跟进 -->
    <el-dialog v-model="followVisible" title="记录跟进" width="480px">
      <el-form ref="followFormRef" :model="followForm" :rules="followRules" label-position="top">
        <el-form-item label="跟进方式" prop="activityType">
          <el-select v-model="followForm.activityType" class="crm-detail__select">
            <el-option v-for="t in FOLLOW_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容" prop="content">
          <el-input v-model="followForm.content" type="textarea" :rows="3" placeholder="沟通要点、客户反馈等" />
        </el-form-item>
        <el-form-item label="跟进结果">
          <el-input v-model="followForm.result" placeholder="如：已加 WhatsApp，等待报价" />
        </el-form-item>
        <el-form-item label="下次跟进时间">
          <el-date-picker
            v-model="followForm.nextFollowAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="可选"
            class="crm-detail__select"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followVisible = false">取消</el-button>
        <el-button type="primary" :loading="following" @click="onFollow">保存</el-button>
      </template>
    </el-dialog>

    <!-- 转商机 -->
    <el-dialog v-model="convertVisible" title="转商机" width="440px">
      <el-alert
        type="info"
        :closable="false"
        class="crm-detail__hint"
        title="客户开发模块尚未上线，本次仅把线索标记为已转化"
      />
      <el-form label-position="top">
        <el-form-item label="商机名称">
          <el-input v-model="convertForm.opportunityName" />
        </el-form-item>
        <el-form-item label="预计金额">
          <el-input-number v-model="convertForm.amount" :min="0" class="crm-detail__select" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model="convertForm.currency" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="convertVisible = false">取消</el-button>
        <el-button type="primary" :loading="converting" @click="onConvert">确认转化</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { activityApi } from '@/api/crm/activity'
import { leadApi } from '@/api/crm/lead'
import {
  FOLLOW_TYPE_OPTIONS,
  LEAD_STATUS_OPTIONS,
  SOURCE_TYPE_OPTIONS,
  type LeadDetailVO,
  type TagType
} from '@/api/crm/types'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const leadId = Number(route.params.id)
const lead = ref<LeadDetailVO | null>(null)
const loading = ref(false)
const tab = ref('contacts')

const jobCode = computed(() => userStore.profile?.position ?? '')
const isOwner = computed(
  () => !!lead.value?.ownerUserId && String(lead.value.ownerUserId) === userStore.profile?.id
)
/** 只有负责人能写跟进、转商机 */
const canFollow = computed(() => isOwner.value)
const canConvert = computed(() => isOwner.value && lead.value?.status === 'QUALIFIED')
/** 老板/主管可退回公海，且线索必须在私有池 */
const canRecycle = computed(
  () =>
    (jobCode.value === 'BOSS' || jobCode.value === 'SALES_MANAGER') &&
    lead.value?.poolType === 'PRIVATE'
)
/** 销售专员可领取公共池里的无主线索 */
const canClaim = computed(
  () => jobCode.value === 'SALES_REP' && lead.value?.poolType === 'PUBLIC' && !lead.value?.ownerUserId
)

async function load() {
  if (!leadId) return
  loading.value = true
  try {
    lead.value = await leadApi.detail(leadId)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// ── 记录跟进 ────────────────────────────────────────────────
const followVisible = ref(false)
const following = ref(false)
const followFormRef = ref<FormInstance>()
const followForm = reactive({
  activityType: 'CALL',
  content: '',
  result: '',
  nextFollowAt: ''
})

const followRules: FormRules = {
  activityType: [{ required: true, message: '请选择跟进方式', trigger: 'change' }],
  content: [{ required: true, message: '请填写跟进内容', trigger: 'blur' }]
}

function openFollow() {
  followForm.activityType = 'CALL'
  followForm.content = ''
  followForm.result = ''
  followForm.nextFollowAt = ''
  followVisible.value = true
  followFormRef.value?.clearValidate()
}

async function onFollow() {
  const valid = await followFormRef.value?.validate().catch(() => false)
  if (!valid) return
  following.value = true
  try {
    await activityApi.create({
      bizType: 'LEAD',
      bizId: leadId,
      activityType: followForm.activityType,
      content: followForm.content,
      result: followForm.result || undefined,
      nextFollowAt: followForm.nextFollowAt || undefined
    })
    ElMessage.success('跟进已记录')
    followVisible.value = false
    await load()
  } catch (e) {
    console.error(e)
  } finally {
    following.value = false
  }
}

// ── 领取 ────────────────────────────────────────────────────
const claiming = ref(false)

async function onClaim() {
  claiming.value = true
  try {
    await leadApi.claim(leadId)
    ElMessage.success('领取成功')
    await load()
  } catch (e) {
    console.error(e)
  } finally {
    claiming.value = false
  }
}

// ── 退回公海 ────────────────────────────────────────────────
async function onRecycle() {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt('退回公共池后归属将被清空，确认继续？', '退回公海', {
      inputPlaceholder: '回收原因（可选）',
      type: 'warning'
    })
    reason = res.value ?? ''
  } catch {
    return
  }
  try {
    await leadApi.recycle(leadId, reason || undefined)
    ElMessage.success('已退回公共池')
    await load()
  } catch (e) {
    console.error(e)
  }
}

// ── 转商机 ──────────────────────────────────────────────────
const convertVisible = ref(false)
const converting = ref(false)
const convertForm = reactive({ opportunityName: '', amount: 0, currency: 'USD' })

function openConvert() {
  convertForm.opportunityName = lead.value?.title ?? ''
  convertForm.amount = 0
  convertForm.currency = 'USD'
  convertVisible.value = true
}

async function onConvert() {
  converting.value = true
  try {
    await leadApi.convert(leadId, {
      opportunityName: convertForm.opportunityName || undefined,
      amount: convertForm.amount || undefined,
      currency: convertForm.currency || undefined
    })
    ElMessage.success('已标记为已转化')
    convertVisible.value = false
    await load()
  } catch (e) {
    console.error(e)
  } finally {
    converting.value = false
  }
}

// ── 展示工具 ────────────────────────────────────────────────
function statusLabel(status?: string) {
  if (!status) return '-'
  return LEAD_STATUS_OPTIONS.find((s) => s.value === status)?.label ?? status
}

function statusTag(status?: string): TagType {
  if (!status) return 'info'
  return LEAD_STATUS_OPTIONS.find((s) => s.value === status)?.tag ?? 'info'
}

function sourceLabel(source?: string) {
  if (!source) return '-'
  return SOURCE_TYPE_OPTIONS.find((s) => s.value === source)?.label ?? source
}

function followLabel(type: string) {
  return FOLLOW_TYPE_OPTIONS.find((t) => t.value === type)?.label ?? type
}

function actionLabel(action: string) {
  const map: Record<string, string> = {
    ASSIGN: '分配',
    CLAIM: '领取',
    RECYCLE: '退回公海'
  }
  return map[action] ?? action
}

function actionTag(action: string): TagType {
  if (action === 'RECYCLE') return 'danger'
  if (action === 'CLAIM') return 'success'
  return 'primary'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<style scoped lang="scss">
.crm-detail {
  &__tabs {
    margin-top: 16px;
  }

  &__owner {
    margin-left: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  &__content {
    margin-top: 4px;
  }

  &__sub {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  &__select {
    width: 100%;
  }

  &__hint {
    margin-bottom: 12px;
  }
}
</style>