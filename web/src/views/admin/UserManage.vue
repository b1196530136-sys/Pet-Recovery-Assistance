<template>
  <div class="admin-user-page">
    <h2 style="margin-bottom: 20px">用户权限管理</h2>
    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="role" label="当前角色" width="120">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-select
            v-model="row._newRole"
            placeholder="修改为"
            size="small"
            style="width: 120px; margin-right: 8px;"
            @change="r => row._newRole = r"
          >
            <el-option label="普通用户" value="USER" />
            <el-option label="认证用户" value="CERTIFIED" />
          </el-select>
          <el-button
            size="small"
            type="primary"
            :disabled="!row._newRole || row._newRole === row.role"
            @click="updateRole(row)"
          >
            确认
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'

const users = ref([])

const roleLabelMap = { USER: '普通用户', PENDING_CERT: '待认证', CERTIFIED: '认证用户', ADMIN: '管理员' }

function roleLabel(role) {
  return roleLabelMap[role] || role
}

function roleTagType(role) {
  return { USER: 'info', PENDING_CERT: 'warning', CERTIFIED: 'success', ADMIN: 'danger' }[role] || 'info'
}

async function load() {
  try {
    const res = await adminApi.users()
    users.value = (res.data || []).map(u => ({ ...u, _newRole: '' }))
  } catch { /* ignore */ }
}

async function updateRole(row) {
  if (!row._newRole) return
  try {
    await adminApi.updateUserRole(row.id, row._newRole)
    ElMessage.success('角色已更新')
    load()
  } catch { /* ignore */ }
}

onMounted(load)
</script>
