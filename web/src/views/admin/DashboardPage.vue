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
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import request from '@/utils/request'

const data = ref({ totalUsers: 0, activePosts: 0, resolvedPosts: 0, totalArchives: 0 })

async function downloadExport(url, filename) {
  try {
    const res = await request.get(url, { responseType: 'blob' })
    // 拦截器对 blob 返回了完整 response，取 response.data
    const blob = res.data || res
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
    link.download = filename
    link.click()
    URL.revokeObjectURL(link.href)
  } catch {
    ElMessage.error('导出失败')
  }
}

function exportArchive() {
  downloadExport('/admin/export/archive', '动物登记台账.xlsx')
}

function exportPosts() {
  downloadExport('/admin/export/resolved-posts', '寻宠成功案例统计.xlsx')
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
