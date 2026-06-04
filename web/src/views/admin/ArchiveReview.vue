<template>
  <div class="admin-review-page">
    <h2 style="margin-bottom: 20px">流浪动物档案审核</h2>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="animalType" label="类型" width="80">
        <template #default="{ row }">{{ typeMap[row.animalType] || row.animalType }}</template>
      </el-table-column>
      <el-table-column prop="address" label="发现地址" min-width="160" show-overflow-tooltip />
      <el-table-column label="现场照片" width="120">
        <template #default="{ row }">
          <el-image v-if="row.photos" :src="row.photos.split(',')[0]" :preview-src-list="row.photos.split(',')" style="width: 60px; height: 60px" fit="cover" preview-teleported />
          <span v-else style="color: #909399; font-size: 12px;">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="placementStatus" label="安置状态" width="100">
        <template #default="{ row }">{{ placementMap[row.placementStatus] || row.placementStatus }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="建档时间" width="170" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'">{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="approve(row)">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="reject(row)">驳回</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { archiveApi } from '@/api/archive'
import { adminApi } from '@/api/admin'

const list = ref([])
const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const placementMap = { observing: '原地观察', sheltered: '基地收容', adoptable: '开放领养' }
const statusMap = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }

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
  await adminApi.reviewArchive(row.id, 'REJECTED')
  ElMessage.success('已驳回')
  load()
}

onMounted(load)
</script>
