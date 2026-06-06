<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div style="text-align: left; margin-bottom: 8px;">
        <router-link to="/" style="color: #909399; font-size: 13px;">&larr; 返回首页</router-link>
      </div>
      <h2 class="auth-title">注册</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" size="large">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item prop="code">
          <div style="display: flex; gap: 8px; width: 100%;">
            <el-input v-model="form.code" placeholder="验证码" style="flex: 1;" />
            <el-button :disabled="codeSending || countdown > 0" @click="handleSendCode">
              {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（3-12位，字母/汉字/数字）" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（6-16位，字母/数字）" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="handleRegister">注册</el-button>
        </el-form-item>
      </el-form>
      <div class="auth-links">
        <router-link to="/auth/login">已有账号？去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer = null
const form = reactive({ email: '', code: '', nickname: '', password: '', confirmPassword: '' })

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 3, max: 12, message: '昵称为3-12个字符', trigger: 'blur' },
    { pattern: /^[一-龥a-zA-Z0-9]+$/, message: '昵称只能包含字母、汉字和数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码为6-16个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '密码只能包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleSendCode() {
  if (!form.email || !/^\S+@\S+\.\S+$/.test(form.email)) {
    ElMessage.warning('请先输入正确的邮箱')
    return
  }
  codeSending.value = true
  try {
    await request.post('/verify/send-code', { email: form.email })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // request.js 拦截器已弹出错误提示，此处不再重复
  }
  codeSending.value = false
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await userApi.register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/auth/login')
  } catch {
    ElMessage.error('注册失败，请重试')
  }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f7fa; }
.auth-card { width: 400px; }
.auth-title { text-align: center; margin-bottom: 24px; font-size: 24px; }
.auth-links { text-align: center; margin-top: 16px; }
.auth-links a { color: #409eff; font-size: 14px; }
</style>
