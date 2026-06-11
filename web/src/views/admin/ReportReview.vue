<template>
  <div class="admin-report-page">
    <div class="page-head">
      <h2>举报处理</h2>
      <el-switch
        v-model="showAll"
        inline-prompt
        active-text="全部"
        inactive-text="待处理"
        @change="load"
      />
    </div>

    <el-table :data="reports" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="举报人" min-width="150">
        <template #default="{ row }">
          <div>{{ row.reporterName }}</div>
          <small>{{ row.reporterEmail }}</small>
        </template>
      </el-table-column>
      <el-table-column label="被举报人" min-width="150">
        <template #default="{ row }">
          <div>{{ row.reportedName }}</div>
          <small>{{ row.reportedEmail }}</small>
        </template>
      </el-table-column>
      <el-table-column prop="reportType" label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="reportTypeTag(row.reportType)" size="small">{{ reportTypeLabel(row.reportType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因摘要" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createTime" label="举报时间" width="180" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button size="small" @click="viewReport(row)">查看</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleReport(row, 'RESOLVED')">处理通过</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleReport(row, 'REJECTED')">驳回举报</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="举报详情" width="720px" destroy-on-close>
      <template v-if="currentReport">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="举报人">{{ currentReport.reporterName }}（{{ currentReport.reporterEmail }}）</el-descriptions-item>
          <el-descriptions-item label="被举报人">{{ currentReport.reportedName }}（{{ currentReport.reportedEmail }}）</el-descriptions-item>
          <el-descriptions-item label="举报类型">{{ reportTypeLabel(currentReport.reportType) }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">{{ statusLabel(currentReport.status) }}</el-descriptions-item>
          <el-descriptions-item label="原因摘要" :span="2">{{ currentReport.reason || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="补充说明" :span="2">{{ currentReport.detail || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="处理备注" :span="2">{{ currentReport.handleNote || '暂无' }}</el-descriptions-item>
        </el-descriptions>

        <div class="snapshot-block">
          <h3>消息快照</h3>
          <div v-if="currentReport.messageSnapshot?.content || currentReport.message?.content" class="snapshot-card">
            <p>{{ currentReport.messageSnapshot?.content || currentReport.message?.content || '无文本内容' }}</p>
            <small>{{ currentReport.messageSnapshot?.createTime || currentReport.message?.createTime || '' }}</small>
          </div>
          <el-empty v-else description="无消息快照" />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const showAll = ref(false)
const reports = ref([])
const detailVisible = ref(false)
const currentReport = ref(null)

function reportTypeLabel(type) {
  return { SPAM: '垃圾骚扰', HARASSMENT: '骚扰辱骂', FALSE_CLUE: '虚假线索', OTHER: '其他' }[type] || type
}

function reportTypeTag(type) {
  return { SPAM: 'warning', HARASSMENT: 'danger', FALSE_CLUE: 'info', OTHER: '' }[type] || ''
}

function statusLabel(status) {
  return { PENDING: '待处理', RESOLVED: '已处理', REJECTED: '已驳回' }[status] || status
}

function statusTag(status) {
  return { PENDING: 'warning', RESOLVED: 'success', REJECTED: 'info' }[status] || ''
}

async function load() {
  try {
    const res = await adminApi.reports({ all: showAll.value })
    reports.value = res.data || []
  } catch { /* ignore */ }
}

function viewReport(row) {
  currentReport.value = row
  detailVisible.value = true
}

async function handleReport(row, action) {
  let value = ''
  let banReportedUser = false
  try {
    const result = await ElMessageBox.prompt(
      action === 'RESOLVED' ? '请填写处理说明，可选勾选是否封禁该用户。' : '请填写驳回原因，便于举报人了解结果。',
      action === 'RESOLVED' ? '处理举报' : '驳回举报',
      {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        inputPlaceholder: action === 'RESOLVED' ? '例如：已核实存在骚扰行为' : '例如：证据不足，暂不支持处理',
      }
    )
    value = result.value || ''
  } catch { return }

  if (action === 'RESOLVED') {
    try {
      await ElMessageBox.confirm('是否同时封禁被举报用户？如暂不封禁，仍会保留举报处理记录。', '附加操作', {
        confirmButtonText: '封禁并处理',
        cancelButtonText: '仅处理不封禁',
        distinguishCancelAndClose: true,
        type: 'warning',
      })
      banReportedUser = true
    } catch (error) {
      if (error !== 'cancel') return
    }
  }

  try {
    await adminApi.handleReport(row.id, { action, handleNote: value, banReportedUser })
    ElMessage.success(action === 'RESOLVED' ? '举报已处理' : '举报已驳回')
    if (detailVisible.value && currentReport.value?.id === row.id) {
      detailVisible.value = false
    }
    load()
  } catch { /* ignore */ }
}

onMounted(load)
</script>

<style scoped>
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.page-head h2 { margin: 0; }
.snapshot-block { margin-top: 20px; }
.snapshot-block h3 { margin: 0 0 12px; font-size: 16px; color: #303133; }
.snapshot-card { padding: 14px 16px; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 8px; color: #303133; line-height: 1.7; }
.snapshot-card p { margin: 0 0 8px; white-space: pre-wrap; word-break: break-word; }
.snapshot-card small { color: #98a2b3; }
</style>
