<template>
  <div class="auth-page">
    <section class="auth-brand">
      <div class="auth-photo"></div>
      <div class="auth-brand-copy">
        <img class="auth-mark" src="/favicon.svg" alt="" aria-hidden="true" />
        <h1>寻宠互助平台</h1>
        <p>登录后可发布寻宠启事、跟进线索消息，并管理动物档案。</p>
      </div>
    </section>
    <el-card class="auth-card">
      <div style="text-align: left; margin-bottom: 8px;">
        <router-link to="/" style="color: #909399; font-size: 13px;">&larr; 返回首页</router-link>
      </div>
      <h2 class="auth-title">登录</h2>
      <el-tabs v-model="loginMode" stretch class="login-tabs">
        <el-tab-pane label="密码登录" name="password">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="0" size="large">
            <el-form-item prop="email">
              <el-input v-model.trim="passwordForm.email" placeholder="邮箱" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="passwordForm.password" type="password" placeholder="密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%" :loading="passwordLoading" @click="handlePasswordLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="验证码登录" name="code">
          <el-form ref="codeFormRef" :model="codeForm" :rules="codeRules" label-width="0" size="large">
            <el-form-item prop="email">
              <el-input v-model.trim="codeForm.email" placeholder="邮箱" />
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-row">
                <el-input v-model.trim="codeForm.code" placeholder="验证码" style="flex: 1;" />
                <el-button :disabled="codeSending || codeCountdown > 0" @click="handleSendLoginCode">
                  {{ codeCountdown > 0 ? codeCountdown + 's' : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%" :loading="codeLoading" @click="handleCodeLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="auth-links">
        <router-link to="/auth/register">没有账号？去注册</router-link>
        <a style="margin-left: 12px; cursor: pointer; color: #909399; font-size: 13px;" @click="showResetDialog = true">忘记密码</a>
      </div>

      <!-- 忘记密码弹窗 -->
      <el-dialog v-model="showResetDialog" title="重置密码" width="420px" destroy-on-close>
        <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="0" size="large">
          <el-form-item prop="email">
            <el-input v-model="resetForm.email" placeholder="邮箱" />
          </el-form-item>
          <el-form-item prop="code">
            <div style="display: flex; gap: 8px; width: 100%;">
              <el-input v-model="resetForm.code" placeholder="验证码" style="flex: 1;" />
              <el-button :disabled="resetCodeSending || resetCountdown > 0" @click="handleResetSendCode">
                {{ resetCountdown > 0 ? resetCountdown + 's' : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item prop="newPassword">
            <el-input v-model="resetForm.newPassword" type="password" placeholder="新密码（6-16位，字母/数字）" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showResetDialog = false">取消</el-button>
          <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">确认重置</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const loginMode = ref('password')
const passwordFormRef = ref(null)
const codeFormRef = ref(null)
const passwordForm = reactive({ email: '', password: '' })
const codeForm = reactive({ email: '', code: '' })
const passwordLoading = ref(false)
const codeLoading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let codeCountdownTimer = null

const emailRules = [
  { required: true, message: '请输入邮箱', trigger: 'blur' },
  { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
]

const passwordRules = {
  email: emailRules,
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const codeRules = {
  email: emailRules,
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 忘记密码
const showResetDialog = ref(false)
const resetFormRef = ref(null)
const resetLoading = ref(false)
const resetCodeSending = ref(false)
const resetCountdown = ref(0)
let resetCountdownTimer = null
const resetForm = reactive({ email: '', code: '', newPassword: '' })
const resetRules = {
  email: emailRules,
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码为6-16个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '密码只能包含字母和数字', trigger: 'blur' }
  ]
}

function isValidEmail(email) {
  return /^\S+@\S+\.\S+$/.test(email)
}

async function redirectAfterLogin() {
  await userStore.fetchProfile()
  if (userStore.isAdmin) {
    router.push('/admin')
  } else {
    router.push('/')
  }
}

async function handlePasswordLogin() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    await userStore.login(passwordForm)
    await redirectAfterLogin()
  } catch {
    // request.js 拦截器已弹出错误提示
  } finally {
    passwordLoading.value = false
  }
}

function startCodeCountdown() {
  codeCountdown.value = 60
  if (codeCountdownTimer) clearInterval(codeCountdownTimer)
  codeCountdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(codeCountdownTimer)
      codeCountdownTimer = null
    }
  }, 1000)
}

