<template>
  <div class="login">
    <div class="login__card">
      <div class="login__brand">
        <el-icon :size="36" color="var(--el-color-primary)"><Ship /></el-icon>
        <h2 class="login__title">{{ t('app.name') }}</h2>
        <p class="login__slogan">{{ t('app.slogan') }}</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11">
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            maxlength="20"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login__submit"
          :loading="loading"
          @click="onSubmit"
        >
          登录
        </el-button>
      </el-form>

      <div class="login__hint">
        <span>未注册的用户请先完成注册</span>
      </div>
      <div class="login__footer">
        <span>还没有账号？</span>
        <el-link type="primary" @click="goRegister">立即注册</el-link>
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
  phone: '',
  password: ''
})

/** 手机号格式校验 */
const phonePattern = /^1[3-9]\d{9}$/

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: phonePattern, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ]
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({
      phone: form.phone,
      password: form.password
    })
    ElMessage.success('登录成功')
    // 老板尚未创建公司 → 引导创建公司
    if (userStore.isBoss && !userStore.hasCompany) {
      router.push('/company/create')
      return
    }
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 错误已由拦截器提示
    console.error(e)
  } finally {
    loading.value = false
  }
}

/** 跳转注册页 */
function goRegister() {
  router.push('/register')
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

  &__submit {
    width: 100%;
  }

  &__hint {
    margin-top: 20px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    text-align: center;
  }

  &__footer {
    margin-top: 12px;
    text-align: center;
    font-size: 13px;
    color: var(--el-text-color-secondary);

    .el-link {
      font-size: 13px;
    }
  }
}
</style>
