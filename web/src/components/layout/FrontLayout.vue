<template>
  <div class="front-layout">
    <header class="site-header">
      <div class="header-inner">
        <router-link to="/" class="logo">🐾 寻宠互助平台</router-link>
        <nav class="nav-links">
          <router-link to="/posts">寻宠大厅</router-link>
          <router-link to="/archives">流浪动物档案</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/posts/create">发布寻宠</router-link>
          <router-link v-if="userStore.isCertified" to="/archives/create">登记档案</router-link>
        </nav>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-badge :value="userStore.unreadCount" :hidden="userStore.unreadCount === 0" class="msg-badge">
              <router-link to="/messages">消息</router-link>
            </el-badge>
            <div class="user-block" @click="$router.push('/profile')" style="cursor: pointer">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" class="user-avatar">
                {{ displayName.charAt(0) }}
              </el-avatar>
              <el-tooltip :content="roleLabel" placement="bottom">
                <span class="user-name">{{ displayName }}</span>
              </el-tooltip>
            </div>
            <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
          </template>
          <template v-else>
            <router-link to="/auth/login">登录</router-link>
            <router-link to="/auth/register" class="register-btn">注册</router-link>
          </template>
        </div>
      </div>
    </header>
    <main class="site-main">
      <router-view />
    </main>
    <footer class="site-footer">
      <p>© 2026 流浪动物身份登记与寻宠互助平台</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { onMessage } from '@/utils/websocket'

const userStore = useUserStore()
const router = useRouter()

const displayName = computed(() => {
  const info = userStore.userInfo
  if (info?.nickname) return info.nickname
  if (info?.email) return info.email.split('@')[0]
  return '用户'
})

const roleLabel = computed(() => {
  const map = { USER: '普通用户', PENDING_CERT: '认证审核中', CERTIFIED: '认证用户', ADMIN: '管理员' }
  return '权限：' + (map[userStore.userInfo?.role] || '未知')
})

function handleLogout() {
  userStore.logout()
  router.push('/auth/login')
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUnread()
    onMessage(() => userStore.fetchUnread())
  }
})
</script>

<style scoped>
.front-layout { min-height: 100vh; display: flex; flex-direction: column; }
.site-header { background: #fff; border-bottom: 1px solid #e4e7ed; position: sticky; top: 0; z-index: 100; }
.header-inner { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; height: 60px; padding: 0 20px; }
.logo { font-size: 20px; font-weight: bold; color: #409eff; margin-right: 40px; }
.nav-links { flex: 1; display: flex; gap: 20px; }
.nav-links a { color: #606266; font-size: 14px; }
.nav-links a:hover, .nav-links a.router-link-active { color: #409eff; }
.header-right { display: flex; align-items: center; gap: 16px; }
.msg-badge { margin-right: 8px; }
.user-block { display: flex; align-items: center; gap: 8px; }
.user-avatar { background: #409eff; color: #fff; font-size: 14px; flex-shrink: 0; }
.user-name { color: #303133; font-size: 14px; font-weight: 500; }
.register-btn { background: #409eff; color: #fff !important; padding: 6px 16px; border-radius: 4px; }
.site-main { flex: 1; max-width: 1200px; margin: 0 auto; padding: 20px; width: 100%; }
.site-footer { text-align: center; padding: 20px; color: #909399; font-size: 13px; border-top: 1px solid #e4e7ed; }
</style>
