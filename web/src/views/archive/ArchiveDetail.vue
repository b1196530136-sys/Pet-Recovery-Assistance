<template>
  <div class="archive-detail-page" style="max-width: 900px; margin: 0 auto">
    <el-card v-if="archive">
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px">
        <div style="display: flex; align-items: center">
          <el-button @click="$router.back()" :icon="ArrowLeft" style="margin-right: 12px">返回</el-button>
          <h2 style="margin: 0">{{ typeMap[archive.animalType] }} · 电子档案</h2>
        </div>
        <div v-if="isOwner" style="display: flex; gap: 8px">
          <el-tag v-if="archive.isUpdate" type="warning" style="margin-right: 8px">修改待审核</el-tag>
          <el-button type="primary" @click="handleEdit">编辑</el-button>
          <el-button type="danger" @click="handleDelete">删除</el-button>
        </div>
        <div v-else-if="archive.placementStatus === 'adoptable' && userStore.isLoggedIn">
          <el-button type="success" @click="showAdoptDialog = true">我要领养</el-button>
        </div>
        <div v-else-if="archive.placementStatus === 'adoptable'">
          <el-button type="success" @click="$router.push('/auth/login')">登录后可领养</el-button>
        </div>
      </div>

      <div v-if="archive.photos" class="photo-gallery" style="margin-bottom: 20px">
        <el-image
          v-for="(photo, index) in photoList"
          :key="index"
          :src="getPhotoUrl(photo)"
          :preview-src-list="previewPhotoList"
          :initial-index="index"
          fit="cover"
          style="width: 200px; height: 200px; border-radius: 8px; margin-right: 12px; margin-bottom: 12px"
        />
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="动物类型">{{ typeMap[archive.animalType] }}</el-descriptions-item>
        <el-descriptions-item label="安置状态">{{ placementMap[archive.placementStatus] }}</el-descriptions-item>
        <el-descriptions-item label="健康状况">{{ archive.healthStatus || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="绝育/免疫">{{ archive.neuteredStatus || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="发现地点" :span="2">{{ archive.address }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ archive.publisherName || '匿名用户' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatTime(archive.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <p style="margin-top: 16px">{{ archive.description }}</p>
      <div ref="mapContainer" style="width: 100%; height: 300px; margin-top: 20px"></div>
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
