<template>
  <div class="archive-detail-page">
    <el-button @click="$router.back()" :icon="ArrowLeft" class="back-btn">返回</el-button>
    <el-card v-if="archive" class="detail-card">
      <section class="detail-summary">
        <div class="detail-cover">
          <el-image
            v-if="photoList.length"
            :src="getPhotoUrl(photoList[0])"
            :preview-src-list="previewPhotoList"
            fit="cover"
            preview-teleported
          />
          <div v-else class="detail-cover-placeholder">暂无图片</div>
        </div>
        <div class="detail-main">
          <el-tag :type="placementTagType[archive.placementStatus]" size="large">{{ placementMap[archive.placementStatus] || '未知' }}</el-tag>
          <h2>{{ typeMap[archive.animalType] || archive.animalType || '未知' }} · 电子档案</h2>
          <p class="detail-address">{{ archive.address }}</p>
          <div class="publisher-info">
            <el-avatar :size="36" :src="archive.publisherAvatar || '/images/default-avatar.png'" />
            <span>发布人：{{ archive.publisherName || '匿名用户' }}</span>
          </div>
          <p class="action-hint">开放领养的档案可直接向发布人发送申请，后续沟通会在站内消息中跟进。</p>
          <div class="action-row">
            <template v-if="isOwner">
              <el-tag v-if="archive.isUpdate" type="warning">修改待审核</el-tag>
              <el-button type="primary" @click="handleEdit">编辑</el-button>
              <el-button type="danger" @click="handleDelete">删除</el-button>
            </template>
            <el-button v-else-if="archive.placementStatus === 'adoptable' && userStore.isLoggedIn" type="success" size="large" @click="showAdoptDialog = true">我要领养</el-button>
            <el-button v-else-if="archive.placementStatus === 'adoptable'" type="success" size="large" @click="$router.push('/auth/login')">登录后可领养</el-button>
          </div>
        </div>
      </section>

      <section class="detail-section">
        <h3>档案信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="动物类型">{{ typeMap[archive.animalType] || archive.animalType }}</el-descriptions-item>
          <el-descriptions-item label="动物昵称">{{ archive.name || '无' }}</el-descriptions-item>
          <el-descriptions-item label="安置状态">{{ placementMap[archive.placementStatus] }}</el-descriptions-item>
          <el-descriptions-item label="健康状况">{{ archive.healthStatus || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="绝育/免疫">{{ archive.neuteredStatus || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="发现地点" :span="2">{{ archive.address }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(archive.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section v-if="photoList.length" class="detail-section">
        <h3>现场照片</h3>
        <div class="photo-gallery">
          <el-image
            v-for="(photo, index) in photoList"
            :key="index"
            :src="getPhotoUrl(photo)"
            :preview-src-list="previewPhotoList"
            :initial-index="index"
            fit="cover"
            preview-teleported
          />
        </div>
      </section>

      <section class="detail-section">
        <h3>备注描述</h3>
        <p class="description-text">{{ archive.description || '暂无描述' }}</p>
      </section>

      <section class="detail-section map-section">
        <h3>发现位置</h3>
        <p class="page-lead">地图展示该动物首次登记的发现地点。</p>
        <div ref="mapContainer" class="detail-map"></div>
      </section>
    </el-card>

    <el-dialog v-model="showAdoptDialog" title="联系发布人" width="500px">
      <el-form :model="adoptForm" label-width="80px">
        <el-form-item label="发布人">
          <span>{{ archive?.publisherName || '匿名用户' }}</span>
        </el-form-item>
        <el-form-item label="留言内容" required>
          <el-input v-model="adoptForm.content" type="textarea" :rows="4" placeholder="请说明您的领养意向和联系方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdoptDialog = false">取消</el-button>
        <el-button type="primary" :loading="adoptLoading" @click="submitAdopt">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { archiveApi } from '@/api/archive'
import { adoptionApi } from '@/api/adoption'
import { messageApi } from '@/api/message'
import { useUserStore } from '@/store/user'
import AMapLoader from '@amap/amap-jsapi-loader'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const archive = ref(null)
const mapContainer = ref(null)
const showAdoptDialog = ref(false)
const adoptLoading = ref(false)
const adoptForm = ref({ content: '' })
const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const placementMap = { observing: '原地观察', sheltered: '基地收容', adoptable: '开放领养', adopted: '已被领养' }
const placementTagType = { observing: 'info', sheltered: 'warning', adoptable: 'success', adopted: 'success' }

const isOwner = computed(() => {
  return userStore.isLoggedIn && archive.value && userStore.userInfo?.id === archive.value.userId
})

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const photoList = computed(() => {
  if (!archive.value?.photos) return []
  return archive.value.photos.split(',').filter(Boolean)
})

const previewPhotoList = computed(() => photoList.value.map(p => getPhotoUrl(p)))

function getPhotoUrl(photo) {
  if (!photo) return ''
  if (photo.startsWith('http')) return photo
  if (photo.startsWith('/')) return photo
  return `/upload/${photo}`
}

function handleEdit() {
  router.push({ path: '/archives/create', query: { id: archive.value.id } })
}

async function handleDelete() {
  await ElMessageBox.confirm('确定要删除该档案吗？删除后无法恢复。', '确认删除', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await archiveApi.delete(archive.value.id)
  ElMessage.success('删除成功')
  router.push('/archives')
}

async function submitAdopt() {
  if (!adoptForm.value.content.trim()) {
    ElMessage.warning('请填写留言内容')
    return
  }
  adoptLoading.value = true
  try {
    await adoptionApi.create({
      archiveId: archive.value.id,
      message: adoptForm.value.content,
    })
    ElMessage.success('领养申请已提交，请等待发布人回复')
    showAdoptDialog.value = false
    adoptForm.value.content = ''
  } catch { /* ignore */ }
  adoptLoading.value = false
}

onMounted(async () => {
  const res = await archiveApi.detail(route.params.id)
  archive.value = res.data

  if (archive.value?.longitude && archive.value?.latitude) {
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
        center: [parseFloat(archive.value.longitude), parseFloat(archive.value.latitude)],
      })
      const marker = new AMap.Marker({
        position: [parseFloat(archive.value.longitude), parseFloat(archive.value.latitude)],
      })
      map.add(marker)
    })
  }
})
</script>

<style scoped>
.archive-detail-page { max-width: 980px; margin: 0 auto; }
.back-btn { margin-bottom: 14px; }
.detail-card :deep(.el-card__body) { padding: 22px; }
.detail-summary { display: grid; grid-template-columns: minmax(240px, 320px) 1fr; gap: 24px; align-items: stretch; margin-bottom: 28px; padding: 16px; border-radius: 10px; background: linear-gradient(135deg, #f3fbf5, #fffaf2); }
.detail-cover,
.detail-cover :deep(.el-image) { width: 100%; min-height: 260px; height: 100%; border-radius: 8px; overflow: hidden; background: #f5f7fa; }
.detail-cover-placeholder { height: 100%; min-height: 260px; display: flex; align-items: center; justify-content: center; color: #98a2b3; }
.detail-main { display: flex; flex-direction: column; align-items: flex-start; justify-content: center; gap: 12px; padding: 8px 0; }
.detail-main h2 { font-size: 30px; line-height: 1.25; color: var(--color-text); }
.detail-address { color: var(--color-muted); line-height: 1.7; }
.publisher-info { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: #f7f9fc; border: 1px solid #edf1f5; border-radius: 8px; color: #606266; font-size: 14px; }
.action-hint { color: #667085; font-size: 13px; line-height: 1.7; }
.action-row { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-top: 6px; }
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
  .archive-detail-page :deep(.el-descriptions__body .el-descriptions__table) { display: block; }
  .archive-detail-page :deep(.el-descriptions__body tbody),
  .archive-detail-page :deep(.el-descriptions__body tr),
  .archive-detail-page :deep(.el-descriptions__body th),
  .archive-detail-page :deep(.el-descriptions__body td) { display: block; width: 100% !important; }
}
</style>
