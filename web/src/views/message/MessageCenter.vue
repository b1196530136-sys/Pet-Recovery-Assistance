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
          <div v-if="messages.length" class="msg-list">
            <div v-for="msg in messages" :key="msg.id"
                 class="msg-item"
                 :class="{ mine: msg.senderId === userId }">
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
            </div>
          </div>
          <el-empty v-else description="选择会话查看消息" />
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
import { messageApi } from '@/api/message'
import { userApi } from '@/api/user'
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
  // 将该用户的所有未读消息标记为已读
  for (const conv of convList.value) {
    if (conv.otherId === otherId && conv.unread > 0) {
      // 获取该用户的未读消息 ID 逐个标记
      const unreadRes = await messageApi.unread()
      for (const msg of (unreadRes.data || [])) {
        const oid = msg.senderId === userId.value ? msg.receiverId : msg.senderId
        if (oid === otherId) {
          await messageApi.markRead(msg.id)
        }
      }
      break
    }
  }
  loadConversations()
}

onMounted(async () => {
  await loadConversations()
})

// 监听 WebSocket 新消息推送，实时更新
const wsUnsubscribe = onMessage((msg) => {
  if (msg.receiverId === userId.value || msg.senderId === userId.value) {
    loadConversations()
    if (currentOtherId.value &&
        (msg.senderId === currentOtherId.value || msg.receiverId === currentOtherId.value)) {
      messages.value.push(msg)
    }
  }
})

onUnmounted(wsUnsubscribe)
</script>

<style scoped>
.conv-item { padding: 12px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f0f0f0; }
.conv-item:hover, .conv-item.active { background: #ecf5ff; }
.msg-list { max-height: 500px; overflow-y: auto; }
.msg-item { display: flex; margin-bottom: 16px; }
.msg-item.mine { justify-content: flex-end; }
.msg-bubble { max-width: 70%; padding: 10px 14px; border-radius: 8px; background: #f0f0f0; }
.msg-item.mine .msg-bubble { background: #ecf5ff; }
.clue-photos { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; padding: 4px; background: #fafafa; border-radius: 6px; }
.clue-bubble { cursor: pointer; transition: box-shadow 0.2s, transform 0.1s; }
.clue-bubble:hover { box-shadow: 0 2px 12px rgba(230, 162, 60, 0.25); transform: translateY(-1px); }
.image-error { width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; color: #c0c4cc; font-size: 12px; }
.detail-photos { display: flex; gap: 10px; flex-wrap: wrap; padding: 8px; background: #f9f9f9; border-radius: 8px; }
.image-error-detail { width: 180px; height: 180px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; color: #c0c4cc; font-size: 13px; }
</style>
