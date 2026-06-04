<template>
  <div class="message-page">
    <h2 style="margin-bottom: 20px">我的消息</h2>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>会话列表</span>
          </template>
          <div v-if="conversations.length">
            <div v-for="conv in conversations" :key="conv.id"
                 class="conv-item"
                 :class="{ active: currentConvId === conv.id }"
                 @click="selectConv(conv)">
              <span>{{ conv.senderId === userId ? '我' : '用户' + conv.senderId }}</span>
              <el-badge :value="conv.unread" :hidden="!conv.unread" />
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
import { ref, computed, onMounted } from 'vue'
import { messageApi } from '@/api/message'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.id)
const conversations = ref([])
const messages = ref([])
const currentConvId = ref(null)

async function selectConv(conv) {
  currentConvId.value = conv.id
  const otherId = conv.senderId === userId.value ? conv.receiverId : conv.senderId
  const res = await messageApi.conversation(otherId)
  messages.value = res.data
  await messageApi.markRead(conv.id)
}

onMounted(async () => {
  const res = await messageApi.unread()
  conversations.value = res.data || []
})
</script>

<style scoped>
.conv-item { padding: 12px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f0f0f0; }
.conv-item:hover, .conv-item.active { background: #ecf5ff; }
.msg-list { max-height: 500px; overflow-y: auto; }
.msg-item { display: flex; margin-bottom: 16px; }
.msg-item.mine { justify-content: flex-end; }
.msg-bubble { max-width: 70%; padding: 10px 14px; border-radius: 8px; background: #f0f0f0; }
.msg-item.mine .msg-bubble { background: #ecf5ff; }
</style>
