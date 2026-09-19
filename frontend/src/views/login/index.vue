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
        <el-form-item prop="code">
          <div class="login__code-row">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              maxlength="6"
              class="login__code-input"
            >
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
            <el-button
              class="login__code-btn"
              :disabled="countdown > 0 || sending"
              @click="onSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : (sending ? '发送中…' : '获取验证码') }}
            </el-button>
          </div>
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
        <span>登录流程：输入手机号 → 获取验证码（登录框有提示）→ 输入验证码登录</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/identity/auth'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  phone: '',
  code: ''
})

/** 手机号格式校验 */
const phonePattern = /^1[3-9]\d{9}$/

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: phonePattern, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ]
}

/** 发送验证码：校验手机号 → 调后端 send-code → 启动 60s 倒计时 */
async function onSendCode() {
  if (!phonePattern.test(form.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  sending.value = true
  try {
    await authApi.sendCode(form.phone)
    ElMessage.success('验证码已发送，请查看后端控制台打印的模拟短信')
    countdown.value = 60
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e) {
    // 错误已由拦截器提示
    console.error(e)
  } finally {
    sending.value = false
  }
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({
      phone: form.phone,
      code: form.code
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

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
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

  &__code-row {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  &__code-input {
    flex: 1;
  }

  &__code-btn {
    width: 130px;
    flex-shrink: 0;
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