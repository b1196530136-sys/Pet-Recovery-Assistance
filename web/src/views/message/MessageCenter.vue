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
              <div class="msg-bubble">
                <p v-if="msg.msgType === 1" style="color: #e6a23c; font-weight: bold">📌 目击线索</p>
                <p>{{ msg.content }}</p>
                <div v-if="msg.cluePhotos" class="clue-photos">
                  <el-image
                    v-for="(url, idx) in msg.cluePhotos.split(',')"
                    :key="idx"
                    :src="url"
                    :preview-src-list="msg.cluePhotos.split(',')"
                    :initial-index="idx"
                    style="width: 100px; height: 100px; border-radius: 4px;"
                    fit="cover"
                    preview-teleported
                  />
                </div>
                <p v-if="msg.clueAddress" style="font-size: 12px; color: #909399; margin-top: 4px">
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
.clue-photos { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 6px; }
</style>
