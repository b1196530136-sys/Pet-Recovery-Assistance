<template>
  <div class="front-layout">
    <header class="site-header">
      <div class="header-inner">
        <router-link to="/" class="logo" @click="menuOpen = false">
          <img class="logo-mark" src="/favicon.svg" alt="" aria-hidden="true" />
          <span>寻宠互助平台</span>
        </router-link>
        <nav class="nav-links desktop-nav">
          <router-link to="/posts">寻宠大厅</router-link>
          <router-link to="/archives">流浪动物档案</router-link>
        </nav>
        <div class="header-right desktop-actions">
          <template v-if="userStore.isLoggedIn">
            <el-badge :value="userStore.unreadCount" :hidden="userStore.unreadCount === 0" :max="99" class="msg-badge">
              <router-link to="/messages">消息</router-link>
            </el-badge>
            <div class="user-block" @click="goProfile">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar || '/images/default-avatar.png'" class="user-avatar" />
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
        <el-button class="mobile-menu-btn" text :icon="menuOpen ? Close : Menu" @click="menuOpen = !menuOpen" />
      </div>
      <div v-if="menuOpen" class="mobile-panel">
        <router-link to="/posts" @click="menuOpen = false">寻宠大厅</router-link>
        <router-link to="/archives" @click="menuOpen = false">流浪动物档案</router-link>
        <template v-if="userStore.isLoggedIn">
          <el-badge :value="userStore.unreadCount" :hidden="userStore.unreadCount === 0" :max="99" class="mobile-msg-badge">
            <router-link to="/messages" @click="menuOpen = false">消息</router-link>
          </el-badge>
          <button class="mobile-user-link" type="button" @click="goProfile">{{ displayName }}</button>
          <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
        </template>
        <template v-else>
          <router-link to="/auth/login" @click="menuOpen = false">登录</router-link>
          <router-link to="/auth/register" class="register-btn" @click="menuOpen = false">注册</router-link>
        </template>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import { Close, Menu } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { onMessage } from '@/utils/websocket'

const userStore = useUserStore()
const router = useRouter()
const menuOpen = ref(false)
let wsUnsubscribe = null

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
  menuOpen.value = false
  router.push('/auth/login')
}

function goProfile() {
  menuOpen.value = false
  router.push('/profile')
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUnread()
    wsUnsubscribe = onMessage((msg) => {
      userStore.fetchUnread()
      if (msg.msgType === 2 && msg.receiverId === userStore.userInfo?.id) {
        const payload = parseSystemPayload(msg.content)
        ElNotification({
          title: payload.title || '系统消息',
          message: payload.body || '你有一条新的系统消息',
          type: payload.resultType === 'REJECTED' ? 'warning' : 'success',
          duration: 5000,
        })
      }
    })
  }
})

onUnmounted(() => {
  if (wsUnsubscribe) wsUnsubscribe()
})

function parseSystemPayload(content) {
  try {
    return JSON.parse(content || '{}')
  } catch {
    return { title: '系统消息', body: content || '你有一条新的系统消息' }
  }
}
</script>

<style scoped>
.front-layout { min-height: 100vh; display: flex; flex-direction: column; }
.site-header { background: rgba(255,255,255,0.96); border-bottom: 1px solid #e4e7ed; position: sticky; top: 0; z-index: 100; backdrop-filter: blur(8px); }
.header-inner { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; height: 64px; padding: 0 20px; }
.logo { display: inline-flex; align-items: center; gap: 10px; font-size: 20px; font-weight: bold; color: var(--color-primary); margin-right: 40px; white-space: nowrap; }
.logo-mark { width: 34px; height: 34px; display: block; border-radius: 10px; box-shadow: 0 6px 14px rgba(47, 141, 243, 0.18); flex-shrink: 0; }
.nav-links { flex: 1; display: flex; gap: 20px; }
.nav-links a { color: #606266; font-size: 14px; font-weight: 500; }
.nav-links a:hover, .nav-links a.router-link-active { color: var(--color-primary); }
.header-right { display: flex; align-items: center; gap: 16px; }
.msg-badge { margin-right: 8px; }
.mobile-msg-badge { width: 100%; }
.mobile-msg-badge :deep(.el-badge__content) { right: 8px; }
.user-block { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-avatar { background: var(--color-primary); color: #fff; font-size: 14px; flex-shrink: 0; }
.user-name { color: #303133; font-size: 14px; font-weight: 500; }
.register-btn { background: var(--color-primary); color: #fff !important; padding: 7px 16px; border-radius: 6px; text-align: center; }
.mobile-menu-btn { display: none; margin-left: auto; color: var(--color-text); }
.mobile-panel { display: none; }
.site-main { flex: 1; max-width: 1200px; margin: 0 auto; padding: 20px; width: 100%; }
.site-footer { text-align: center; padding: 20px; color: #909399; font-size: 13px; border-top: 1px solid #e4e7ed; }

@media (max-width: 768px) {
  .header-inner { height: 58px; padding: 0 16px; }
  .logo { margin-right: 0; font-size: 18px; min-width: 0; }
  .logo-mark { width: 32px; height: 32px; border-radius: 9px; }
  .desktop-nav, .desktop-actions { display: none; }
  .mobile-menu-btn { display: inline-flex; }
  .mobile-panel {
    display: grid;
    gap: 10px;
    max-width: 1200px;
    margin: 0 auto;
    padding: 12px 16px 16px;
    background: #fff;
    border-top: 1px solid #eef2f6;
    box-shadow: 0 12px 24px rgba(25, 42, 70, 0.08);
  }
  .mobile-panel a,
  .mobile-user-link {
    width: 100%;
    min-height: 40px;
    display: flex;
    align-items: center;
    padding: 0 12px;
    border: 1px solid #edf1f5;
    border-radius: 8px;
    background: #fff;
    color: var(--color-text);
    font: inherit;
    text-align: left;
  }
  .mobile-msg-badge :deep(a) { width: 100%; }
  .mobile-panel a.router-link-active { border-color: #c9e4ff; color: var(--color-primary); background: #f2f8ff; }
}
</style>
