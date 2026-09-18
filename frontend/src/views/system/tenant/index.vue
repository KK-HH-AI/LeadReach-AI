<template>
  <PageContainer :title="t('menu.tenant')">
    <template #actions>
      <PermissionButton
        permission="system:tenant:create"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        {{ t('common.add') }}
      </PermissionButton>
    </template>

    <ProTable
      :data="list"
      :total="total"
      :loading="loading"
      :page="query.page"
      :size="query.size"
      @change="onQuery"
    >
      <el-table-column prop="code" :label="t('tenant.code')" width="140" />
      <el-table-column prop="name" :label="t('tenant.name')" min-width="160" />
      <el-table-column prop="plan" :label="t('tenant.plan')" width="120" />
      <el-table-column prop="status" :label="t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'enabled' ? 'success' : 'danger'">
            {{ row.status === 'enabled' ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="t('common.createdAt')" width="180" />
      <el-table-column :label="t('common.operate')" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as unknown as Tenant)">{{
            t('common.edit')
          }}</el-button>
          <el-button
            link
            :type="row.status === 'enabled' ? 'danger' : 'success'"
            @click="onToggle(row as unknown as Tenant)"
          >
            {{ row.status === 'enabled' ? t('common.disabled') : t('common.enabled') }}
          </el-button>
          <el-button link type="warning" @click="onResetPassword(row as unknown as Tenant)">{{
            t('tenant.resetPassword')
          }}</el-button>
        </template>
      </el-table-column>
    </ProTable>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('common.edit') : t('common.add')"
      width="520px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item :label="t('tenant.code')" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('tenant.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <template v-if="!isEdit">
          <el-form-item :label="t('tenant.adminAccount')" prop="adminAccount">
            <el-input v-model="form.adminAccount" />
          </el-form-item>
          <el-form-item :label="t('tenant.adminEmail')" prop="adminEmail">
            <el-input v-model="form.adminEmail" />
          </el-form-item>
          <el-form-item :label="t('tenant.initialPassword')" prop="initialPassword">
            <el-input v-model="form.initialPassword" type="password" />
          </el-form-item>
        </template>
        <el-form-item :label="t('tenant.plan')" prop="plan">
          <el-select v-model="form.plan" style="width: 100%">
            <el-option label="标准版" value="标准版" />
            <el-option label="专业版" value="专业版" />
            <el-option label="旗舰版" value="旗舰版" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{
          t('common.save')
        }}</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import ProTable from '@/components/ProTable/index.vue'
import PermissionButton from '@/components/PermissionButton/index.vue'
import { tenantApi } from '@/api/identity/tenant'
import type { Tenant } from '@/api/identity/types'

const { t } = useI18n()
const list = ref<Tenant[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref('')
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  code: '',
  name: '',
  adminAccount: '',
  adminEmail: '',
  initialPassword: '',
  plan: '标准版'
})

const formRules: FormRules = {
  code: [{ required: true, message: '请输入租户码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  adminAccount: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  initialPassword: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await tenantApi.list(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onQuery(q: { page: number; size: number }) {
  query.page = q.page
  query.size = q.size
  load()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    code: '',
    name: '',
    adminAccount: '',
    adminEmail: '',
    initialPassword: '',
    plan: '标准版'
  })
  dialogVisible.value = true
}

function openEdit(row: Tenant) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, { code: row.code, name: row.name, plan: row.plan })
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await tenantApi.update(editingId.value, { name: form.name, plan: form.plan })
    } else {
      await tenantApi.create({ ...form })
    }
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onToggle(row: Tenant) {
  const next = row.status === 'enabled' ? 'disabled' : 'enabled'
  await tenantApi.toggle(row.id, next)
  ElMessage.success(t('common.success'))
  load()
}

async function onResetPassword(row: Tenant) {
  await ElMessageBox.confirm(t('tenant.resetPassword'), t('common.confirm'), { type: 'warning' })
  await tenantApi.resetAdminPassword(row.id)
  ElMessage.success(t('common.success'))
}

onMounted(load)
</script>
