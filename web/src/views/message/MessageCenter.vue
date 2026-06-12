<template>
  <div class="message-page">
    <h2 class="page-title">我的消息</h2>

    <el-tabs v-model="activeTab" class="message-tabs">
      <el-tab-pane name="chat" label="会话消息">
        <el-row :gutter="20">
          <el-col :xs="24" :md="8">
            <el-card class="conversation-card">
              <template #header>
                <span>会话列表</span>
              </template>
              <div v-if="convList.length">
                <div v-for="conv in convList" :key="conv.otherId"
                     class="conv-item"
                     :class="{ active: currentOtherId === conv.otherId }"
                     @click="selectConvByUser(conv.otherId)">
                  <el-avatar
                    :size="36"
                    :src="getUserAvatar(conv.otherId)"
                    class="conv-avatar clickable-avatar"
                    @click.stop="openUserProfile(conv.otherId)"
                  >
                  </el-avatar>
                  <div style="flex:1; min-width:0">
                    <div style="font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                      {{ getUserName(conv.otherId) }}
                    </div>
                    <div style="font-size: 12px; color: #909399; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                      {{ conv.lastContent }}
                    </div>
                  </div>
                  <el-badge :value="conv.unread" :hidden="!conv.unread" style="margin-left: 8px; flex-shrink: 0;" />
                </div>
              </div>
              <el-empty v-else description="暂无消息" />
            </el-card>
          </el-col>

          <el-col :xs="24" :md="16">
            <el-card class="message-card">
              <template #header>
                <div class="message-card-header">
                  <span>消息详情</span>
                  <el-dropdown v-if="currentOtherId" trigger="click">
                    <el-button text class="chat-tools-btn">会话管理</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-if="!conversationMeta.iBlocked"
                          @click="handleBlockUser"
                        >
                          拉黑对方
                        </el-dropdown-item>
                        <el-dropdown-item
                          v-else
                          @click="handleUnblockUser"
                        >
                          取消拉黑
                        </el-dropdown-item>
                        <el-dropdown-item @click="openReportDialog()">举报对方</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
              <div v-if="conversationMeta.isBlocked" class="safety-banner" :class="{ danger: conversationMeta.blockedMe }">
                <strong>{{ conversationMeta.blockedMe ? '对方已拒收你的消息' : '你已拉黑对方' }}</strong>
                <span>{{ conversationMeta.blockedMe ? '你仍可查看历史记录，但无法继续发送消息。' : '你仍可查看历史记录；如需恢复沟通，可取消拉黑。' }}</span>
              </div>
              <div v-if="contactInfo?.hasClueRelation" class="contact-banner">
                <div>
                  <strong>线索联系号码</strong>
                  <span v-if="contactInfo.otherPhone">对方电话：{{ contactInfo.otherPhone }}</span>
                  <span v-else>对方暂未填写联系电话</span>
                  <small v-if="!contactInfo.myHasPhone">你还未填写联系电话，可在个人中心补充后让对方查看。</small>
                </div>
              </div>
              <!-- 领养申请操作栏 -->
              <div v-if="pendingAdoption" class="adopt-banner">
                <div class="adopt-info">
                  <strong>{{ pendingAdoption.applicantName }} 想领养您的「{{ typeMap[pendingAdoption.animalType] || pendingAdoption.animalType }}」</strong>
                  <div class="adopt-fields">
                    <span>联系方式：{{ pendingAdoption.contact || '未填写' }}</span>
                    <span>居住条件：{{ pendingAdoption.livingCondition || '未填写' }}</span>
                    <span>养宠经验：{{ pendingAdoption.petExperience || '未填写' }}</span>
                    <span v-if="pendingAdoption.message">留言：{{ pendingAdoption.message }}</span>
                  </div>
                </div>
                <div class="adopt-actions">
                  <el-button size="small" type="success" @click="handleAdoptReview('APPROVED')">接受</el-button>
                  <el-button size="small" type="danger" @click="handleAdoptReview('REJECTED')">拒绝</el-button>
                </div>
              </div>
              <div v-if="messages.length" class="msg-list">
                <div v-for="msg in messages" :key="msg.id"
                     class="msg-item"
                     :class="{ mine: msg.senderId === userId }">
                  <el-avatar
                    v-if="msg.senderId !== userId"
                    :size="36"
                    :src="getUserAvatar(msg.senderId)"
                    class="msg-avatar clickable-avatar"
                    @click.stop="openUserProfile(msg.senderId)"
                  />
                  <div class="msg-content-wrap">
                    <div v-if="msg.msgType === 1" class="clue-card" @click="showClueDetail(msg)">
                      <div class="clue-card-header">
                        <span class="clue-card-title">目击线索</span>
                      </div>
                      <p v-if="msg.content" class="clue-card-desc">{{ msg.content }}</p>
                      <div v-if="msg.cluePhotos && msg.cluePhotos.trim()" class="clue-card-photo">
                        <el-image
                          :src="parsePhotos(msg.cluePhotos)[0]"
                          :preview-src-list="parsePhotos(msg.cluePhotos)"
                          style="width: 100%; height: 140px; border-radius: 6px;"
                          fit="cover"
                          preview-teleported
                        >
                          <template #error>
                            <div style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;background:#f5f7fa;color:#c0c4cc;font-size:12px">加载失败</div>
                          </template>
                        </el-image>
                      </div>
                      <p v-if="msg.clueAddress" class="clue-card-location">
                        {{ msg.clueTime }} · {{ msg.clueAddress }}
                      </p>
                      <small class="clue-card-time">{{ msg.createTime }}</small>
                    </div>
                    <div v-else
                         class="msg-bubble"
                         @click="msg.msgType === 1 && showClueDetail(msg)">
                      <p v-if="msg.content" style="margin-bottom: 8px">{{ msg.content }}</p>
                      <small style="color: #c0c4cc; font-size: 11px">{{ msg.createTime }}</small>
                    </div>
                    <div v-if="msg.senderId !== userId" class="msg-actions">
                      <el-button text size="small" @click.stop="openReportDialog(msg)">举报</el-button>
                    </div>
                  </div>
                  <el-avatar
                    v-if="msg.senderId === userId"
                    :size="36"
                    :src="getUserAvatar(msg.senderId)"
                    class="msg-avatar clickable-avatar"
                    @click.stop="openUserProfile(msg.senderId)"
                  />
                </div>
              </div>
              <el-empty v-else description="选择会话查看消息" />

              <!-- 发送消息输入框 -->
              <div v-if="currentOtherId" class="msg-input-bar">
                <el-input
                  v-model="inputText"
                  :placeholder="conversationMeta.isBlocked ? '当前会话不可继续发送消息' : '输入消息...'"
                  @keyup.enter="sendNormalMessage"
                  clearable
                  :disabled="conversationMeta.isBlocked"
                />
                <el-button type="primary" @click="sendNormalMessage" :disabled="conversationMeta.isBlocked || !inputText.trim()">发送</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
      <el-tab-pane name="system">
        <template #label>
          <el-badge :value="systemUnreadCount" :hidden="systemUnreadCount === 0" :max="99" class="tab-badge">
            <span>系统消息</span>
          </el-badge>
        </template>
        <el-card class="system-card">
          <div v-if="systemMessages.length" class="system-list">
            <div v-for="msg in systemMessages" :key="msg.id" class="system-item" :class="[msg.resultType, { unread: msg.readStatus === 0 }]">
              <div class="system-item-main">
                <div class="system-item-tags">
                  <el-tag :type="msg.resultType === 'APPROVED' ? 'success' : 'danger'" size="small">
                    {{ msg.resultType === 'APPROVED' ? '审核通过' : '审核失败' }}
                  </el-tag>
                  <el-tag v-if="msg.readStatus === 0" type="warning" size="small">未读</el-tag>
                </div>
                <h3>{{ msg.title || '系统消息' }}</h3>
                <p>{{ msg.body }}</p>
                <small>{{ msg.createTime }}</small>
              </div>
              <el-button v-if="canEditFromSystemMessage(msg)" type="primary" @click="openSystemTarget(msg)">
                {{ systemActionLabel(msg) }}
              </el-button>
            </div>
          </div>
          <el-empty v-else description="暂无系统消息" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 线索详情弹窗 -->
    <el-dialog v-model="clueDialogVisible" title="📌 目击线索详情" width="560px" destroy-on-close>
      <div v-if="clueDetail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="目击描述">
            {{ clueDetail.content || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="目击时间">
            {{ clueDetail.clueTime || '未提供' }}
          </el-descriptions-item>
          <el-descriptions-item label="目击地点">
            {{ clueDetail.clueAddress || '未提供' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ clueDetail.createTime }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 现场照片 -->
        <div style="margin-top: 20px">
          <h4 style="margin-bottom: 10px; color: #303133">现场照片</h4>
          <template v-if="clueDetail.cluePhotos && clueDetail.cluePhotos.trim()">
            <div class="detail-photos">
              <el-image
                v-for="(url, idx) in parsePhotos(clueDetail.cluePhotos)"
                :key="idx"
                :src="url"
                :preview-src-list="parsePhotos(clueDetail.cluePhotos)"
                :initial-index="idx"
                style="width: 180px; height: 180px; border-radius: 8px;"
                fit="cover"
                preview-teleported
              >
                <template #error>
                  <div class="image-error-detail">加载失败</div>
                </template>
              </el-image>
            </div>
          </template>
          <p v-else style="color: #909399; font-size: 13px">暂无现场照片</p>
        </div>

        <!-- 地图定位 -->
        <div v-if="clueDetail.clueLongitude && clueDetail.clueLatitude" ref="clueMapContainer" style="width: 100%; height: 280px; margin-top: 20px; border-radius: 8px; overflow: hidden"></div>
      </div>
    </el-dialog>

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
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { messageApi } from '@/api/message'
import { userApi } from '@/api/user'
import { adoptionApi } from '@/api/adoption'
import { useUserStore } from '@/store/user'
import { onMessage } from '@/utils/websocket'
import { loadAmap } from '@/utils/amap'

const userStore = useUserStore()
const router = useRouter()
const userId = computed(() => userStore.userInfo?.id)
const activeTab = ref('chat')
const convList = ref([])
const messages = ref([])
const systemMessages = ref([])
const systemUnreadCount = computed(() => systemMessages.value.filter(msg => msg.readStatus === 0).length)
const currentOtherId = ref(null)
const userCache = ref({})
const conversationMeta = ref({ iBlocked: false, blockedMe: false, isBlocked: false, pendingReportCount: 0 })

// 线索详情弹窗
const clueDialogVisible = ref(false)
const clueDetail = ref(null)
const clueMapContainer = ref(null)

// 领养申请
const pendingAdoption = ref(null)
const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const contactInfo = ref(null)
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportForm = ref({ reportType: 'SPAM', reason: '', detail: '', messageId: null })

// 普通消息输入
const inputText = ref('')

async function getUserInfo(id) {
  if (userCache.value[id]) return userCache.value[id]
  try {
    const res = await userApi.getUserInfo(id)
    if (res.data) {
      userCache.value[id] = res.data
      return res.data
    }
  } catch { /* ignore */ }
  return null
}

function getUserName(id) {
  return userCache.value[id]?.nickname || '用户' + id
}

const DEFAULT_AVATAR = '/images/default-avatar.png'

function getUserAvatar(id) {
  if (id === userId.value) return userStore.userInfo?.avatar || DEFAULT_AVATAR
  return userCache.value[id]?.avatar || DEFAULT_AVATAR
}

function openUserProfile(id) {
  if (!id) return
  if (String(userId.value || '') === String(id)) {
    router.push('/profile')
    return
  }
  router.push(`/users/${id}`)
}

function parsePhotos(photos) {
  if (!photos || !photos.trim()) return []
  return photos.split(',').map(url => url.trim()).filter(Boolean)
}

function showClueDetail(msg) {
  clueDetail.value = msg
  clueDialogVisible.value = true
  // 弹窗打开后渲染地图
  nextTick(() => {
    if (msg.clueLongitude && msg.clueLatitude && clueMapContainer.value) {
      if (!window.AMap) {
        loadAmap().then(() => { renderClueMap(msg) }).catch((error) => {
          ElMessage.error(error.message || '地图加载失败')
        })
      } else {
        renderClueMap(msg)
      }
    }
  })
}

function renderClueMap(msg) {
  const container = clueMapContainer.value
  if (!container || !window.AMap) return
  const map = new AMap.Map(container, {
    zoom: 16,
    center: [parseFloat(msg.clueLongitude), parseFloat(msg.clueLatitude)]
  })
  new AMap.Marker({
    position: [parseFloat(msg.clueLongitude), parseFloat(msg.clueLatitude)],
    map,
    label: { content: '<div style="padding:2px 6px;background:#e6a23c;color:#fff;border-radius:4px;font-size:12px">目击位置</div>', direction: 'top' }
  })
}

async function loadConversations() {
  const res = await messageApi.conversations()
  convList.value = res.data || []
  const ids = new Set()
  for (const conv of convList.value) {
    ids.add(conv.otherId)
  }
  await Promise.all([...ids].map(id => getUserInfo(id)))
}

async function selectConvByUser(otherId) {
  currentOtherId.value = otherId
  contactInfo.value = null
  conversationMeta.value = { iBlocked: false, blockedMe: false, isBlocked: false, pendingReportCount: 0 }
  const res = await messageApi.conversation(otherId)
  messages.value = res.data
  await getUserInfo(otherId)
  // 立即清除当前会话的本地未读标记（UI 立即更新）
  const conv = convList.value.find(c => c.otherId === otherId)
  if (conv && conv.unread > 0) {
    conv.unread = 0
  }
  // 将该用户的所有未读消息标记为已读（后端）
  const unreadRes = await messageApi.unread()
  for (const msg of (unreadRes.data || [])) {
    const oid = msg.senderId === userId.value ? msg.receiverId : msg.senderId
    if (oid === otherId) {
      try { await messageApi.markRead(msg.id) } catch { /* ignore */ }
    }
  }
  loadConversations()
  userStore.fetchUnread()
  scrollToBottom()
  // 检查是否有该用户的待处理领养申请
  checkPendingAdoption(otherId)
  loadContactInfo(otherId)
  loadConversationMeta(otherId)
}

async function loadSystemMessages() {
  try {
    const res = await messageApi.systemMessages()
    systemMessages.value = res.data || []
    if (activeTab.value === 'system') {
      markSystemMessagesRead()
    }
  } catch {
    systemMessages.value = []
  }
}

function canEditFromSystemMessage(msg) {
  return msg.resultType === 'REJECTED' && ['post', 'archive', 'certification'].includes(msg.targetType) && msg.targetId
}

function systemActionLabel(msg) {
  return msg.targetType === 'certification' ? '重新提交认证' : '修改并重新提交'
}

function openSystemTarget(msg) {
  if (msg.targetType === 'post') {
    router.push({ path: '/posts/create', query: { id: msg.targetId } })
    return
  }
  if (msg.targetType === 'archive') {
    router.push({ path: '/archives/create', query: { id: msg.targetId } })
    return
  }
  if (msg.targetType === 'certification') {
    router.push('/profile')
  }
}

async function markSystemMessagesRead() {
  const unread = systemMessages.value.filter(msg => msg.readStatus === 0)
  if (!unread.length) return
  await Promise.all(unread.map(msg => messageApi.markRead(msg.id).catch(() => null)))
  unread.forEach(msg => { msg.readStatus = 1 })
  userStore.fetchUnread()
}

async function sendNormalMessage() {
  const text = inputText.value.trim()
  if (!text || !currentOtherId.value || conversationMeta.value.isBlocked) return
  try {
    const res = await messageApi.send({
      receiverId: currentOtherId.value,
      content: text,
      msgType: 0
    })
    inputText.value = ''
    // 将发送的消息直接添加到列表（避免全量刷新）
    if (res.data) {
      const exists = messages.value.some(m => m.id === res.data.id)
      if (!exists) {
        messages.value.push(res.data)
      }
    }
    loadConversations()
    scrollToBottom()
  } catch {
    // ignore
  }
}

async function checkPendingAdoption(otherId) {
  pendingAdoption.value = null
  if (!otherId) return
  try {
    const res = await adoptionApi.incomingFrom(otherId)
    pendingAdoption.value = res.data || null
  } catch { /* ignore */ }
}

async function loadContactInfo(otherId) {
  try {
    const res = await messageApi.contact(otherId)
    contactInfo.value = res.data || null
  } catch {
    contactInfo.value = null
  }
}

async function loadConversationMeta(otherId) {
  try {
    const res = await messageApi.meta(otherId)
    conversationMeta.value = res.data || { iBlocked: false, blockedMe: false, isBlocked: false, pendingReportCount: 0 }
  } catch {
    conversationMeta.value = { iBlocked: false, blockedMe: false, isBlocked: false, pendingReportCount: 0 }
  }
}

function openReportDialog(msg = null) {
  reportForm.value = {
    reportType: msg?.msgType === 1 ? 'FALSE_CLUE' : 'SPAM',
    reason: '',
    detail: '',
    messageId: msg?.id || null,
  }
  reportDialogVisible.value = true
}

async function submitReport() {
  if (!currentOtherId.value) return
  if (!reportForm.value.reason.trim() && !reportForm.value.detail.trim()) {
    ElMessage.warning('请至少填写原因摘要或补充说明')
    return
  }
  reportSubmitting.value = true
  try {
    await messageApi.report({
      reportedUserId: currentOtherId.value,
      messageId: reportForm.value.messageId,
      reportType: reportForm.value.reportType,
      reason: reportForm.value.reason,
      detail: reportForm.value.detail,
    })
    ElMessage.success('举报已提交，平台会尽快处理')
    reportDialogVisible.value = false
    loadConversationMeta(currentOtherId.value)
  } catch { /* ignore */ }
  reportSubmitting.value = false
}

async function handleBlockUser() {
  if (!currentOtherId.value) return
  try {
    const { value } = await ElMessageBox.prompt('可选填写拉黑原因，方便你自己后续查看。', '拉黑用户', {
      confirmButtonText: '确认拉黑',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：频繁骚扰、虚假线索',
      inputValue: '',
    })
    await messageApi.block({ blockedUserId: currentOtherId.value, reason: value || '' })
    ElMessage.success('已拉黑，对方将无法继续向你发送消息')
    loadConversationMeta(currentOtherId.value)
    loadContactInfo(currentOtherId.value)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

async function handleUnblockUser() {
  if (!currentOtherId.value) return
  try {
    await ElMessageBox.confirm('确认取消拉黑？恢复后对方可以再次向你发送消息。', '取消拉黑', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await messageApi.unblock(currentOtherId.value)
    ElMessage.success('已取消拉黑')
    loadConversationMeta(currentOtherId.value)
    loadContactInfo(currentOtherId.value)
  } catch { /* ignore */ }
}

async function handleAdoptReview(action) {
  if (!pendingAdoption.value) return
  const label = action === 'APPROVED' ? '接受' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${label}该领养申请吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  } catch { return }
  try {
    await adoptionApi.review(pendingAdoption.value.id, action)
    ElMessage.success(`已${label}`)
  } catch { /* ignore */ }
  // 无论成功失败都重新检查，避免状态不同步
  pendingAdoption.value = null
  loadConversations()
  checkPendingAdoption(currentOtherId.value)
}

function scrollToBottom() {
  nextTick(() => {
    const list = document.querySelector('.msg-list')
    if (list) list.scrollTop = list.scrollHeight
  })
}

onMounted(async () => {
  await Promise.all([loadConversations(), loadSystemMessages()])
  // 确保 WebSocket 已连接
  import('@/utils/websocket').then(({ initWebSocket }) => {
    if (userStore.token) initWebSocket(userStore.token)
  })
  // 轮询兜底：每 5 秒检查新消息
  pollTimer = setInterval(async () => {
    if (currentOtherId.value) {
      try {
        const res = await messageApi.conversation(currentOtherId.value)
        const newMsgs = res.data || []
        // 只追加新消息
        const existIds = new Set(messages.value.map(m => m.id))
        let hasNew = false
        for (const m of newMsgs) {
          if (!existIds.has(m.id)) {
            messages.value.push(m)
            hasNew = true
          }
        }
        if (hasNew) scrollToBottom()
      } catch { /* ignore */ }
    }
    loadConversations()
    loadSystemMessages()
  }, 5000)
})

watch(activeTab, (tab) => {
  if (tab === 'system') {
    markSystemMessagesRead()
  }
})

let pollTimer = null

// 监听 WebSocket 新消息推送，实时更新
const wsUnsubscribe = onMessage((msg) => {
  // 只处理与当前用户相关的消息
  if (msg.receiverId !== userId.value && msg.senderId !== userId.value) return

  // 刷新会话列表
  if (msg.msgType === 2) {
    loadSystemMessages()
    userStore.fetchUnread()
    return
  }
  loadConversations()

  // 如果消息属于当前打开的会话，添加到消息列表
  if (currentOtherId.value &&
      (msg.senderId === currentOtherId.value || msg.receiverId === currentOtherId.value)) {
    // 避免重复：sendNormalMessage 已通过 API 响应刷新了消息列表
    const exists = messages.value.some(m => m.id === msg.id)
    if (!exists) {
      messages.value.push(msg)
      scrollToBottom()
    }
  }
})

onUnmounted(() => {
  wsUnsubscribe()
  clearInterval(pollTimer)
})
</script>

<style scoped>
.message-page { max-width: 1100px; margin: 0 auto; }
.message-tabs :deep(.el-tabs__content) { overflow: visible; }
.conversation-card,
.message-card { height: 100%; }
.message-card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.chat-tools-btn { padding: 0; color: #606266; }
.conversation-card :deep(.el-card__body),
.message-card :deep(.el-card__body) { min-height: 340px; }
.conv-item { padding: 12px; cursor: pointer; display: flex; align-items: center; border-bottom: 1px solid #f0f0f0; }
.conv-item .el-avatar { flex-shrink: 0; }
.conv-avatar { margin-right: 10px; }
.clickable-avatar { cursor: pointer; transition: box-shadow 0.18s ease, transform 0.18s ease; }
.clickable-avatar:hover { box-shadow: 0 0 0 3px #d9ecff; transform: translateY(-1px); }
.conv-item:hover, .conv-item.active { background: #ecf5ff; }
.msg-list { max-height: 500px; overflow-y: auto; }
.msg-item { display: flex; align-items: flex-start; margin-bottom: 16px; gap: 8px; }
.msg-item.mine { justify-content: flex-end; }
.msg-content-wrap { display: grid; gap: 4px; }
.msg-avatar { flex-shrink: 0; margin-top: 4px; }
.msg-bubble { max-width: 70%; padding: 10px 14px; border-radius: 8px; background: #f0f0f0; }
.msg-actions { display: flex; justify-content: flex-end; }
.msg-item.mine .msg-bubble { background: #409eff; color: #fff; }
.msg-item.mine .msg-bubble small { color: rgba(255,255,255,0.7); }
.msg-item.mine .clue-bubble { background: #e6a23c; }
.clue-photos { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; padding: 4px; background: #fafafa; border-radius: 6px; }
.clue-card { max-width: 280px; padding: 12px; border-radius: 10px; background: #fff; border: 1px solid #e8e8e8; cursor: pointer; transition: box-shadow 0.2s, transform 0.1s; box-sizing: border-box; }
.clue-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.12); transform: translateY(-1px); }
.clue-card-header { margin-bottom: 8px; }
.clue-card-title { color: #e6a23c; font-weight: bold; font-size: 14px; }
.clue-card-desc { color: #303133; font-size: 13px; margin-bottom: 8px; line-height: 1.5; word-break: break-all; }
.clue-card-photo { margin-bottom: 8px; border-radius: 6px; overflow: hidden; background: #f5f7fa; }
.clue-card-location { color: #909399; font-size: 12px; margin-top: 6px; line-height: 1.4; }
.clue-card-time { color: #c0c4cc; font-size: 11px; display: block; margin-top: 6px; }
.image-error { width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; color: #c0c4cc; font-size: 12px; }
.detail-photos { display: flex; gap: 10px; flex-wrap: wrap; padding: 8px; background: #f9f9f9; border-radius: 8px; }
.image-error-detail { width: 180px; height: 180px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; color: #c0c4cc; font-size: 13px; }
.msg-input-bar { display: flex; gap: 10px; margin-top: 16px; padding-top: 12px; border-top: 1px solid #ebeef5; align-items: center; }
.msg-input-bar .el-input { flex: 1; }
.tab-badge { line-height: 1; }
.tab-badge :deep(.el-badge__content) { top: 2px; right: -10px; }
.safety-banner { padding: 12px 16px; margin-bottom: 12px; background: #fff7e6; border: 1px solid #ffd591; border-radius: 8px; color: #8a4b00; display: grid; gap: 4px; }
.safety-banner.danger { background: #fff1f0; border-color: #ffccc7; color: #a8071a; }
.safety-banner strong { font-size: 14px; }
.safety-banner span { font-size: 13px; line-height: 1.6; }
.contact-banner { padding: 12px 16px; margin-bottom: 12px; background: #ecf5ff; border: 1px solid #b3d8ff; border-radius: 8px; color: #303133; }
.contact-banner div { display: grid; gap: 4px; }
.contact-banner strong { color: #1d4ed8; font-size: 14px; }
.contact-banner span { font-size: 14px; }
.contact-banner small { color: #667085; font-size: 12px; }
.adopt-banner { display: flex; align-items: flex-start; gap: 12px; padding: 12px 16px; margin-bottom: 12px; background: #fff7e6; border: 1px solid #ffd591; border-radius: 8px; font-size: 14px; color: #303133; }
.adopt-info { flex: 1; display: grid; gap: 8px; min-width: 0; }
.adopt-info strong { color: #8a4b00; }
.adopt-fields { display: grid; gap: 4px; color: #606266; line-height: 1.6; }
.adopt-fields span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.adopt-actions { display: flex; flex-shrink: 0; gap: 8px; }
.system-card :deep(.el-card__body) { padding: 10px 0; }
.system-list { display: grid; }
.system-item { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px 22px; border-bottom: 1px solid #edf1f5; }
.system-item:last-child { border-bottom: 0; }
.system-item.unread { box-shadow: inset 3px 0 0 #e6a23c; }
.system-item-main { display: grid; gap: 8px; min-width: 0; }
.system-item-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.system-item-main h3 { font-size: 16px; color: #303133; margin: 0; }
.system-item-main p { color: #606266; margin: 0; line-height: 1.7; }
.system-item-main small { color: #98a2b3; font-size: 12px; }
.system-item.REJECTED { background: #fffafa; }
.system-item.APPROVED { background: #fbfffb; }

@media (max-width: 768px) {
  .conversation-card :deep(.el-card__body),
  .message-card :deep(.el-card__body) { min-height: 220px; }
  .msg-list { max-height: 420px; }
  .msg-input-bar { position: sticky; bottom: 0; background: #fff; }
  .msg-bubble,
  .clue-card { max-width: 82%; }
  .adopt-banner { display: grid; }
  .adopt-actions,
  .adopt-actions :deep(.el-button) { width: 100%; }
  .adopt-actions { display: grid; grid-template-columns: 1fr 1fr; }
  .system-item { align-items: flex-start; display: grid; }
  .system-item :deep(.el-button) { width: 100%; }
}
</style>
