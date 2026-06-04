<template>
  <div class="post-detail-page" style="max-width: 900px; margin: 0 auto">
    <el-card v-if="post" class="detail-card">
      <div class="post-header">
        <div>
          <h2>{{ post.petName || ' unnamed ' }}({{ typeMap[post.petType] }})</h2>
          <el-tag :type="tagType(post.status)">{{ statusMap[post.status] }}</el-tag>
        </div>
        <div v-if="post.status === 'ACTIVE' && userStore.isLoggedIn" style="margin-top: 16px">
          <el-button type="warning" @click="showClueDialog = true">我有点线索</el-button>
        </div>
        <div v-if="post.userId === userId && post.status === 'ACTIVE'" style="margin-top: 8px">
          <el-button type="success" @click="handleResolve">已找到宠物</el-button>
        </div>
      </div>

      <el-descriptions :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="品种">{{ post.breed || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="丢失时间">{{ post.lostTime }}</el-descriptions-item>
        <el-descriptions-item label="丢失地点" :span="2">{{ post.address }}</el-descriptions-item>
        <el-descriptions-item label="酬金">{{ post.reward || '未说明' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 20px">
        <h4>特征描述</h4>
        <p>{{ post.description }}</p>
      </div>

      <div ref="mapContainer" style="width: 100%; height: 300px; margin-top: 20px"></div>
    </el-card>

    <el-dialog v-model="showClueDialog" title="提供线索" width="500px">
      <el-form :model="clueForm" label-width="100px">
        <el-form-item label="目击时间" required>
          <el-date-picker v-model="clueForm.clueTime" type="datetime" placeholder="选择时间" />
        </el-form-item>
        <el-form-item label="目击地点" required>
          <AmapPicker v-model="clueMapLocation" />
        </el-form-item>
        <el-form-item label="现场照片">
          <el-upload action="#" list-type="picture-card" :auto-upload="false">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="目击描述">
          <el-input v-model="clueForm.content" type="textarea" :rows="3" placeholder="描述目击时的具体情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showClueDialog = false">取消</el-button>
        <el-button type="primary" @click="submitClue">提交线索</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import { messageApi } from '@/api/message'
import { useUserStore } from '@/store/user'
import AmapPicker from '@/components/map/AmapPicker.vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userId = ref(null)

const post = ref(null)
const mapContainer = ref(null)
const showClueDialog = ref(false)
const clueForm = ref({ clueTime: '', content: '' })
const clueMapLocation = ref({ lng: '', lat: '', address: '' })

const statusMap = { PENDING: '待审核', ACTIVE: '寻找中', REJECTED: '已驳回', RESOLVED: '已找到' }
const typeMap = { cat: '猫', dog: '狗', other: '其他' }

function tagType(status) {
  return { PENDING: 'warning', ACTIVE: 'primary', REJECTED: 'danger', RESOLVED: 'success' }[status] || 'info'
}

async function submitClue() {
  await messageApi.send({
    postId: post.value.id,
    receiverId: post.value.userId,
    content: clueForm.value.content,
    clueTime: clueForm.value.clueTime,
    clueLongitude: clueMapLocation.value.lng,
    clueLatitude: clueMapLocation.value.lat,
    clueAddress: clueMapLocation.value.address,
  })
  ElMessage.success('线索已提交，将通过私信发送给失主')
  showClueDialog.value = false
}

async function handleResolve() {
  await postApi.resolve(post.value.id)
  ElMessage.success('已标记为已找到')
  router.push('/posts')
}

onMounted(async () => {
  const res = await postApi.detail(route.params.id)
  post.value = res.data
  userId.value = userStore.userInfo?.id

  if (post.value?.longitude && post.value?.latitude) {
    await nextTick()
    window._AMapSecurityConfig = {
      securityJsCode: 'd2a114ab986fe29825688ec540030b5a',
    }
    AMapLoader.load({
      key: 'eb4473d0bf626ceed61dbb79c86ba988',
      version: '2.0',
    }).then((AMap) => {
      const map = new AMap.Map(mapContainer.value, {
        zoom: 15,
        center: [parseFloat(post.value.longitude), parseFloat(post.value.latitude)],
      })
      const marker = new AMap.Marker({
        position: [parseFloat(post.value.longitude), parseFloat(post.value.latitude)],
      })
      map.add(marker)
    })
  }
})
</script>
