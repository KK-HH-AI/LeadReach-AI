<template>
  <PageContainer :title="t('menu.role')">
    <template #actions>
      <PermissionButton
        permission="system:role:create"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        {{ t('common.add') }}
      </PermissionButton>
    </template>

    <ProTable :data="list" :loading="loading" :show-pagination="false">
      <el-table-column prop="name" :label="t('role.name')" width="160" />
      <el-table-column prop="code" :label="t('role.code')" width="180" />
      <el-table-column prop="description" :label="t('role.description')" min-width="200" />
      <el-table-column prop="userCount" :label="t('role.userCount')" width="90" align="center" />
      <el-table-column :label="t('common.operate')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as unknown as Role)">{{
            t('common.edit')
          }}</el-button>
          <el-button link type="primary" @click="openAssign(row as unknown as Role)">{{
            t('role.assignPermission')
          }}</el-button>
          <el-button link type="danger" @click="onDelete(row as unknown as Role)">{{
            t('common.delete')
          }}</el-button>
        </template>
      </el-table-column>
    </ProTable>

    <!-- 新建/编辑角色 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('common.edit') : t('common.add')"
      width="480px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="t('role.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('role.code')" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('role.description')" prop="description">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{
          t('common.save')
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="permVisible" :title="t('role.assignPermission')" width="520px">
      <el-tree
        ref="treeRef"
        :data="permTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="permVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSavePermissions">{{ t('common.save') }}</el-button>
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
import { roleApi, permissionApi } from '@/api/identity/role'
import type { Role, PermissionNode } from '@/api/identity/types'

const { t } = useI18n()
const list = ref<Role[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref('')
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ name: '', code: '', description: '' })
const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入标识', trigger: 'blur' }]
}

const permVisible = ref(false)
const permTree = ref<PermissionNode[]>([])
const treeRef = ref()

async function load() {
  loading.value = true
  try {
    list.value = await roleApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { name: '', code: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row: Role) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, { name: row.name, code: row.code, description: row.description })
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await roleApi.update(editingId.value, { name: form.name, description: form.description })
    } else {
      await roleApi.create({ ...form })
    }
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row: Role) {
  await ElMessageBox.confirm(`删除角色「${row.name}」？`, t('common.delete'), { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success(t('common.success'))
  load()
}

async function openAssign(row: Role) {
  permTree.value = await permissionApi.tree()
  editingId.value = row.id
  permVisible.value = true
  // 骨架阶段不预选；真实分配由后端返回角色已授权权限后 setCheckedKeys
  treeRef.value?.setCheckedKeys([])
}

async function onSavePermissions() {
  const checked = treeRef.value?.getCheckedKeys(false) ?? []
  await roleApi.assignPermissions(editingId.value, checked)
  ElMessage.success(t('common.success'))
  permVisible.value = false
}

onMounted(load)
</script>
