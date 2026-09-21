<template>
  <div class="register">
    <div class="register__card">
      <div class="register__brand">
        <el-icon :size="36" color="var(--el-color-primary)"><Ship /></el-icon>
        <h2 class="register__title">{{ t('app.name') }} · 注册</h2>
        <p class="register__slogan">选择身份完成注册，老板可创建公司与团队</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11">
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code">
          <div class="register__code-row">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              maxlength="6"
              class="register__code-input"
            >
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
            <el-button
              class="register__code-btn"
              :disabled="countdown > 0 || sending"
              @click="onSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : (sending ? '发送中…' : '获取验证码') }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="设置登录密码（6-20 位）"
            maxlength="20"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            maxlength="20"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="role">
          <el-radio-group v-model="form.role" class="register__roles">
            <el-radio-button value="BOSS">老板</el-radio-button>
            <el-radio-button value="EMPLOYEE">员工</el-radio-button>
          </el-radio-group>
          <div class="register__role-tip">
            {{ form.role === 'BOSS' ? '老板注册后创建公司，拉员工组建团队' : '员工注册后等待老板拉入团队' }}
          </div>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="register__submit"
          :loading="loading"
          @click="onSubmit"
        >
          注册
        </el-button>
      </el-form>

      <div class="register__footer">
        <span>已有账号？</span>
        <el-link type="primary" @click="goLogin">去登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/identity/auth'

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
  code: '',
  password: '',
  confirmPassword: '',
  role: 'BOSS' as 'BOSS' | 'EMPLOYEE'
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
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  role: [{ required: true, message: '请选择身份角色', trigger: 'change' }]
}

/** 发送验证码 */
async function onSendCode() {
  if (!phonePattern.test(form.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  sending.value = true
  try {
    const res = await authApi.sendCode(form.phone)
    // 模拟阶段：后端直接返回验证码，展示到界面方便联调
    ElMessage.success(`验证码已发送：${res.smsCode}`)
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
    console.error(e)
  } finally {
    sending.value = false
  }
}

/** 提交注册 */
async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.register({
      phone: form.phone,
      code: form.code,
      password: form.password,
      role: form.role
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.register {
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
    font-size: 20px;
  }

  &__slogan {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
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

  &__roles {
    width: 100%;
  }

  &__role-tip {
    width: 100%;
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }

  &__submit {
    width: 100%;
  }

  &__footer {
    margin-top: 16px;
    text-align: center;
    font-size: 13px;
    color: var(--el-text-color-secondary);

    .el-link {
      font-size: 13px;
    }
  }
}
</style>
