<template>
  <div class="public-profile-page" v-loading="loading">
    <el-button @click="$router.back()" :icon="ArrowLeft" class="back-btn">返回</el-button>

    <el-card v-if="profile" class="profile-card">
      <div class="profile-header">
        <el-avatar :size="78" :src="profile.avatar || DEFAULT_AVATAR" class="profile-avatar" />
        <div class="profile-info">
          <div class="profile-name-row">
            <span class="profile-name">{{ profile.nickname || '匿名用户' }}</span>
            <el-tag :type="roleTagType(profile.role)" size="small">{{ roleText(profile.role) }}</el-tag>
          </div>
          <div class="profile-meta">专属ID: {{ profile.id }}</div>
          <div class="profile-meta">加入时间: {{ formatDate(profile.createTime) || '未知' }}</div>
          <div class="profile-meta">发布寻宠: {{ profile.postCount || 0 }} 条</div>
        </div>
        <div class="profile-actions">
          <el-button type="warning" plain @click="openReportDialog">举报</el-button>
          <el-button
            :type="conversationMeta.iBlocked ? 'info' : 'danger'"
            plain
            @click="conversationMeta.iBlocked ? handleUnblockUser() : handleBlockUser()"
          >
            {{ conversationMeta.iBlocked ? '取消拉黑' : '拉黑' }}
          </el-button>
          <el-tag v-if="conversationMeta.pendingReportCount" type="warning" size="small">举报处理中</el-tag>
        </div>
      </div>

      <el-divider />

      <div class="contact-row">
        <div>
          <strong>联系电话</strong>
          <p>{{ phoneText }}</p>
        </div>
        <el-tag v-if="profile.phoneVisible" type="success">已解锁</el-tag>
        <el-tag v-else-if="profile.hasPhone" type="info">已隐藏</el-tag>
        <el-tag v-else type="warning">未填写</el-tag>
      </div>
    </el-card>

    <el-card class="posts-card">
      <template #header>
        <div class="posts-header">
          <span>发布的寻宠</span>
          <small>{{ profile?.postCount || 0 }} 条</small>
        </div>
      </template>

      <div v-if="posts.length" class="post-grid">
        <article v-for="post in posts" :key="post.id" class="post-item" @click="openPost(post.id)">
          <div class="post-cover">
            <el-image v-if="firstPhoto(post.photos)" :src="firstPhoto(post.photos)" fit="cover" />
            <div v-else class="post-cover-placeholder">暂无照片</div>
            <span class="post-status" :class="post.status">{{ statusText(post.status) }}</span>
          </div>
          <div class="post-body">
            <h3>{{ post.petName || '未命名' }} ({{ typeMap[post.petType] || post.petType || '未知' }})</h3>
            <p>{{ post.address || '未填写地点' }}</p>
            <small>{{ formatDate(post.lostTime) || formatDate(post.createTime) }}</small>
          </div>
        </article>
      </div>
      <el-empty v-else description="该用户暂无公开寻宠启事" />
    </el-card>

    <el-dialog v-model="reportDialogVisible" title="举报用户" width="420px" destroy-on-close>
      <el-form :model="reportForm" label-width="86px">
        <el-form-item label="举报类型">
          <el-select v-model="reportForm.reportType" style="width: 100%">
            <el-option label="垃圾骚扰" value="SPAM" />
            <el-option label="骚扰辱骂" value="HARASSMENT" />
            <el-option label="虚假线索" value="FALSE_CLUE" />
            <el-option label="其他问题" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因摘要">
          <el-input v-model.trim="reportForm.reason" maxlength="128" placeholder="简要说明问题" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model.trim="reportForm.detail" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="可补充细节，管理员会据此处理" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api/user'
import { messageApi } from '@/api/message'
import { useUserStore } from '@/store/user'

const DEFAULT_AVATAR = '/images/default-avatar.png'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const profile = ref(null)
const conversationMeta = ref({ iBlocked: false, blockedMe: false, isBlocked: false, pendingReportCount: 0 })
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportForm = ref({ reportType: 'SPAM', reason: '', detail: '' })
const posts = computed(() => profile.value?.posts || [])
const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const statusMap = { ACTIVE: '寻找中', RESOLVED: '已找到' }

const phoneText = computed(() => {
  if (!profile.value) return ''
  if (profile.value.phoneVisible && profile.value.phone) return profile.value.phone
  if (profile.value.hasPhone) return '双方通过线索建立联系后可查看完整号码'
  return '对方暂未填写联系电话'
})

function roleText(role) {
  return { CERTIFIED: '已认证', PENDING_CERT: '认证审核中', ADMIN: '管理员', USER: '普通用户' }[role] || '普通用户'
}

function roleTagType(role) {
  return { CERTIFIED: 'success', PENDING_CERT: 'warning', ADMIN: 'danger', USER: 'info' }[role] || 'info'
}

function statusText(status) {
  return statusMap[status] || status || '未知'
}

function formatDate(time) {
  if (!time) return ''
  return String(time).slice(0, 10)
}

function firstPhoto(photos) {
  if (!photos) return ''
  return photos.split(',').map(url => url.trim()).filter(Boolean)[0] || ''
}

function openPost(id) {
  router.push(`/posts/${id}`)
}

function requireLogin() {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('请先登录后再操作')
  router.push('/auth/login')
  return false
}

function resetConversationMeta() {
  conversationMeta.value = { iBlocked: false, blockedMe: false, isBlocked: false, pendingReportCount: 0 }
}

