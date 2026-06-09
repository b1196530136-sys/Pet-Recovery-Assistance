<template>
  <div class="profile-page" v-loading="loading">
    <el-button @click="$router.back()" :icon="ArrowLeft" style="margin-bottom: 16px">返回</el-button>
    <!-- 个人信息卡片 -->
    <el-card class="profile-card">
      <div class="profile-header">
        <el-avatar :size="80" :src="userInfo?.avatar || '/images/default-avatar.png'" class="profile-avatar" style="cursor: pointer" @click="avatarInput.click()" />
        <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="onAvatarChange" />
        <div class="profile-info">
          <div class="profile-name-row">
            <span class="profile-name">{{ displayName }}</span>
            <el-tag v-if="userInfo?.role === 'CERTIFIED'" type="success" size="small">已认证</el-tag>
            <el-tag v-else-if="userInfo?.role === 'PENDING_CERT'" type="warning" size="small">认证审核中</el-tag>
            <el-tag v-else-if="userInfo?.role === 'ADMIN'" type="danger" size="small">管理员</el-tag>
            <el-tag v-else type="info" size="small">普通用户</el-tag>
          </div>
          <div class="profile-id">专属ID: {{ userInfo?.id }}</div>
          <div class="profile-email">{{ userInfo?.email }}</div>
        </div>
      </div>

      <!-- 认证申请 -->
      <div v-if="userInfo?.role === 'USER' || userInfo?.role === 'PENDING_CERT'" class="cert-section">
        <el-divider />
        <div class="cert-row">
          <span style="font-size: 14px; color: #606266;">
            <template v-if="userInfo?.role === 'PENDING_CERT'">认证申请已提交，请等待管理员审核</template>
            <template v-else>申请成为认证用户，获得更多平台权限</template>
          </span>
          <el-button v-if="userInfo?.role === 'USER'" type="primary" size="small" @click="showCertDialog = true">
            申请认证
          </el-button>
          <el-tag v-else type="warning">审核中</el-tag>
        </div>
      </div>

      <!-- 认证凭证上传弹窗 -->
      <el-dialog v-model="showCertDialog" title="提交认证凭证" width="500px">
        <el-upload
          ref="certUploadRef"
          action="/api/upload/image"
          accept="image/*"
          :headers="uploadHeaders"
          :auto-upload="true"
          :on-success="onCertUploadSuccess"
          :on-error="() => ElMessage.error('上传失败')"
          :before-upload="beforeCertUpload"
          list-type="picture-card"
          :limit="3"
          multiple
        >
          <div style="display: flex; flex-direction: column; align-items: center;">
            <el-icon><Plus /></el-icon>
            <span style="font-size: 12px; color: #909399; margin-top: 4px;">点击上传图片</span>
          </div>
        </el-upload>
        <p style="font-size: 13px; color: #909399; margin-top: 8px;">请上传相关凭证（最多3张）</p>
        <template #footer>
          <el-button @click="showCertDialog = false">取消</el-button>
          <el-button type="primary" :loading="certLoading" :disabled="!certUrls.length" @click="submitCert">
            提交申请
          </el-button>
        </template>
      </el-dialog>
    </el-card>

    <el-tabs class="profile-tabs" type="border-card">
      <el-tab-pane label="我发布的寻宠">
        <el-table v-if="myPosts.length" :data="myPosts" stripe style="width: 100%">
          <el-table-column prop="petName" label="宠物名" width="120" />
          <el-table-column prop="petType" label="类型" width="80">
            <template #default="{ row }">{{ typeMap[row.petType] || row.petType }}</template>
          </el-table-column>
          <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" width="170" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="$router.push(`/posts/${row.id}`)">查看</el-button>
              <el-button v-if="row.status === 'RESOLVED' || row.status === 'REJECTED'" size="small" type="danger" @click="handleDeletePost(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无发布的寻宠启事" />
      </el-tab-pane>

      <el-tab-pane label="提供过线索">
        <el-table v-if="cluedPosts.length" :data="cluedPosts" stripe style="width: 100%">
          <el-table-column prop="petName" label="宠物名" width="120" />
          <el-table-column prop="petType" label="类型" width="80">
            <template #default="{ row }">{{ typeMap[row.petType] || row.petType }}</template>
          </el-table-column>
          <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" width="170" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="$router.push(`/posts/${row.id}`)">查看</el-button>
              <el-button v-if="row.status === 'RESOLVED' || row.status === 'REJECTED'" size="small" type="danger" @click="handleDeleteCluedPost(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无提供过线索的寻宠启事" />
      </el-tab-pane>

      <el-tab-pane label="收到申请">
        <el-table v-if="incomingRequests.length" :data="incomingRequests" stripe style="width: 100%">
          <el-table-column prop="animalType" label="档案类型" width="100">
            <template #default="{ row }">{{ typeMap[row.animalType] || row.animalType }}</template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请人" width="100" />
          <el-table-column prop="message" label="留言" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="adoptStatusTagType(row.status)">{{ adoptStatusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="170" />
          <el-table-column label="操作" width="160" v-if="hasPendingIncoming">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleReview(row, 'APPROVED')">接受</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleReview(row, 'REJECTED')">拒绝</el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无收到的领养申请" />
      </el-tab-pane>

      <el-tab-pane label="我的申请">
        <el-table v-if="myRequests.length" :data="myRequests" stripe style="width: 100%">
          <el-table-column prop="animalType" label="档案类型" width="100">
            <template #default="{ row }">{{ typeMap[row.animalType] || row.animalType }}</template>
          </el-table-column>
          <el-table-column prop="ownerName" label="发布人" width="100" />
          <el-table-column prop="message" label="留言" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="adoptStatusTagType(row.status)">{{ adoptStatusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="170" />
        </el-table>
        <el-empty v-else description="暂无领养申请" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { postApi } from '@/api/post'
import { userApi } from '@/api/user'
import { adoptionApi } from '@/api/adoption'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const certLoading = ref(false)
const myPosts = ref([])
const cluedPosts = ref([])
const showCertDialog = ref(false)
const certUrls = ref([])
const avatarInput = ref(null)
const avatarLoading = ref(false)
const certUploading = ref(false)
const incomingRequests = ref([])
const myRequests = ref([])
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const adoptStatusMap = { PENDING: '待处理', APPROVED: '已通过', REJECTED: '已拒绝' }
function adoptStatusTagType(status) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info'
}
const hasPendingIncoming = computed(() => incomingRequests.value.some(r => r.status === 'PENDING'))

const userInfo = computed(() => userStore.userInfo)

const displayName = computed(() => {
  const info = userInfo.value
  if (info?.nickname) return info.nickname
  if (info?.email) return info.email.split('@')[0]
  return '用户'
})

const statusMap = { PENDING: '待审核', ACTIVE: '寻找中', REJECTED: '已驳回', RESOLVED: '已找到' }
const typeMap = { cat: '猫', dog: '狗', other: '其他' }

function statusTagType(status) {
  return { PENDING: 'warning', ACTIVE: 'primary', REJECTED: 'danger', RESOLVED: 'success' }[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const [myRes, cluedRes, incomingRes, myAdoptRes] = await Promise.all([
      postApi.my(),
      postApi.clued(),
      adoptionApi.incoming(),
      adoptionApi.my(),
      userStore.fetchProfile(),
    ])
    myPosts.value = myRes.data || []
    cluedPosts.value = cluedRes.data || []
    incomingRequests.value = incomingRes.data || []
    myRequests.value = myAdoptRes.data || []
  } catch { /* ignore */ }
  loading.value = false
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  if (file.size / 1024 / 1024 >= 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }
  avatarLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadRes = await fetch('/api/upload/image', {
      method: 'POST',
      headers: { Authorization: `Bearer ${userStore.token}` },
      body: formData,
    })
    const json = await uploadRes.json()
    if (json.code === 200 && json.data) {
      await userApi.updateAvatar(json.data)
      await userStore.fetchProfile()
      ElMessage.success('头像已更新')
    } else {
      ElMessage.error(json.message || '上传失败')
    }
  } catch {
    ElMessage.error('头像上传失败')
  }
  avatarLoading.value = false
  avatarInput.value.value = ''
}

function onCertUploadSuccess(response) {
  certUploading.value = false
  if (response?.code === 200 && response?.data) {
    certUrls.value.push(response.data)
  }
}

function beforeCertUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 >= 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  certUploading.value = true
  return true
}

async function submitCert() {
  if (!certUrls.value.length) return
  certLoading.value = true
  try {
    await userApi.applyCertification(certUrls.value.join(','))
    ElMessage.success('认证申请已提交，请等待管理员审核')
    showCertDialog.value = false
    certUrls.value = []
    await userStore.fetchProfile()
  } catch { /* ignore */ }
  certLoading.value = false
}

async function handleDeletePost(row) {
  try {
    await ElMessageBox.confirm('确定要删除这条寻宠启事吗？', '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  } catch { return }
  try {
    await postApi.delete(row.id)
    ElMessage.success('删除成功')
    myPosts.value = myPosts.value.filter(p => p.id !== row.id)
  } catch { /* ignore */ }
}

async function handleDeleteCluedPost(row) {
  try {
    await ElMessageBox.confirm('确定要删除这条寻宠启事吗？', '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  } catch { return }
  try {
    await postApi.delete(row.id)
    ElMessage.success('删除成功')
    cluedPosts.value = cluedPosts.value.filter(p => p.id !== row.id)
  } catch { /* ignore */ }
}

async function handleReview(row, action) {
  const label = action === 'APPROVED' ? '接受' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${label}该领养申请吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  } catch { return }
  try {
    await adoptionApi.review(row.id, action)
    ElMessage.success(`已${label}`)
  } catch { /* ignore */ }
  // 重新从服务器加载列表，确保状态同步
  const [incomingRes, myAdoptRes] = await Promise.all([
    adoptionApi.incoming(),
    adoptionApi.my(),
  ])
  incomingRequests.value = incomingRes.data || []
  myRequests.value = myAdoptRes.data || []
}

onMounted(loadData)
</script>

<style scoped>
.profile-page { max-width: 900px; margin: 0 auto; }
.profile-card { margin-bottom: 20px; }
.profile-header { display: flex; align-items: center; gap: 24px; }
.profile-avatar { flex-shrink: 0; background: #409eff; color: #fff; font-size: 32px; }
.profile-info { flex: 1; }
.profile-name-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.profile-name { font-size: 22px; font-weight: 600; color: #303133; }
.profile-id { font-size: 13px; color: #909399; margin-bottom: 4px; }
.profile-email { font-size: 14px; color: #606266; }
.cert-row { display: flex; align-items: center; justify-content: space-between; }
.section-card { margin-bottom: 20px; }
.profile-tabs { margin-bottom: 20px; overflow-x: auto; }
.profile-tabs :deep(.el-tabs__content) { min-height: 260px; }
.profile-tabs :deep(.el-empty) { padding: 36px 0; }
.profile-tabs :deep(.el-empty__image) { width: 120px; }
.profile-tabs :deep(.el-table) { min-width: 760px; }

@media (max-width: 768px) {
  .profile-page { max-width: none; }
  .profile-header { align-items: flex-start; gap: 16px; }
  .profile-avatar { width: 64px !important; height: 64px !important; }
  .profile-name-row { flex-wrap: wrap; }
  .cert-row { display: grid; gap: 10px; }
  .profile-tabs :deep(.el-tabs__nav) { white-space: nowrap; }
  .profile-tabs :deep(.el-tabs__content) { min-height: 220px; padding: 12px; }
}
</style>
