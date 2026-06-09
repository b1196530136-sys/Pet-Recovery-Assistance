<template>
  <div class="dashboard-page">
    <h2 style="margin-bottom: 20px">数据大盘</h2>
    <el-alert
      v-if="data.pendingTotal > 0"
      class="pending-alert"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        当前有 {{ data.pendingTotal }} 条待处理信息，请及时处理
      </template>
    </el-alert>
    <el-card class="pending-card">
      <template #header>
        <span>待处理信息</span>
      </template>
      <el-row :gutter="16">
        <el-col v-for="item in pendingItems" :key="item.path" :xs="24" :sm="8">
          <div class="pending-item" :class="{ active: item.count > 0 }" @click="goPending(item.path)">
            <div class="pending-count">{{ item.count }}</div>
            <div class="pending-label">{{ item.label }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
    <el-row :gutter="20">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card>
          <div class="dash-stat">
            <div class="dash-num">{{ data.totalUsers }}</div>
            <div class="dash-label">注册总人数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card>
          <div class="dash-stat">
            <div class="dash-num">{{ data.activePosts }}</div>
            <div class="dash-label">正在寻找</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card>
          <div class="dash-stat">
            <div class="dash-num">{{ data.resolvedPosts }}</div>
            <div class="dash-label">成功找回</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
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
      <el-space wrap>
        <el-button @click="exportArchive">导出动物登记台账 (Excel)</el-button>
        <el-button @click="exportPosts">导出寻宠成功案例统计 (Excel)</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import request from '@/utils/request'

const router = useRouter()
const data = ref({
  totalUsers: 0,
  activePosts: 0,
  resolvedPosts: 0,
  totalArchives: 0,
  pendingPosts: 0,
  pendingArchives: 0,
  pendingCertifications: 0,
  pendingTotal: 0,
})

const pendingItems = computed(() => [
  { label: '寻宠启事待审', count: Number(data.value.pendingPosts || 0), path: '/admin/posts' },
  { label: '档案变更待审', count: Number(data.value.pendingArchives || 0), path: '/admin/archives' },
  { label: '认证申请待审', count: Number(data.value.pendingCertifications || 0), path: '/admin/certifications' },
])

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

function goPending(path) {
  router.push(path)
}

onMounted(async () => {
  const res = await adminApi.dashboard()
  data.value = res.data
})
</script>

<style scoped>
.pending-alert { margin-bottom: 16px; }
.pending-card { margin-bottom: 20px; }
.pending-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  padding: 18px 12px;
  text-align: center;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}
.pending-item:hover { border-color: #409eff; box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12); transform: translateY(-1px); }
.pending-item.active { border-color: #e6a23c; background: #fdf6ec; }
.pending-count { color: #303133; font-size: 30px; font-weight: bold; line-height: 1; }
.pending-item.active .pending-count { color: #e6a23c; }
.pending-label { color: #606266; font-size: 14px; margin-top: 10px; }
.dash-stat { text-align: center; padding: 12px; }
.dash-num { font-size: 36px; font-weight: bold; color: #409eff; }
.dash-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
