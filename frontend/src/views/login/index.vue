<template>
  <div class="login">
    <div class="login__card">
      <div class="login__brand">
        <el-icon :size="36" color="var(--el-color-primary)"><Ship /></el-icon>
        <h2 class="login__title">{{ t('app.name') }}</h2>
        <p class="login__slogan">{{ t('app.slogan') }}</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="tenantCode">
          <el-input v-model="form.tenantCode" :placeholder="t('login.tenantCodePlaceholder')">
            <template #prefix
              ><el-icon><OfficeBuilding /></el-icon
            ></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="t('login.usernamePlaceholder')">
            <template #prefix
              ><el-icon><User /></el-icon
            ></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="t('login.passwordPlaceholder')"
          />
        </el-form-item>
        <div class="login__options">
          <el-checkbox v-model="form.remember">{{ t('login.remember') }}</el-checkbox>
          <el-link type="primary" :underline="false">{{ t('login.forget') }}</el-link>
        </div>
        <el-button
          type="primary"
          size="large"
          class="login__submit"
          :loading="loading"
          @click="onSubmit"
        >
          {{ t('login.button') }}
        </el-button>
      </el-form>

      <div class="login__hint">
        <span>演示账号：admin / admin123（超管），demo / admin123（租户管理员）</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  tenantCode: '',
  username: 'admin',
  password: 'admin123',
  remember: true
})

const rules: FormRules = {
  tenantCode: [{ required: false }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({
      tenantCode: form.tenantCode || undefined,
      username: form.username,
      password: form.password
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 错误已由拦截器提示
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    var(--el-color-primary-light-9),
    var(--el-fill-color-lighter)
  );

  &__card {
    width: 400px;
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
    font-size: 22px;
  }

  &__slogan {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  &__options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  &__submit {
    width: 100%;
  }

  &__hint {
    margin-top: 20px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    text-align: center;
  }
}
</style>
