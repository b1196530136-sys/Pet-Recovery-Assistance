<template>
  <div class="post-detail-page">
    <el-button @click="$router.back()" :icon="ArrowLeft" class="back-btn">返回</el-button>
    <el-card v-if="post" class="detail-card">
      <section class="detail-summary">
        <div class="detail-cover">
          <el-image
            v-if="photoList.length"
            :src="photoList[0]"
            :preview-src-list="photoList"
            fit="cover"
            preview-teleported
          />
          <div v-else class="detail-cover-placeholder">暂无照片</div>
        </div>
        <div class="detail-main">
          <el-tag :type="tagType(post.status)" size="large">{{ statusMap[post.status] }}</el-tag>
          <h2>{{ post.petName || '未命名' }}（{{ typeMap[post.petType] || post.petType || '未知' }}）</h2>
          <p class="detail-address">{{ post.address }}</p>
          <div class="publisher-info">
            <el-avatar :size="36" :src="post.publisherAvatar || '/images/default-avatar.png'" />
            <span>发布人：{{ post.publisherName || '未知用户' }}</span>
          </div>
          <p class="action-hint">如果你近期见过相似宠物，可提交目击时间、地点和照片，线索会私信给失主。</p>
          <div class="action-row">
            <el-button v-if="post.status === 'ACTIVE' && userStore.isLoggedIn && post.userId !== userId" type="warning" size="large" @click="showClueDialog = true">我有线索</el-button>
            <el-button v-if="post.userId === userId && post.status === 'ACTIVE'" type="success" size="large" @click="handleResolve">已找到宠物</el-button>
          </div>
        </div>
      </section>

      <section class="detail-section">
        <h3>关键信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="品种">{{ post.breed || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="丢失时间">{{ post.lostTime }}</el-descriptions-item>
          <el-descriptions-item label="丢失地点" :span="2">{{ post.address }}</el-descriptions-item>
          <el-descriptions-item label="酬金">{{ post.reward || '未说明' }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section v-if="photoList.length" class="detail-section">
        <h3>宠物照片</h3>
        <div class="photo-gallery">
          <el-image
            v-for="(url, idx) in photoList"
            :key="idx"
            :src="url"
            :preview-src-list="photoList"
            :initial-index="idx"
            fit="cover"
            preview-teleported
          />
        </div>
      </section>

      <section class="detail-section">
        <h3>特征描述</h3>
        <p class="description-text">{{ post.description || '暂无描述' }}</p>
      </section>

      <section class="detail-section map-section">
        <h3>丢失位置</h3>
        <p class="page-lead">地图标注为发布人填写的丢失地点，可结合线索进一步核实。</p>
        <div ref="mapContainer" class="detail-map"></div>
      </section>
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
import { ArrowLeft } from '@element-plus/icons-vue'
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
const photoList = computed(() => {
  if (!post.value?.photos) return []
  return post.value.photos.split(',').map(url => url.trim()).filter(Boolean)
})

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
  // 校验：目击时间不能早于丢失时间
  if (clueForm.value.clueTime && post.value.lostTime) {
    const clueDate = new Date(clueForm.value.clueTime).getTime()
    const lostDate = new Date(post.value.lostTime).getTime()
    if (clueDate < lostDate) {
      ElMessage.warning('目击时间不能早于丢失时间')
      return
    }
  }
  const data = {
    postId: post.value.id,
    receiverId: post.value.userId,
    content: clueForm.value.content,
    msgType: 1
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
    }).then(async (AMap) => {
      const lng = parseFloat(post.value.longitude)
      const lat = parseFloat(post.value.latitude)
      const map = new AMap.Map(mapContainer.value, {
        zoom: 14,
        center: [lng, lat],
      })
      // 丢失地点标记
      new AMap.Marker({
        position: [lng, lat],
        label: { content: '<div style="background:#f56c6c;color:#fff;padding:2px 6px;border-radius:4px;font-size:12px">丢失地点</div>', direction: 'top' },
      })

      // 发布者查看线索轨迹
      if (post.value.userId === userId.value) {
        try {
          const trailRes = await postApi.clueTrail(post.value.id)
          const clues = trailRes.data || []
          if (clues.length > 0) {
            const path = clues.map(c => [parseFloat(c.clueLongitude), parseFloat(c.clueLatitude)])
            // 轨迹连线
            new AMap.Polyline({
              map,
              path,
              strokeColor: '#e6a23c',
              strokeWeight: 4,
              strokeOpacity: 0.8,
              lineJoin: 'round',
              lineCap: 'round',
              showDir: true,
            })
            // 线索标记
            clues.forEach((c, i) => {
              new AMap.Marker({
                map,
                position: path[i],
                label: { content: '<div style="background:#e6a23c;color:#fff;padding:2px 6px;border-radius:4px;font-size:12px">线索' + (i + 1) + '</div>', direction: 'top' },
              })
            })
            // 自适应视野
            map.setFitView()
          }
        } catch { /* ignore */ }
      }
    })
  }
})
</script>

