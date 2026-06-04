<template>
  <div class="dashboard-page">
    <h2 style="margin-bottom: 20px">数据大盘</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div class="dash-stat">
            <div class="dash-num">{{ data.totalUsers }}</div>
            <div class="dash-label">注册总人数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="dash-stat">
            <div class="dash-num">{{ data.activePosts }}</div>
            <div class="dash-label">正在寻找</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="dash-stat">
            <div class="dash-num">{{ data.resolvedPosts }}</div>
            <div class="dash-label">成功找回</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="dash-stat">
            <div class="dash-num">{{ data.totalArchives }}</div>
            <div class="dash-label">动物档案</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>数据导出</span>
      </template>
      <el-space>
        <el-button @click="exportArchive">导出动物登记台账 (Excel)</el-button>
        <el-button @click="exportPosts">导出寻宠成功案例统计 (Excel)</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'

const data = ref({ totalUsers: 0, activePosts: 0, resolvedPosts: 0, totalArchives: 0 })

function exportArchive() {
  window.open('/api/admin/export/archive', '_blank')
}

function exportPosts() {
  window.open('/api/admin/export/resolved-posts', '_blank')
}

onMounted(async () => {
  const res = await adminApi.dashboard()
  data.value = res.data
})
</script>

<style scoped>
.dash-stat { text-align: center; padding: 12px; }
.dash-num { font-size: 36px; font-weight: bold; color: #409eff; }
.dash-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
