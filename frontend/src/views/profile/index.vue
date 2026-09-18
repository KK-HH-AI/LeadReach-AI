<template>
  <PageContainer :title="t('menu.profile')">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" :header="t('profile.basic')">
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="t('user.name')">{{
              userStore.profile?.name
            }}</el-descriptions-item>
            <el-descriptions-item :label="t('user.username')">{{
              userStore.profile?.username
            }}</el-descriptions-item>
            <el-descriptions-item :label="t('user.email')">{{
              userStore.profile?.email
            }}</el-descriptions-item>
            <el-descriptions-item :label="t('user.mobile')">{{
              userStore.profile?.mobile || '-'
            }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" :header="t('profile.changePassword')">
          <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-width="100px">
            <el-form-item :label="t('profile.oldPassword')" prop="oldPassword">
              <el-input v-model="pwd.oldPassword" type="password" />
            </el-form-item>
            <el-form-item :label="t('profile.newPassword')" prop="newPassword">
              <el-input v-model="pwd.newPassword" type="password" />
            </el-form-item>
            <el-form-item :label="t('profile.confirmPassword')" prop="confirm">
              <el-input v-model="pwd.confirm" type="password" />
            </el-form-item>
            <el-button type="primary" @click="onChangePwd">{{ t('common.save') }}</el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px" :header="t('profile.myRoles')">
      <div class="profile__roles">
        <el-tag v-for="r in userStore.roleCodes" :key="r">{{ r }}</el-tag>
      </div>
      <el-descriptions :column="1" border title="">
        <el-descriptions-item :label="t('user.role')">
          <el-tag v-for="r in userStore.roleCodes" :key="r" style="margin-right: 6px">{{
            r
          }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()

const pwdRef = ref<FormInstance>()
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '至少6位', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_r, value, cb) =>
        value === pwd.newPassword ? cb() : cb(new Error('两次输入不一致')),
      trigger: 'blur'
    }
  ]
}

async function onChangePwd() {
  const valid = await pwdRef.value?.validate().catch(() => false)
  if (!valid) return
  ElMessage.success('（骨架阶段）修改成功')
  Object.assign(pwd, { oldPassword: '', newPassword: '', confirm: '' })
}
</script>

<style scoped lang="scss">
.profile__roles {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
