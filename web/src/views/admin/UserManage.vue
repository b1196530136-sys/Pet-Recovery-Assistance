<template>
  <div class="admin-user-page">
    <h2 style="margin-bottom: 20px">用户权限管理</h2>
    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="role" label="当前角色" width="100">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" type="danger" size="small">已封禁</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-select
            v-model="row._newRole"
            placeholder="修改为"
            size="small"
            style="width: 100px; margin-right: 6px;"
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
          <el-button
            v-if="row.status !== 0"
            size="small"
            type="danger"
            @click="handleBan(row)"
          >
            封禁
          </el-button>
          <el-button
            v-else
            size="small"
            type="warning"
            @click="handleUnban(row)"
          >
            解封
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

async function handleBan(row) {
  try {
    await ElMessageBox.confirm(`确定封禁用户「${row.nickname || row.email}」吗？封禁后该用户将无法登录。`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  } catch { return }
  try {
    await adminApi.banUser(row.id)
    ElMessage.success('已封禁')
    load()
  } catch { /* ignore */ }
}

async function handleUnban(row) {
  try {
    await ElMessageBox.confirm(`确定解封用户「${row.nickname || row.email}」吗？`, '提示', { type: 'info', confirmButtonText: '确定', cancelButtonText: '取消' })
  } catch { return }
  try {
    await adminApi.unbanUser(row.id)
    ElMessage.success('已解封')
    load()
  } catch { /* ignore */ }
}

onMounted(load)
</script>
