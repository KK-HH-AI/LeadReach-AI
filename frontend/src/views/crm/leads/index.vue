<template>
  <PageContainer title="线索列表">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建线索</el-button>
    </template>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="我的线索" name="my" />
      <el-tab-pane label="全部线索" name="all" />
    </el-tabs>

    <el-form :inline="true" class="crm-leads__filter">
      <el-form-item label="关键字">
        <el-input
          v-model="query.keyword"
          placeholder="线索标题 / 公司名"
          clearable
          class="crm-leads__input"
          @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable class="crm-leads__select">
          <el-option v-for="s in LEAD_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="tab === 'all'" label="线索池">
        <el-select v-model="query.poolType" placeholder="全部" clearable class="crm-leads__select-sm">
          <el-option label="公共池" value="PUBLIC" />
          <el-option label="私有池" value="PRIVATE" />
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
      @change="onPageChange"
    >
      <el-table-column label="线索 / 公司" min-width="220">
        <template #default="{ row }">
          <div class="crm-leads__title">{{ row.title || '-' }}</div>
          <div class="crm-leads__sub">
            {{ row.companyName || '-' }}{{ row.countryCode ? ` · ${row.countryCode}` : '' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="联系人" min-width="150">
        <template #default="{ row }">
          <div>{{ row.contactName || '-' }}</div>
          <div class="crm-leads__sub">{{ row.contactEmail || '' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="归属" min-width="140">
        <template #default="{ row }">
          <template v-if="row.ownerName">
            <div>{{ row.ownerName }}</div>
            <div class="crm-leads__sub">{{ row.ownerTeamName || '未分团队' }}</div>
          </template>
          <el-tag v-else type="info" size="small">公共池</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近跟进" width="160">
        <template #default="{ row }">{{ formatTime(row.lastFollowAt) }}</template>
      </el-table-column>
      <el-table-column label="保护期至" width="160">
        <template #default="{ row }">{{ formatTime(row.protectedUntil) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as LeadVO)">详情</el-button>
          <el-button v-if="canAssign" link type="primary" @click="openAssign(row as LeadVO)">分配</el-button>
          <el-button
            v-for="s in manualNextStatuses(row as LeadVO)"
            :key="s"
            link
            type="primary"
            @click="onStatusChange(row as LeadVO, s)"
          >
            标记{{ statusLabel(s) }}
          </el-button>
          <el-button v-if="canConvert(row as LeadVO)" link type="success" @click="openConvert(row as LeadVO)">
            转商机
          </el-button>
          <el-button v-if="canRecycle(row as LeadVO)" link type="danger" @click="onRecycle(row as LeadVO)">
            退回公海
          </el-button>
        </template>
      </el-table-column>
    </ProTable>

    <!-- 新建线索 -->
    <el-dialog v-model="createVisible" title="新建线索" width="640px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-divider content-position="left">采购商公司</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="公司名称" prop="company.name">
              <el-input v-model="createForm.company.name" placeholder="如 ABC Trading GmbH" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="官网">
              <el-input v-model="createForm.company.website" placeholder="https://example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="国家代码">
              <el-input v-model="createForm.company.countryCode" placeholder="如 DE" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行业">
              <el-input v-model="createForm.company.industryName" placeholder="如 家居用品" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="createForm.company.address" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">主联系人（可选）</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="createForm.contact.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="createForm.contact.jobTitle" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="createForm.contact.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="createForm.contact.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="LinkedIn">
              <el-input v-model="createForm.contact.linkedinUrl" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否关键人">
              <el-switch v-model="createForm.contact.isKp" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">线索信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="线索标题">
              <el-input v-model="createForm.title" placeholder="缺省用公司名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源">
              <el-select v-model="createForm.sourceType" class="crm-leads__select">
                <el-option v-for="s in SOURCE_TYPE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="createForm.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配线索 -->
    <el-dialog v-model="assignVisible" title="分配线索" width="440px">
      <el-form label-position="top">
        <el-form-item label="线索">
          <el-input :model-value="current?.title" disabled />
        </el-form-item>
        <el-form-item label="分配给销售专员">
          <el-select v-model="assignForm.toUserId" placeholder="选择销售专员" class="crm-leads__select">
            <el-option
              v-for="m in salesOptions"
              :key="m.id"
              :label="`${m.phone}${m.teamName ? ` (${m.teamName})` : ''}`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分配说明">
          <el-input v-model="assignForm.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">确认分配</el-button>
      </template>
    </el-dialog>

    <!-- 转商机 -->
    <el-dialog v-model="convertVisible" title="转商机" width="440px">
      <el-alert
        type="info"
        :closable="false"
        class="crm-leads__hint"
        title="客户开发模块尚未上线，本次仅把线索标记为已转化"
      />
      <el-form label-position="top">
        <el-form-item label="商机名称">
          <el-input v-model="convertForm.opportunityName" placeholder="缺省用线索标题" />
        </el-form-item>
        <el-form-item label="预计金额">
          <el-input-number v-model="convertForm.amount" :min="0" class="crm-leads__number" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model="convertForm.currency" placeholder="如 USD" />
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import ProTable from '@/components/ProTable/index.vue'
import { leadApi } from '@/api/crm/lead'
import {
  LEAD_NEXT_STATUS,
  LEAD_STATUS_OPTIONS,
  SOURCE_TYPE_OPTIONS,
  type AssignableUser,
  type LeadCreatePayload,
  type LeadVO,
  type TagType
} from '@/api/crm/types'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 当前岗位：决定「分配 / 退回公海」等按钮是否可见，最终权限仍由后端校验 */
const jobCode = computed(() => userStore.profile?.position ?? '')
const canAssign = computed(() => jobCode.value === 'BOSS' || jobCode.value === 'SALES_MANAGER')

const tab = ref<'my' | 'all'>('my')
const rows = ref<LeadVO[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: '',
  poolType: ''
})

/** 加载列表：我的线索走 /my，全部线索走 /（后端按岗位叠加数据范围） */
async function load() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    }
    const res =
      tab.value === 'my'
        ? await leadApi.my(params)
        : await leadApi.list({ ...params, poolType: query.poolType || undefined })
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
  query.poolType = ''
  onSearch()
}

function onTabChange() {
  onReset()
}

function onPageChange(p: { page: number; size: number }) {
  query.page = p.page
  query.size = p.size
  load()
}

function openDetail(row: LeadVO) {
  router.push(`/crm/leads/${row.id}`)
}

// ── 新建线索 ────────────────────────────────────────────────
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = reactive<LeadCreatePayload & { contact: NonNullable<LeadCreatePayload['contact']> }>({
  title: '',
  sourceType: 'MANUAL',
  remark: '',
  company: { name: '', website: '', countryCode: '', industryName: '', address: '' },
  contact: {
    name: '',
    jobTitle: '',
    email: '',
    phone: '',
    linkedinUrl: '',
    isKp: false
  }
})

const createRules: FormRules = {
  'company.name': [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
}

function openCreate() {
  createVisible.value = true
  createFormRef.value?.clearValidate()
}

async function onCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    // 联系人四项标识全空时后端会忽略，这里直接不传，语义更清晰
    const c = createForm.contact
    const hasContact = !!(c.name || c.email || c.phone || c.linkedinUrl)
    await leadApi.create({
      title: createForm.title || undefined,
      sourceType: createForm.sourceType || 'MANUAL',
      remark: createForm.remark || undefined,
      company: createForm.company,
      contact: hasContact ? c : undefined
    })
    ElMessage.success('线索已创建')
    createVisible.value = false
    Object.assign(createForm, {
      title: '',
      sourceType: 'MANUAL',
      remark: '',
      company: { name: '', website: '', countryCode: '', industryName: '', address: '' },
      contact: { name: '', jobTitle: '', email: '', phone: '', linkedinUrl: '', isKp: false }
    })
    tab.value = 'my'
    onReset()
  } catch (e) {
    console.error(e)
  } finally {
    creating.value = false
  }
}

