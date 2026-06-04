<template>
  <div class="admin-review-page">
    <h2 style="margin-bottom: 20px">认证审批</h2>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="当前角色" width="100" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="approve(row)">认证通过</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/user'
import { adminApi } from '@/api/admin'

const list = ref([])

async function load() {
  // 需要新增一个获取待认证用户的接口，这里使用模拟数据
  list.value = []
}

async function approve(row) {
  await adminApi.reviewCertification(row.id, 'APPROVED')
  ElMessage.success('已通过')
  load()
}

onMounted(load)
</script>
