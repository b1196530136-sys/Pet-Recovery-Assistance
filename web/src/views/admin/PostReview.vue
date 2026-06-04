<template>
  <div class="admin-review-page">
    <h2 style="margin-bottom: 20px">寻宠启事审核</h2>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="petName" label="宠物名" />
      <el-table-column prop="petType" label="类型" width="80" />
      <el-table-column label="照片" width="120">
        <template #default="{ row }">
          <template v-if="row.photos">
            <el-image
              v-for="(url, idx) in row.photos.split(',')"
              :key="idx"
              :src="url"
              :preview-src-list="row.photos.split(',')"
              :initial-index="idx"
              style="width: 60px; height: 60px; margin-right: 4px;"
              fit="cover"
              preview-teleported
            />
          </template>
          <span v-else style="color: #c0c4cc;">无照片</span>
        </template>
      </el-table-column>
      <el-table-column prop="address" label="丢失地址" />
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'">{{ row.status }}</el-tag>
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
import { postApi } from '@/api/post'
import { adminApi } from '@/api/admin'

const list = ref([])

async function load() {
  const res = await postApi.search({ status: 'PENDING', page: 1, size: 100 })
  list.value = res.data.records
}

async function approve(row) {
  await adminApi.reviewPost(row.id, 'APPROVED')
  ElMessage.success('已通过')
  load()
}

async function reject(row) {
  await adminApi.reviewPost(row.id, 'REJECTED')
  ElMessage.success('已驳回')
  load()
}

onMounted(load)
</script>