<style scoped>
.post-detail-page { max-width: 980px; margin: 0 auto; }
.back-btn { margin-bottom: 14px; }
.detail-card :deep(.el-card__body) { padding: 22px; }
.detail-summary { display: grid; grid-template-columns: minmax(240px, 320px) 1fr; gap: 24px; align-items: stretch; margin-bottom: 28px; padding: 16px; border-radius: 10px; background: linear-gradient(135deg, #f6fbff, #fffaf2); }
.detail-cover,
.detail-cover :deep(.el-image) { width: 100%; min-height: 260px; height: 100%; border-radius: 8px; overflow: hidden; background: #f5f7fa; }
.detail-cover-placeholder { height: 100%; min-height: 260px; display: flex; align-items: center; justify-content: center; color: #98a2b3; }
.detail-main { display: flex; flex-direction: column; align-items: flex-start; justify-content: center; gap: 12px; padding: 8px 0; }
.detail-main h2 { font-size: 30px; line-height: 1.25; color: var(--color-text); }
.detail-address { color: var(--color-muted); line-height: 1.7; }
.publisher-info { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: #f7f9fc; border: 1px solid #edf1f5; border-radius: 8px; color: #606266; font-size: 14px; }
.action-hint { color: #667085; font-size: 13px; line-height: 1.7; }
.action-row { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 6px; }
.action-row :deep(.el-button--warning) { --el-button-bg-color: var(--color-warning); --el-button-border-color: var(--color-warning); --el-button-hover-bg-color: #d58b21; --el-button-hover-border-color: #d58b21; color: #fff; }
.detail-section { margin-top: 24px; }
.detail-section h3 { font-size: 18px; margin-bottom: 12px; color: var(--color-text); }
.photo-gallery { display: flex; gap: 10px; flex-wrap: wrap; }
.photo-gallery :deep(.el-image) { width: 150px; height: 150px; border-radius: 8px; overflow: hidden; }
.description-text { color: #475467; line-height: 1.8; white-space: pre-wrap; }
.detail-map { width: 100%; height: min(320px, 48vh); margin-top: 12px; border: 1px solid #edf1f5; border-radius: 8px; overflow: hidden; }

@media (max-width: 768px) {
  .detail-card :deep(.el-card__body) { padding: 16px; }
  .detail-summary { grid-template-columns: 1fr; gap: 16px; margin-bottom: 20px; }
  .detail-cover,
  .detail-cover :deep(.el-image) { min-height: 230px; height: 230px; }
  .detail-main h2 { font-size: 24px; }
  .action-row,
  .action-row :deep(.el-button) { width: 100%; }
  .photo-gallery :deep(.el-image) { width: calc(50% - 5px); height: 150px; }
  .post-detail-page :deep(.el-descriptions__body .el-descriptions__table) { display: block; }
  .post-detail-page :deep(.el-descriptions__body tbody),
  .post-detail-page :deep(.el-descriptions__body tr),
  .post-detail-page :deep(.el-descriptions__body th),
  .post-detail-page :deep(.el-descriptions__body td) { display: block; width: 100% !important; }
}
</style>
