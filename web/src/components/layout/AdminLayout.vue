<template>
  <div class="admin-layout">
    <el-container class="admin-shell" style="height: 100vh">
      <el-aside class="admin-aside" width="220px" style="background: #304156">
        <div class="admin-logo">管理后台</div>
        <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
          <el-menu-item index="/admin">数据大盘</el-menu-item>
          <el-menu-item index="/admin/posts">
            <el-badge :value="pendingSummary.pendingPosts" :hidden="pendingSummary.pendingPosts === 0" :max="99" class="menu-badge">
              <span>寻宠启事审核</span>
            </el-badge>
          </el-menu-item>
          <el-menu-item index="/admin/archives">
            <el-badge :value="pendingSummary.pendingArchives" :hidden="pendingSummary.pendingArchives === 0" :max="99" class="menu-badge">
              <span>流浪动物档案审核</span>
            </el-badge>
          </el-menu-item>
          <el-menu-item index="/admin/certifications">
            <el-badge :value="pendingSummary.pendingCertifications" :hidden="pendingSummary.pendingCertifications === 0" :max="99" class="menu-badge">
              <span>认证审批</span>
            </el-badge>
          </el-menu-item>
          <el-menu-item index="/admin/reports">
            <el-badge :value="pendingSummary.pendingReports" :hidden="pendingSummary.pendingReports === 0" :max="99" class="menu-badge">
              <span>举报处理</span>
            </el-badge>
          </el-menu-item>
          <el-menu-item index="/admin/users">用户权限管理</el-menu-item>
          <el-menu-item index="/admin/front/posts" style="border-top: 1px solid #415a6e; margin-top: 8px; padding-top: 4px;">
            <span style="color: #409eff">浏览寻宠大厅</span>
          </el-menu-item>
          <el-menu-item index="/admin/front/archives">
            <span style="color: #409eff">浏览档案大厅</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="admin-header" style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: flex-end; padding: 0 20px;">
          <div class="pending-reminder" :class="{ active: pendingSummary.pendingTotal > 0 }">
            <el-badge :value="pendingSummary.pendingTotal" :hidden="pendingSummary.pendingTotal === 0" :max="99">
              <span>待处理信息</span>
            </el-badge>
          </div>
          <el-dropdown>
            <span style="cursor: pointer">{{ userStore.userInfo?.nickname || '管理员' }}</span>
            <template #dropdown>
              <el-dropdown-item @click="userStore.logout(); $router.push('/auth/login')">退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </el-header>
        <el-main class="admin-main" style="background: #f0f2f5; padding: 20px;">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { adminApi } from '@/api/admin'

const userStore = useUserStore()
const route = useRoute()
const pendingSummary = ref({
  pendingPosts: 0,
  pendingArchives: 0,
  pendingCertifications: 0,
  pendingReports: 0,
  pendingTotal: 0,
})

let pendingTimer = null

async function fetchPendingSummary() {
  try {
    const res = await adminApi.dashboard()
    pendingSummary.value = {
      pendingPosts: Number(res.data?.pendingPosts || 0),
      pendingArchives: Number(res.data?.pendingArchives || 0),
      pendingCertifications: Number(res.data?.pendingCertifications || 0),
      pendingReports: Number(res.data?.pendingReports || 0),
      pendingTotal: Number(res.data?.pendingTotal || 0),
    }
  } catch {
    // 管理后台数据刷新失败时保持当前提醒，不打断页面操作
  }
}

watch(() => route.fullPath, fetchPendingSummary)

onMounted(() => {
  fetchPendingSummary()
  pendingTimer = setInterval(fetchPendingSummary, 30000)
})

onBeforeUnmount(() => {
  if (pendingTimer) clearInterval(pendingTimer)
})
</script>

<style scoped>
.admin-logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; }
.admin-aside { box-shadow: 6px 0 20px rgba(19, 36, 56, 0.08); }
.admin-shell > :deep(.el-container) { min-width: 0; }
.admin-main { overflow-x: auto; min-width: 0; }
.menu-badge { width: 100%; }
.menu-badge :deep(.el-badge__content) { top: 50%; right: 4px; transform: translateY(-50%) translateX(100%); }
.pending-reminder { margin-right: 24px; color: #909399; font-size: 14px; }
.pending-reminder.active { color: #e6a23c; font-weight: 500; }

@media (max-width: 980px) {
  .admin-aside { width: 200px !important; }
  .admin-main { padding: 16px !important; }
}

@media (max-width: 768px) {
  .admin-shell {
    flex-direction: column;
    height: auto !important;
    min-height: 100vh;
  }

  .admin-aside {
    width: 100% !important;
    height: auto;
    box-shadow: 0 8px 18px rgba(19, 36, 56, 0.12);
  }

  .admin-logo {
    height: 52px;
    justify-content: flex-start;
    padding: 0 16px;
  }

  .admin-aside :deep(.el-menu) {
    display: flex;
    overflow-x: auto;
    border-right: 0;
    scrollbar-width: thin;
  }

  .admin-aside :deep(.el-menu-item) {
    flex: 0 0 auto;
    height: 46px;
    line-height: 46px;
    padding: 0 14px !important;
  }

  .admin-aside :deep(.el-menu-item[style]) {
    margin-top: 0 !important;
    padding-top: 0 !important;
    border-top: 0 !important;
    border-left: 1px solid #415a6e;
  }

  .menu-badge :deep(.el-badge__content) {
    right: -4px;
    top: 8px;
    transform: translateX(100%);
  }

  .admin-header {
    min-height: 56px;
    height: auto !important;
    justify-content: space-between !important;
    gap: 12px;
    padding: 10px 14px !important;
  }

  .pending-reminder {
    margin-right: 0;
    line-height: 1.4;
  }

  .admin-main {
    width: 100%;
    padding: 14px !important;
  }
}
</style>