async function loadConversationMeta() {
  resetConversationMeta()
  if (!userStore.isLoggedIn || !route.params.id) return
  try {
    const res = await messageApi.meta(route.params.id)
    conversationMeta.value = res.data || conversationMeta.value
  } catch {
    resetConversationMeta()
  }
}

function openReportDialog() {
  if (!requireLogin()) return
  reportForm.value = { reportType: 'SPAM', reason: '', detail: '' }
  reportDialogVisible.value = true
}

async function submitReport() {
  if (!profile.value || !requireLogin()) return
  if (!reportForm.value.reason.trim() && !reportForm.value.detail.trim()) {
    ElMessage.warning('请至少填写原因摘要或补充说明')
    return
  }
  reportSubmitting.value = true
  try {
    await messageApi.report({
      reportedUserId: profile.value.id,
      messageId: null,
      reportType: reportForm.value.reportType,
      reason: reportForm.value.reason,
      detail: reportForm.value.detail,
    })
    ElMessage.success('举报已提交，平台会尽快处理')
    reportDialogVisible.value = false
    await loadConversationMeta()
  } catch { /* ignore */ }
  reportSubmitting.value = false
}

async function handleBlockUser() {
  if (!profile.value || !requireLogin()) return
  try {
    const { value } = await ElMessageBox.prompt('可选填写拉黑原因，拉黑后对方将无法继续向你发送消息。', '拉黑用户', {
      confirmButtonText: '确认拉黑',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：频繁骚扰、虚假线索',
      inputValue: '',
    })
    await messageApi.block({ blockedUserId: profile.value.id, reason: value || '' })
    ElMessage.success('已拉黑，对方将无法继续向你发送消息')
    await Promise.all([loadConversationMeta(), loadProfile()])
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

async function handleUnblockUser() {
  if (!profile.value || !requireLogin()) return
  try {
    await ElMessageBox.confirm('确认取消拉黑？恢复后对方可以再次向你发送消息。', '取消拉黑', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await messageApi.unblock(profile.value.id)
    ElMessage.success('已取消拉黑')
    await Promise.all([loadConversationMeta(), loadProfile()])
  } catch { /* ignore */ }
}

async function loadProfile() {
  const targetId = route.params.id
  if (!targetId) return
  if (String(userStore.userInfo?.id || '') === String(targetId)) {
    router.replace('/profile')
    return
  }
  loading.value = true
  try {
    const res = await userApi.publicProfile(targetId)
    profile.value = res.data
    await loadConversationMeta()
  } catch {
    profile.value = null
    resetConversationMeta()
  }
  loading.value = false
}

watch(() => route.params.id, loadProfile)
onMounted(loadProfile)
</script>

<style scoped>
.public-profile-page { max-width: 980px; margin: 0 auto; }
.back-btn { margin-bottom: 14px; }
.profile-card { margin-bottom: 20px; }
.profile-header { display: flex; align-items: center; gap: 22px; }
.profile-avatar { flex-shrink: 0; background: #409eff; color: #fff; font-size: 30px; }
.profile-info { min-width: 0; display: grid; gap: 6px; }
.profile-actions { margin-left: auto; display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.profile-name-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.profile-name { font-size: 22px; font-weight: 700; color: #303133; overflow-wrap: anywhere; }
.profile-meta { color: #667085; font-size: 14px; }
.contact-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.contact-row strong { display: block; color: #303133; font-size: 15px; margin-bottom: 5px; }
.contact-row p { margin: 0; color: #606266; line-height: 1.7; }
.posts-card :deep(.el-card__body) { min-height: 240px; }
.posts-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.posts-header span { font-weight: 700; color: #303133; }
.posts-header small { color: #98a2b3; }
.post-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.post-item { border: 1px solid #edf1f5; border-radius: 8px; overflow: hidden; cursor: pointer; background: #fff; transition: transform 0.18s ease, box-shadow 0.18s ease; }
.post-item:hover { transform: translateY(-2px); box-shadow: var(--shadow-card); }
.post-cover { position: relative; height: 160px; background: #f5f7fa; overflow: hidden; }
.post-cover :deep(.el-image) { width: 100%; height: 100%; display: block; }
.post-cover-placeholder { height: 100%; display: flex; align-items: center; justify-content: center; color: #98a2b3; background: linear-gradient(135deg, #eef7ff, #fff8ed); }
.post-status { position: absolute; right: 10px; top: 10px; padding: 4px 9px; border-radius: 999px; font-size: 12px; font-weight: 700; background: #e6f7ff; color: #1890ff; box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12); }
.post-status.RESOLVED { background: #f6ffed; color: #52c41a; }
.post-body { padding: 13px 14px 15px; display: grid; gap: 7px; }
.post-body h3 { margin: 0; color: #303133; font-size: 16px; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.post-body p { margin: 0; color: #606266; font-size: 13px; line-height: 1.6; min-height: 40px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.post-body small { color: #98a2b3; font-size: 12px; }

@media (max-width: 900px) {
  .post-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
  .profile-header { align-items: flex-start; gap: 16px; flex-wrap: wrap; }
  .profile-avatar { width: 64px !important; height: 64px !important; }
  .profile-info { flex: 1; }
  .profile-actions { width: 100%; margin-left: 0; justify-content: flex-start; }
  .profile-actions :deep(.el-button) { flex: 1; min-width: 110px; margin-left: 0; }
  .contact-row { align-items: flex-start; display: grid; }
  .post-grid { grid-template-columns: 1fr; }
  .post-cover { height: 210px; }
}
</style>
