<template>
  <PageContainer :title="t('menu.user')">
    <template #actions>
      <PermissionButton
        permission="system:user:create"
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
      <el-table-column prop="username" :label="t('user.username')" width="120" />
      <el-table-column prop="name" :label="t('user.name')" min-width="120" />
      <el-table-column prop="email" :label="t('user.email')" min-width="180" />
      <el-table-column :label="t('user.role')" width="150">
        <template #default="{ row }">{{ row.roles?.join(', ') }}</template>
      </el-table-column>
      <el-table-column :label="t('user.tenant')" min-width="120">
        <template #default="{ row }">{{ row.tenantName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" :label="t('common.status')" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'enabled' ? 'success' : 'danger'">
            {{ row.status === 'enabled' ? t('common.enabled') : t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.operate')" width="330" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAssign()">{{
            t('user.assignment')
          }}</el-button>
          <el-button link type="warning" @click="onResetPassword(row as unknown as User)">{{
            t('user.resetPassword')
          }}</el-button>
          <el-button link type="danger" @click="onKick(row as unknown as User)">{{
            t('user.kick')
          }}</el-button>
        </template>
      </el-table-column>
    </ProTable>

    <!-- 新建用户 -->
    <el-dialog v-model="dialogVisible" :title="t('common.add')" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="t('user.username')" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item :label="t('user.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('user.email')" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item :label="t('user.mobile')" prop="mobile">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item :label="t('user.role')" prop="roleIds">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('user.resetPassword')" prop="initialPassword">
          <el-input v-model="form.initialPassword" type="password" />
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
import { userApi } from '@/api/identity/user'
import { roleApi } from '@/api/identity/role'
import type { User, Role } from '@/api/identity/types'

const { t } = useI18n()
const list = ref<User[]>([])
const total = ref(0)
const loading = ref(false)
const roles = ref<Role[]>([])
const query = reactive({ page: 1, size: 10 })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  name: '',
  email: '',
  mobile: '',
  roleIds: [] as string[],
  initialPassword: ''
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请选择角色', trigger: 'change' }],
  initialPassword: [{ required: true, min: 6, message: '至少6位', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await userApi.list(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  roles.value = await roleApi.list()
}

function onQuery(q: { page: number; size: number }) {
  query.page = q.page
  query.size = q.size
  load()
}

function openCreate() {
  Object.assign(form, {
    username: '',
    name: '',
    email: '',
    mobile: '',
    roleIds: [],
    initialPassword: ''
  })
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await userApi.create({ ...form })
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onResetPassword(row: User) {
  await ElMessageBox.confirm(
    `重置「${row.username}」的密码为 admin123？`,
    t('user.resetPassword'),
    { type: 'warning' }
  )
  await userApi.resetPassword(row.id, 'admin123')
  ElMessage.success(t('common.success'))
}

async function onKick(row: User) {
  await ElMessageBox.confirm(`确定将「${row.username}」踢下线？`, t('user.kick'), {
    type: 'warning'
  })
  await userApi.kick(row.id)
  ElMessage.success(t('common.success'))
}

function openAssign() {
  ElMessage.info('分配角色请使用角色管理页（骨架阶段占位）')
}

onMounted(() => {
  load()
  loadRoles()
})
</script>
