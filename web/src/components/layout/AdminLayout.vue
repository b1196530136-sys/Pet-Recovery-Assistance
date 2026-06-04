<template>
  <div class="admin-layout">
    <el-container style="height: 100vh">
      <el-aside width="220px" style="background: #304156">
        <div class="admin-logo">管理后台</div>
        <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
          <el-menu-item index="/admin">数据大盘</el-menu-item>
          <el-menu-item index="/admin/posts">寻宠启事审核</el-menu-item>
          <el-menu-item index="/admin/archives">流浪动物档案审核</el-menu-item>
          <el-menu-item index="/admin/certifications">认证审批</el-menu-item>
          <el-menu-item index="/admin/users">用户权限管理</el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: flex-end; padding: 0 20px;">
          <el-dropdown>
            <span style="cursor: pointer">{{ userStore.userInfo?.nickname || '管理员' }}</span>
            <template #dropdown>
              <el-dropdown-item @click="userStore.logout(); $router.push('/auth/login')">退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </el-header>
        <el-main style="background: #f0f2f5; padding: 20px;">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
</script>

<style scoped>
.admin-logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; }
</style>
