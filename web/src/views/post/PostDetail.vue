<template>
  <div class="post-detail-page" style="max-width: 900px; margin: 0 auto">
    <el-card v-if="post" class="detail-card">
      <div class="post-header">
        <div>
          <h2>{{ post.petName || ' unnamed ' }}({{ typeMap[post.petType] }})</h2>
          <el-tag :type="tagType(post.status)">{{ statusMap[post.status] }}</el-tag>
        </div>
        <div v-if="post.status === 'ACTIVE' && userStore.isLoggedIn && post.userId !== userId" style="margin-top: 16px">
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

      <div v-if="post.photos" style="margin-top: 20px">
        <h4>宠物照片</h4>
        <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px">
          <el-image
            v-for="(url, idx) in post.photos.split(',')"
            :key="idx"
            :src="url"
            :preview-src-list="post.photos.split(',')"
            :initial-index="idx"
            style="width: 160px; height: 160px; border-radius: 6px;"
            fit="cover"
            preview-teleported
          />
        </div>
      </div>

      <div style="margin-top: 20px">
        <h4>特征描述</h4>
        <p>{{ post.description }}</p>
      </div>

      <div ref="mapContainer" style="width: 100%; height: 300px; margin-top: 20px"></div>
    </el-card>

    <el-dialog v-model="showClueDialog" title="提供线索" width="500px">
      <el-form :model="clueForm" label-width="100px">
        <el-form-item label="目击时间" required>
          <el-date-picker v-model="clueForm.clueTime" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="目击地点" required>
          <AmapPicker v-model="clueMapLocation" />
        </el-form-item>
        <el-form-item label="现场照片">
          <el-upload
            ref="clueUploadRef"
            action="/api/upload/image"
            list-type="picture-card"
            name="file"
            accept="image/*"
            :headers="uploadHeaders"
            :on-success="onCluePhotoSuccess"
            :on-remove="onCluePhotoRemove"
            :before-upload="beforeClueUpload"
          >
            <div style="display: flex; flex-direction: column; align-items: center;">
              <el-icon><Plus /></el-icon>
              <span style="font-size: 12px; color: #909399; margin-top: 4px;">点击上传图片</span>
            </div>
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
const clueUploadRef = ref(null)
const cluePhotoUrls = ref([])
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const statusMap = { PENDING: '待审核', ACTIVE: '寻找中', REJECTED: '已驳回', RESOLVED: '已找到' }
const typeMap = { cat: '猫', dog: '狗', other: '其他' }

function tagType(status) {
  return { PENDING: 'warning', ACTIVE: 'primary', REJECTED: 'danger', RESOLVED: 'success' }[status] || 'info'
}

function beforeClueUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  return file.size / 1024 / 1024 < 5 || (ElMessage.error('图片大小不能超过 5MB'), false)
}

function onCluePhotoSuccess(response, file) {
  if (response.code === 200 && response.data) {
    cluePhotoUrls.value.push(response.data)
    file.url = response.data
  }
}

function onCluePhotoRemove(_file, fileList) {
  cluePhotoUrls.value = fileList.map(f => f.url).filter(Boolean)
}

async function submitClue() {
  const data = {
    postId: post.value.id,
    receiverId: post.value.userId,
    content: clueForm.value.content,
  }
  if (clueForm.value.clueTime) {
    data.clueTime = clueForm.value.clueTime
  }
  if (cluePhotoUrls.value.length) {
    data.cluePhotos = cluePhotoUrls.value.join(',')
  }
  if (clueMapLocation.value.lng && clueMapLocation.value.lat) {
    data.clueLongitude = clueMapLocation.value.lng
    data.clueLatitude = clueMapLocation.value.lat
    data.clueAddress = clueMapLocation.value.address
  }
  await messageApi.send(data)
  ElMessage.success('线索已提交，将通过私信发送给失主')
  showClueDialog.value = false
  cluePhotoUrls.value = []
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
