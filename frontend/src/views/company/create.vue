<template>
  <div class="company-create">
    <div class="company-create__card">
      <div class="company-create__brand">
        <el-icon :size="40" color="var(--el-color-primary)"><OfficeBuilding /></el-icon>
        <h2 class="company-create__title">创建公司</h2>
        <p class="company-create__slogan">
          当前身份：<el-tag type="warning" size="small">{{ userStore.profile?.username }}</el-tag>
          （老板）。创建公司后即可组建团队。
        </p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        label-position="top"
        @keyup.enter="onSubmit"
      >
        <el-form-item prop="name" label="公司名称">
          <el-input v-model="form.name" placeholder="请输入公司名称" maxlength="50">
            <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="company-create__submit"
          :loading="loading"
          @click="onSubmit"
        >
          创建公司
        </el-button>
      </el-form>

      <el-alert
        class="company-create__tip"
        type="info"
        :closable="false"
        title="创建成功后需要重新登录，新的登录令牌才会带上公司身份"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { companyApi } from '@/api/identity/company'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  name: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { min: 2, max: 50, message: '公司名称长度为 2-50 个字符', trigger: 'blur' }
  ]
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await companyApi.create(form.name)
    ElMessage.success(`公司「${res.name}」创建成功，请重新登录`)
    // 旧 token 里没有租户上下文，必须重新登录
    userStore.logout()
    router.push('/login')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.company-create {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    var(--el-color-primary-light-9),
    var(--el-fill-color-lighter)
  );

  &__card {
    width: 420px;
    padding: 40px;
    border-radius: 12px;
    background: var(--el-bg-color);
    box-shadow: var(--el-box-shadow-light);
  }

  &__brand {
    text-align: center;
    margin-bottom: 28px;
  }

  &__title {
    margin: 8px 0 4px;
    font-size: 20px;
  }

  &__slogan {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__submit {
    width: 100%;
  }

  &__tip {
    margin-top: 16px;
  }
}
</style>
