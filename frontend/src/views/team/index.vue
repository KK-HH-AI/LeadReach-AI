<template>
  <PageContainer title="团队管理">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="onCreateTeam">确认创建团队</el-button>
    </template>

    <el-row :gutter="16">
      <!-- 拉人表单 -->
      <el-col :span="8">
        <el-card shadow="never" class="team__panel">
          <template #header><b>拉员工入队</b></template>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="员工手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="员工需先完成注册" maxlength="11">
                <template #prefix><el-icon><Iphone /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="岗位" prop="position">
              <el-select v-model="form.position" placeholder="选择岗位" class="team__select">
                <el-option
                  v-for="p in POSITION_OPTIONS"
                  :key="p.value"
                  :label="p.label"
                  :value="p.value"
                />
              </el-select>
            </el-form-item>
            <el-button type="primary" class="team__add-btn" :loading="adding" @click="onAddMember">
              拉入团队
            </el-button>
          </el-form>
          <el-alert type="info" :closable="false" class="team__hint"
            title="员工必须先注册账号，老板才能拉人" />
        </el-card>
      </el-col>

      <!-- 成员列表 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <b>团队成员（{{ members.length }}）</b>
          </template>
          <el-table :data="members" v-loading="loading" empty-text="暂无成员，请通过左侧表单拉人">
            <el-table-column label="手机号" prop="phone" min-width="140" />
            <el-table-column label="岗位" min-width="120">
              <template #default="{ row }">
                <el-tag :type="positionTag(row.position)">{{ positionLabel(row.position) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" min-width="160">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAssign(row as TeamMember)">分配内容</el-button>
                <el-button link type="danger" @click="onRemoveMember(row as TeamMember)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分配内容弹窗 -->
    <el-dialog v-model="assignVisible" title="分配负责的内容" width="420px">
      <el-form label-position="top">
        <el-form-item label="内容类型（预留字段）">
          <el-input v-model="assignForm.contentType" placeholder="如 LEAD / CONTENT" />
        </el-form-item>
        <el-form-item label="内容 ID">
          <el-input v-model="assignForm.contentId" placeholder="如 100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">保存分配</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { teamApi, POSITION_OPTIONS, type TeamMember } from '@/api/identity/team'

const members = ref<TeamMember[]>([])
const loading = ref(false)
const adding = ref(false)
const assigning = ref(false)

const formRef = ref<FormInstance>()
const form = reactive({
  phone: '',
  position: ''
})

const phonePattern = /^1[3-9]\d{9}$/

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入员工手机号', trigger: 'blur' },
    { pattern: phonePattern, message: '手机号格式不正确', trigger: 'blur' }
  ],
  position: [{ required: true, message: '请选择岗位', trigger: 'change' }]
}

const assignVisible = ref(false)
const assignForm = reactive({
  employeeId: 0,
  phone: '',
  contentType: '',
  contentId: ''
})

/** 加载团队成员 */
async function loadMembers() {
  loading.value = true
  try {
    members.value = await teamApi.list()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

/** 创建/确认团队（幂等） */
async function onCreateTeam() {
  try {
    const res = await teamApi.create()
    ElMessage.success(`团队可用，当前成员 ${res.memberCount} 人`)
  } catch (e) {
    console.error(e)
  }
}

/** 拉人 */
async function onAddMember() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  adding.value = true
  try {
    await teamApi.addMember({ phone: form.phone, position: form.position })
    ElMessage.success('拉人成功')
    form.phone = ''
    form.position = ''
    formRef.value?.clearValidate()
    await loadMembers()
  } catch (e) {
    console.error(e)
  } finally {
    adding.value = false
  }
}

/** 删除成员 */
async function onRemoveMember(row: TeamMember) {
  try {
    await ElMessageBox.confirm(`确认将 ${row.phone} 移出团队？`, '删除成员', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await teamApi.removeMember(row.employeeId)
    ElMessage.success('删除成功')
    await loadMembers()
  } catch (e) {
    console.error(e)
  }
}

/** 打开分配弹窗 */
function openAssign(row: TeamMember) {
  assignForm.employeeId = row.employeeId
  assignForm.phone = row.phone
  assignForm.contentType = ''
  assignForm.contentId = ''
  assignVisible.value = true
}

/** 保存分配/修改 */
async function onAssign() {
  if (!assignForm.contentType.trim() || !assignForm.contentId.trim()) {
    ElMessage.warning('请填写内容类型和内容 ID')
    return
  }
  assigning.value = true
  try {
    await teamApi.assign(assignForm.employeeId, {
      contentType: assignForm.contentType.trim(),
      contentId: assignForm.contentId.trim()
    })
    ElMessage.success('分配成功')
    assignVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    assigning.value = false
  }
}

function positionLabel(position: string) {
  return POSITION_OPTIONS.find((p) => p.value === position)?.label ?? position
}

function positionTag(position: string) {
  if (position === 'SALES_MANAGER') return 'warning' as const
  if (position === 'OPERATOR') return 'success' as const
  return 'primary' as const
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

onMounted(loadMembers)
</script>

<style scoped lang="scss">
.team {
  &__panel {
    margin-bottom: 0;
  }

  &__select {
    width: 100%;
  }

  &__add-btn {
    width: 100%;
  }

  &__hint {
    margin-top: 12px;
  }
}
</style>
