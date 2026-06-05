<template>
  <div class="message-page">
    <h2 style="margin-bottom: 20px">我的消息</h2>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>会话列表</span>
          </template>
          <div v-if="convList.length">
            <div v-for="conv in convList" :key="conv.otherId"
                 class="conv-item"
                 :class="{ active: currentOtherId === conv.otherId }"
                 @click="selectConvByUser(conv.otherId)">
              <el-avatar :size="36" :src="getUserAvatar(conv.otherId)" style="flex-shrink: 0; margin-right: 10px;">
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

      <el-col :span="16">
        <el-card>
          <template #header>
            <span>消息详情</span>
          </template>
          <!-- 领养申请操作栏 -->
          <div v-if="pendingAdoption" class="adopt-banner">
            <span>{{ pendingAdoption.applicantName }} 想领养您的「{{ typeMap[pendingAdoption.animalType] || pendingAdoption.animalType }}」</span>
            <el-button size="small" type="success" @click="handleAdoptReview('APPROVED')">接受</el-button>
            <el-button size="small" type="danger" @click="handleAdoptReview('REJECTED')">拒绝</el-button>
          </div>
          <div v-if="messages.length" class="msg-list">
            <div v-for="msg in messages" :key="msg.id"
                 class="msg-item"
                 :class="{ mine: msg.senderId === userId }">
              <el-avatar v-if="msg.senderId !== userId" :size="36" :src="getUserAvatar(msg.senderId)" class="msg-avatar" />
              <div class="msg-bubble"
                   :class="{ 'clue-bubble': msg.msgType === 1 }"
                   @click="msg.msgType === 1 && showClueDetail(msg)">
                <p v-if="msg.msgType === 1" style="color: #e6a23c; font-weight: bold; margin-bottom: 8px">📌 目击线索</p>
                <p v-if="msg.content" style="margin-bottom: 8px">{{ msg.content }}</p>
                <div v-if="msg.cluePhotos && msg.cluePhotos.trim()" class="clue-photos">
                  <el-image
                    v-for="(url, idx) in parsePhotos(msg.cluePhotos)"
                    :key="idx"
                    :src="url"
                    :preview-src-list="parsePhotos(msg.cluePhotos)"
                    :initial-index="idx"
                    style="width: 120px; height: 120px; border-radius: 6px;"
                    fit="cover"
                    preview-teleported
                  >
                    <template #error>
                      <div class="image-error">加载失败</div>
                    </template>
                  </el-image>
                </div>
                <p v-if="msg.clueAddress" style="font-size: 12px; color: #909399; margin-top: 6px">
                  📍 {{ msg.clueTime }} · {{ msg.clueAddress }}
                </p>
                <small style="color: #c0c4cc; font-size: 11px">{{ msg.createTime }}</small>
              </div>
              <el-avatar v-if="msg.senderId === userId" :size="36" :src="getUserAvatar(msg.senderId)" class="msg-avatar" />
            </div>
          </div>
          <el-empty v-else description="选择会话查看消息" />

          <!-- 发送消息输入框 -->
          <div v-if="currentOtherId" class="msg-input-bar">
            <el-input
              v-model="inputText"
              placeholder="输入消息..."
              @keyup.enter="sendNormalMessage"
              clearable
            />
            <el-button type="primary" @click="sendNormalMessage" :disabled="!inputText.trim()">发送</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { messageApi } from '@/api/message'
import { userApi } from '@/api/user'
import { adoptionApi } from '@/api/adoption'
import { useUserStore } from '@/store/user'
import { onMessage } from '@/utils/websocket'

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.id)
const convList = ref([])
const messages = ref([])
const currentOtherId = ref(null)
const userCache = ref({})

// 线索详情弹窗
const clueDialogVisible = ref(false)
const clueDetail = ref(null)
const clueMapContainer = ref(null)

// 领养申请
const pendingAdoption = ref(null)
const typeMap = { cat: '猫', dog: '狗', other: '其他' }

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
      renderClueMap(msg)
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
}

async function sendNormalMessage() {
  const text = inputText.value.trim()
  if (!text || !currentOtherId.value) return
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
  await loadConversations()
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
  }, 5000)
})

let pollTimer = null

// 监听 WebSocket 新消息推送，实时更新
const wsUnsubscribe = onMessage((msg) => {
  // 只处理与当前用户相关的消息
  if (msg.receiverId !== userId.value && msg.senderId !== userId.value) return

  // 刷新会话列表
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
.conv-item { padding: 12px; cursor: pointer; display: flex; align-items: center; border-bottom: 1px solid #f0f0f0; }
.conv-item .el-avatar { flex-shrink: 0; }
.conv-item:hover, .conv-item.active { background: #ecf5ff; }
.msg-list { max-height: 500px; overflow-y: auto; }
.msg-item { display: flex; align-items: flex-start; margin-bottom: 16px; gap: 8px; }
.msg-item.mine { justify-content: flex-end; }
.msg-avatar { flex-shrink: 0; margin-top: 4px; }
.msg-bubble { max-width: 70%; padding: 10px 14px; border-radius: 8px; background: #f0f0f0; }
.msg-item.mine .msg-bubble { background: #409eff; color: #fff; }
.msg-item.mine .msg-bubble small { color: rgba(255,255,255,0.7); }
.msg-item.mine .clue-bubble { background: #e6a23c; }
.clue-photos { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; padding: 4px; background: #fafafa; border-radius: 6px; }
.clue-bubble { cursor: pointer; transition: box-shadow 0.2s, transform 0.1s; }
.clue-bubble:hover { box-shadow: 0 2px 12px rgba(230, 162, 60, 0.25); transform: translateY(-1px); }
.image-error { width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; color: #c0c4cc; font-size: 12px; }
.detail-photos { display: flex; gap: 10px; flex-wrap: wrap; padding: 8px; background: #f9f9f9; border-radius: 8px; }
.image-error-detail { width: 180px; height: 180px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; color: #c0c4cc; font-size: 13px; }
.msg-input-bar { display: flex; gap: 10px; margin-top: 16px; padding-top: 12px; border-top: 1px solid #ebeef5; align-items: center; }
.msg-input-bar .el-input { flex: 1; }
.adopt-banner { display: flex; align-items: center; gap: 12px; padding: 12px 16px; margin-bottom: 12px; background: #fff7e6; border: 1px solid #ffd591; border-radius: 8px; font-size: 14px; color: #303133; }
.adopt-banner span { flex: 1; }
</style>
