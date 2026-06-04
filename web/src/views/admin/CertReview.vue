<template>
  <div class="admin-review-page">
    <h2 style="margin-bottom: 20px">认证审批</h2>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="当前角色" width="120">
        <template #default>
          <el-tag type="warning">待认证</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="认证凭证" min-width="180">
        <template #default="{ row }">
          <div v-if="row.certCredentials" class="cert-imgs">
            <el-image
              v-for="url in row.certCredentials.split(',')"
              :key="url"
              :src="url"
              style="width: 60px; height: 60px; border-radius: 4px; margin-right: 6px; cursor: pointer;"
              fit="cover"
              :preview-src-list="row.certCredentials.split(',')"
              preview-teleported
            />
          </div>
          <span v-else style="color: #c0c4cc; font-size: 13px;">无凭证</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="approve(row)">认证通过</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!list.length" description="暂无待认证用户" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'

const list = ref([])

async function load() {
  try {
    const res = await adminApi.pendingCertifications()
    list.value = res.data || []
  } catch { /* ignore */ }
}

async function approve(row) {
  await adminApi.reviewCertification(row.id, 'APPROVED')
  ElMessage.success('已通过')
  load()
}

onMounted(load)
</script>
