<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div style="text-align: left; margin-bottom: 8px;">
        <router-link to="/" style="color: #909399; font-size: 13px;">&larr; 返回首页</router-link>
      </div>
      <h2 class="auth-title">注册</h2>
      <el-form :model="form" label-width="0" size="large">
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.nickname" placeholder="昵称" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
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
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'

const router = useRouter()
const form = reactive({ email: '', nickname: '', password: '' })

async function handleRegister() {
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
