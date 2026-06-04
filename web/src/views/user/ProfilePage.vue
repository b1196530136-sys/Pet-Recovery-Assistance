<template>
  <div class="profile-page" v-loading="loading">
    <!-- 个人信息卡片 -->
    <el-card class="profile-card">
      <div class="profile-header">
        <el-avatar :size="80" :src="userInfo?.avatar" class="profile-avatar">
          {{ displayName.charAt(0) }}
        </el-avatar>
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
          :auto-upload="true"
          :on-success="onCertUploadSuccess"
          :on-error="() => ElMessage.error('上传失败')"
          :before-upload="beforeCertUpload"
          list-type="picture-card"
          :limit="3"
          multiple
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <p style="font-size: 13px; color: #909399; margin-top: 8px;">请上传相关凭证（最多3张），如身份证明、领养证明等</p>
        <template #footer>
          <el-button @click="showCertDialog = false">取消</el-button>
          <el-button type="primary" :loading="certLoading" :disabled="!certUrls.length" @click="submitCert">
            提交申请
          </el-button>
        </template>
      </el-dialog>
    </el-card>

    <!-- 我的寻宠启事 -->
    <el-card class="section-card">
      <template #header>
        <span>我发布的寻宠启事</span>
      </template>
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/posts/${row.id}`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无发布的寻宠启事" />
    </el-card>

    <!-- 提供过线索的寻宠 -->
    <el-card class="section-card">
      <template #header>
        <span>提供过线索的寻宠启事</span>
      </template>
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/posts/${row.id}`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无提供过线索的寻宠启事" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { postApi } from '@/api/post'
import { userApi } from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const certLoading = ref(false)
const myPosts = ref([])
const cluedPosts = ref([])
const showCertDialog = ref(false)
const certUrls = ref([])
const certUploading = ref(false)

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
    const [myRes, cluedRes] = await Promise.all([
      postApi.my(),
      postApi.clued(),
      userStore.fetchProfile(),
    ])
    myPosts.value = myRes.data || []
    cluedPosts.value = cluedRes.data || []
  } catch { /* ignore */ }
  loading.value = false
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
</style>