async function handleSendLoginCode() {
  if (!codeForm.email || !isValidEmail(codeForm.email)) {
    ElMessage.warning('请先输入正确的邮箱')
    return
  }
  codeSending.value = true
  try {
    await request.post('/verify/send-login-code', { email: codeForm.email })
    ElMessage.success('验证码已发送')
    startCodeCountdown()
  } catch {
    // request.js 拦截器已弹出错误提示
  } finally {
    codeSending.value = false
  }
}

async function handleCodeLogin() {
  const valid = await codeFormRef.value.validate().catch(() => false)
  if (!valid) return
  codeLoading.value = true
  try {
    await userStore.loginByCode(codeForm)
    await redirectAfterLogin()
  } catch {
    // request.js 拦截器已弹出错误提示
  } finally {
    codeLoading.value = false
  }
}

function startResetCountdown() {
  resetCountdown.value = 60
  if (resetCountdownTimer) clearInterval(resetCountdownTimer)
  resetCountdownTimer = setInterval(() => {
    resetCountdown.value--
    if (resetCountdown.value <= 0) {
      clearInterval(resetCountdownTimer)
      resetCountdownTimer = null
    }
  }, 1000)
}

async function handleResetSendCode() {
  if (!resetForm.email || !isValidEmail(resetForm.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  resetCodeSending.value = true
  try {
    const res = await request.post('/verify/send-code', { email: resetForm.email })
    if (res.code === 200) {
      ElMessage.success('验证码已发送')
      startResetCountdown()
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch {
    ElMessage.error('发送失败')
  }
  resetCodeSending.value = false
}

async function handleResetPassword() {
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return
  resetLoading.value = true
  try {
    await request.post('/user/reset-password', {
      email: resetForm.email,
      code: resetForm.code,
      newPassword: resetForm.newPassword,
    })
    ElMessage.success('密码已重置，请登录')
    showResetDialog.value = false
    resetForm.email = ''
    resetForm.code = ''
    resetForm.newPassword = ''
  } catch (e) {
    ElMessage.error(e.message || '重置失败')
  }
  resetLoading.value = false
}

onBeforeUnmount(() => {
  if (codeCountdownTimer) clearInterval(codeCountdownTimer)
  if (resetCountdownTimer) clearInterval(resetCountdownTimer)
})
</script>

<style scoped>
.auth-page { display: grid; grid-template-columns: minmax(280px, 390px) 400px; gap: 36px; justify-content: center; align-items: center; min-height: 100vh; padding: 32px; background: #f5f7fa; }
.auth-brand { color: var(--color-text); border-radius: 12px; overflow: hidden; background: #fff; border: 1px solid var(--color-line); box-shadow: var(--shadow-soft); }
.auth-photo { min-height: 240px; background: url('/images/archive-care.jpg') center/cover no-repeat; }
.auth-brand-copy { padding: 22px; }
.auth-mark { width: 54px; height: 54px; display: block; border-radius: 14px; margin-bottom: 18px; box-shadow: 0 10px 22px rgba(47, 141, 243, 0.18); }
.auth-brand h1 { font-size: 32px; margin-bottom: 12px; }
.auth-brand p { color: var(--color-muted); line-height: 1.8; font-size: 15px; }
.auth-card { width: 400px; }
.auth-title { text-align: center; margin-bottom: 24px; font-size: 24px; }
.login-tabs { margin-bottom: 4px; }
.code-row { display: flex; gap: 8px; width: 100%; }
.auth-links { text-align: center; margin-top: 16px; }
.auth-links a { color: #409eff; font-size: 14px; }

@media (max-width: 768px) {
  .auth-page { grid-template-columns: 1fr; gap: 18px; align-items: start; min-height: 100vh; padding: 28px 16px; }
  .auth-brand { text-align: center; border: 0; box-shadow: none; background: transparent; }
  .auth-photo { display: none; }
  .auth-brand-copy { padding: 0; }
  .auth-mark { margin: 0 auto 12px; width: 44px; height: 44px; border-radius: 12px; }
  .auth-brand h1 { font-size: 24px; margin-bottom: 8px; }
  .auth-brand p { font-size: 13px; }
  .auth-card { width: 100%; }
  .code-row { display: grid; grid-template-columns: 1fr; }
}
</style>
