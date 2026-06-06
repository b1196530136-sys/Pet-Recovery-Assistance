<template>
  <div class="admin-review-page">
    <h2 style="margin-bottom: 20px">流浪动物档案审核</h2>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="animalType" label="类型" width="80">
        <template #default="{ row }">{{ typeMap[row.animalType] || row.animalType }}</template>
      </el-table-column>
      <el-table-column prop="name" label="昵称" width="100" />
      <el-table-column prop="address" label="发现地址" min-width="140" show-overflow-tooltip />
      <el-table-column label="现场照片" width="120">
        <template #default="{ row }">
          <el-image v-if="row.photos" :src="row.photos.split(',')[0]" :preview-src-list="row.photos.split(',')" style="width: 60px; height: 60px" fit="cover" preview-teleported />
          <span v-else style="color: #909399; font-size: 12px;">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="healthStatus" label="健康状况" width="90" />
      <el-table-column prop="neuteredStatus" label="绝育状态" width="90" />
      <el-table-column prop="immuneStatus" label="免疫状态" width="90" />
      <el-table-column prop="placementStatus" label="安置状态" width="100">
        <template #default="{ row }">{{ placementMap[row.placementStatus] || row.placementStatus }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
          <el-tag v-if="row.isUpdate" type="info" size="small" style="margin-left: 4px">修改</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-if="row.isUpdate" size="small" type="primary" plain @click="showDiff(row)">查看修改</el-button>
          <el-button v-if="row.status === 'PENDING' || row.isUpdate" size="small" type="success" @click="approve(row)">通过</el-button>
          <el-button v-if="row.status === 'PENDING' || row.isUpdate" size="small" type="danger" @click="reject(row)">驳回</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 修改对比弹窗 -->
    <el-dialog v-model="diffVisible" title="修改内容对比" width="700px" destroy-on-close>
      <el-descriptions :column="1" border size="default">
        <el-descriptions-item v-for="(label, key) in fieldLabels" :key="key" :label="label">
          <div style="display: flex; align-items: flex-start; gap: 12px;">
            <div style="flex: 1; color: #909399; text-decoration: line-through;">{{ formatValue(oldData[key]) }}</div>
            <el-icon><ArrowRight /></el-icon>
            <div style="flex: 1; color: #E6A23C; font-weight: 500;">{{ formatValue(newData[key]) }}</div>
            <div v-if="key === 'photos' && newData[key]" style="margin-top: 4px; width: 100%;">
              <el-image
                v-for="(photo, idx) in newData[key].split(',')" :key="idx"
                :src="getPhotoUrl(photo)"
                :preview-src-list="newData[key].split(',').map(p => getPhotoUrl(p))"
                style="width: 80px; height: 80px; margin-right: 4px;" fit="cover"
                preview-teleported
              />
            </div>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { archiveApi } from '@/api/archive'
import { adminApi } from '@/api/admin'

const list = ref([])
const diffVisible = ref(false)
const oldData = ref({})
const newData = ref({})

const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const placementMap = { observing: '原地观察', sheltered: '基地收容', adoptable: '开放领养', adopted: '已被领养' }
const statusMap = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }

const fieldLabels = {
  animalType: '动物类型',
  name: '动物昵称',
  address: '发现地址',
  healthStatus: '健康状况',
  neuteredStatus: '绝育状态',
  immuneStatus: '免疫状态',
  placementStatus: '安置状态',
  photos: '现场照片',
  description: '备注描述'
}

function formatValue(val) {
  if (val == null || val === '') return '—'
  return val
}

function getPhotoUrl(photo) {
  if (!photo) return ''
  if (photo.startsWith('http')) return photo
  if (photo.startsWith('/')) return photo
  return '/upload/' + photo
}

function showDiff(row) {
  try {
    const pending = JSON.parse(row.pendingData)
    const old = {
      animalType: typeMap[row.animalType] || row.animalType,
      name: row.name || '',
      address: row.address,
      healthStatus: row.healthStatus,
      neuteredStatus: row.neuteredStatus,
      immuneStatus: row.immuneStatus,
      placementStatus: placementMap[row.placementStatus] || row.placementStatus,
      photos: row.photos,
      description: row.description
    }
    const next = {
      animalType: typeMap[pending.animalType] || pending.animalType,
      name: pending.name || '',
      address: pending.address,
      healthStatus: pending.healthStatus,
      neuteredStatus: pending.neuteredStatus,
      immuneStatus: pending.immuneStatus,
      placementStatus: placementMap[pending.placementStatus] || pending.placementStatus,
      photos: pending.photos,
      description: pending.description
    }
    // 只保留有变化的字段
    const changed = {}
    for (const key of Object.keys(fieldLabels)) {
      if (String(old[key] || '') !== String(next[key] || '')) {
        changed[key] = { old: old[key], new: next[key] }
      }
    }
    if (Object.keys(changed).length === 0) {
      ElMessage.info('未检测到内容变化')
      return
    }
    oldData.value = Object.fromEntries(Object.entries(changed).map(([k, v]) => [k, v.old]))
    newData.value = Object.fromEntries(Object.entries(changed).map(([k, v]) => [k, v.new]))
    diffVisible.value = true
  } catch (e) {
    ElMessage.error('修改数据解析失败')
  }
}

async function load() {
  const res = await archiveApi.pending({ page: 1, size: 100 })
  list.value = res.data.records
}

async function approve(row) {
  await adminApi.reviewArchive(row.id, 'APPROVED')
  ElMessage.success('已通过')
  load()
}

async function reject(row) {
  const hint = row.isUpdate ? '将仅驳回修改内容，原档案保留' : '该档案将被驳回删除'
  await ElMessageBox.confirm(`确定要驳回吗？${hint}`, '确认驳回', {
    confirmButtonText: '确定驳回',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await adminApi.reviewArchive(row.id, 'REJECTED')
  ElMessage.success('已驳回')
  load()
}

onMounted(load)
</script>
