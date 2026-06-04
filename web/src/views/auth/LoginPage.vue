<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div style="text-align: left; margin-bottom: 8px;">
        <router-link to="/" style="color: #909399; font-size: 13px;">&larr; 返回首页</router-link>
      </div>
      <h2 class="auth-title">登录</h2>
      <el-form :model="form" label-width="0" size="large">
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="auth-links">
        <router-link to="/auth/register">没有账号？去注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ email: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(form)
    await userStore.fetchProfile()
    if (userStore.isAdmin) {
      router.push('/admin')
    } else {
      router.push('/')
    }
  } catch (e) {
    ElMessage.error(e?.message || '登录失败，请检查邮箱和密码')
  } finally {
    loading.value = false
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