// ── 分配 ────────────────────────────────────────────────────
const assignVisible = ref(false)
const assigning = ref(false)
const current = ref<LeadVO | null>(null)
const assignForm = reactive({ toUserId: undefined as number | undefined, reason: '' })

/** 可分配的销售专员：老板得到全公司，主管只得到本团队（范围由后端裁好） */
const salesOptions = ref<AssignableUser[]>([])

async function openAssign(row: LeadVO) {
  current.value = row
  assignForm.toUserId = undefined
  assignForm.reason = ''
  assignVisible.value = true
  if (!salesOptions.value.length) {
    try {
      salesOptions.value = await leadApi.assignableUsers()
    } catch (e) {
      console.error(e)
    }
  }
}

async function onAssign() {
  if (!current.value || !assignForm.toUserId) {
    ElMessage.warning('请选择要分配的销售专员')
    return
  }
  assigning.value = true
  try {
    await leadApi.assign(current.value.id, {
      toUserId: assignForm.toUserId,
      reason: assignForm.reason || undefined
    })
    ElMessage.success('分配成功')
    assignVisible.value = false
    await load()
  } catch (e) {
    console.error(e)
  } finally {
    assigning.value = false
  }
}

// ── 状态流转 ────────────────────────────────────────────────
/** 可手动标记的下一步状态；ASSIGNED 需要指定负责人，只能通过「分配」进入 */
function manualNextStatuses(row: LeadVO): string[] {
  if (!isOwner(row)) return []
  return (LEAD_NEXT_STATUS[row.status] ?? []).filter((s) => s !== 'ASSIGNED')
}

/** 登录时 profile.id 存的就是后端 userId，可直接与 ownerUserId 比对 */
function isOwner(row: LeadVO) {
  return !!row.ownerUserId && String(row.ownerUserId) === userStore.profile?.id
}

async function onStatusChange(row: LeadVO, status: string) {
  try {
    await ElMessageBox.confirm(`确认把线索「${row.title}」标记为${statusLabel(status)}？`, '变更状态', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await leadApi.updateStatus(row.id, { status })
    ElMessage.success('状态已更新')
    await load()
  } catch (e) {
    console.error(e)
  }
}

// ── 退回公海 ────────────────────────────────────────────────
function canRecycle(row: LeadVO) {
  return canAssign.value && row.poolType === 'PRIVATE'
}

async function onRecycle(row: LeadVO) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt(`把「${row.title}」退回公共池？`, '退回公海', {
      inputPlaceholder: '回收原因（可选）',
      type: 'warning'
    })
    reason = res.value ?? ''
  } catch {
    return
  }
  try {
    await leadApi.recycle(row.id, reason || undefined)
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

function canConvert(row: LeadVO) {
  return isOwner(row) && row.status === 'QUALIFIED'
}

function openConvert(row: LeadVO) {
  current.value = row
  convertForm.opportunityName = row.title
  convertForm.amount = 0
  convertForm.currency = 'USD'
  convertVisible.value = true
}

async function onConvert() {
  if (!current.value) return
  converting.value = true
  try {
    await leadApi.convert(current.value.id, {
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
.crm-leads {
  &__filter {
    margin-bottom: 4px;
  }

  &__input {
    width: 220px;
  }

  &__select {
    width: 100%;
  }

  &__select-sm {
    width: 130px;
  }

  &__number {
    width: 100%;
  }

  &__title {
    font-weight: 500;
  }

  &__sub {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  &__hint {
    margin-bottom: 12px;
  }
}
</style>