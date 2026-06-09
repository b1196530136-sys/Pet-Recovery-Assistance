<template>
  <div class="post-list-page">
    <div class="page-heading list-heading">
      <div>
        <h2 class="page-title">寻宠大厅</h2>
        <p class="page-lead">按类型、地区或照片快速查找走失宠物线索。</p>
      </div>
      <router-link to="/posts/create">
        <el-button type="primary" size="large" class="primary-cta">我要寻宠</el-button>
      </router-link>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="search-bar">
      <el-form :model="filters" inline>
        <el-form-item label="宠物类型">
          <el-select v-model="filters.petType" placeholder="全部" clearable style="width: 120px">
            <el-option label="猫" value="cat" />
            <el-option label="狗" value="dog" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-input v-if="filters.petType === 'other'" v-model="filters.customPetType" placeholder="具体类型" style="width: 120px; margin-left: 8px;" />
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="filters.province" placeholder="省/市" style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="search-action" @click="search">搜索</el-button>
          <el-button type="warning" class="image-action" @click="showImageSearch = true">以图搜宠</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-row :gutter="18" class="post-grid">
      <el-col v-for="post in posts" :key="post.id" :xs="24" :sm="12" :md="8" style="margin-bottom: 18px">
        <el-card class="post-card" @click="$router.push(`/posts/${post.id}`)" style="cursor: pointer">
          <div class="post-status" :class="post.status">{{ statusMap[post.status] }}</div>
          <div v-if="post.similarity" class="post-similarity">{{ post.similarity }}%</div>
          <div v-if="post.photos" class="post-photo">
            <el-image
              :src="post.photos.split(',')[0]"
              style="width: 100%; height: 160px; border-radius: 4px;"
              fit="cover"
              :preview-src-list="post.photos.split(',')"
              preview-teleported
            />
          </div>
          <div v-else class="post-photo-placeholder">
            <span>暂无照片</span>
          </div>
          <h3 class="post-title line-clamp-1">{{ post.petName || '未命名' }} ({{ typeMap[post.petType] || post.petType || '未知' }})</h3>
          <p class="post-meta line-clamp-2">
            {{ post.address }} · {{ formatDate(post.lostTime) }}
          </p>
          <p class="post-desc line-clamp-2">
            {{ post.description?.slice(0, 60) }}...
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!posts.length" description="暂时没有匹配的寻宠启事，换个条件或发布新的求助吧" />

    <div class="pagination" style="margin-top: 20px; text-align: center">
      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        layout="prev, pager, next"
        @current-change="search"
      />
    </div>

    <!-- 以图搜宠弹窗（调用阿里云宠物相似度对比API） -->
    <el-dialog v-model="showImageSearch" title="以图搜宠" width="500px" @close="imageSearchResult = null">
      <div v-if="!imageSearchResult" style="text-align: center; padding: 20px 0;">
        <el-upload
          ref="imageSearchUpload"
          action="#"
          :auto-upload="false"
          accept="image/*"
          :show-file-list="false"
          :on-change="onImageSearchChange"
        >
          <el-button type="primary" size="large" :loading="imageSearching">
            上传照片进行搜索
          </el-button>
        </el-upload>
        <p style="font-size: 13px; color: #909399; margin-top: 12px;">
          上传一张宠物照片，系统将通过AI图像识别接口与寻宠信息中的照片进行比对
        </p>
        <el-image v-if="previewImage" :src="previewImage" style="max-width: 200px; max-height: 200px; margin-top: 16px; border-radius: 8px;" fit="contain" />
      </div>
      <div v-else>
        <div v-if="imageSearchResult.length" style="max-height: 400px; overflow-y: auto;">
          <div v-for="item in imageSearchResult" :key="item.id" class="image-search-item" @click="$router.push(`/posts/${item.id}`)">
            <el-image v-if="item.photos" :src="item.photos.split(',')[0]" style="width: 80px; height: 80px; border-radius: 6px; flex-shrink: 0;" fit="cover" />
            <div style="flex: 1; min-width: 0;">
              <div style="font-weight: 500;">{{ item.petName || '未命名' }} ({{ typeMap[item.petType] || item.petType || '未知' }})</div>
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">{{ item.address }}</div>
              <div v-if="item.recognitionDesc" style="font-size: 11px; color: #e6a23c; margin-top: 2px;">{{ item.recognitionDesc }}</div>
            </div>
            <div class="similarity-tag" :class="simClass(item.similarity)">{{ item.similarity }}%</div>
          </div>
        </div>
        <el-empty v-else description="未找到相似的寻宠信息，请尝试更换照片或手动搜索" />
      </div>
      <template #footer>
        <el-button @click="resetImageSearch">{{ imageSearchResult ? '重新搜索' : '取消' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { postApi } from '@/api/post'

const posts = ref([])
const page = ref(1)
const total = ref(0)
const filters = ref({ petType: '', customPetType: '', province: '' })

// 以图搜宠相关变量和方法
const showImageSearch = ref(false)
const imageSearching = ref(false)
const imageSearchResult = ref(null)
const previewImage = ref('')

const statusMap = { PENDING: '待审核', ACTIVE: '寻找中', REJECTED: '已驳回', RESOLVED: '已找到' }
const typeMap = { cat: '猫', dog: '狗', other: '其他' }

function formatDate(d) {
  return d ? d.slice(0, 10) : ''
}

function simClass(similarity) {
  if (similarity >= 90) return 'sim-high'
  if (similarity >= 75) return 'sim-mid'
  return 'sim-low'
}

async function search() {
  const params = { ...filters.value, page: page.value, size: 20 }
  if (params.petType === 'other' && params.customPetType) params.petType = params.customPetType
  delete params.customPetType
  const res = await postApi.search(params)
  posts.value = res.data.records
  total.value = res.data.total
}

function onImageSearchChange(file) {
  if (!file.raw) return
  if (!file.raw.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  if (file.raw.size / 1024 / 1024 >= 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }
  previewImage.value = URL.createObjectURL(file.raw)
  // 先上传到图床，再进行以图搜宠
  uploadAndSearchByImage(file.raw)
}

async function uploadAndSearchByImage(file) {
  imageSearching.value = true
  try {
    // 1. 先将图片上传到图床
    const uploadRes = await uploadToImageHost(file)
    if (!uploadRes || !uploadRes.data) {
      throw new Error('图片上传到图床失败')
    }
    const imageUrl = uploadRes.data
    console.log('图片已上传到图床:', imageUrl)

    // 2. 使用图床URL进行以图搜宠
    const res = await postApi.searchByImageUrl(imageUrl)
    imageSearchResult.value = res.data || []
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || '图片搜索失败，请稍后重试'
    ElMessage.error(msg)
  }
  imageSearching.value = false
}

async function uploadToImageHost(file) {
  const formData = new FormData()
  formData.append('file', file)
  // 复用现有的图床上传接口
  const request = (await import('@/utils/request')).default
  return request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

function resetImageSearch() {
  showImageSearch.value = false
  imageSearchResult.value = null
  previewImage.value = ''
}

onMounted(search)
</script>

<style scoped>
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.list-heading { padding: 4px 0 2px; }
.primary-cta { min-width: 132px; }
.search-bar { margin-bottom: 22px; border-left: 4px solid var(--color-primary); }
.search-bar :deep(.el-form-item__label) { font-weight: 700; color: #475467; }
.search-action,
.image-action { min-width: 92px; }
.post-card { position: relative; height: 100%; transition: transform 0.18s ease, box-shadow 0.18s ease; }
.post-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-card); }
.post-card :deep(.el-card__body) { padding: 18px; }
.post-photo { margin-bottom: 10px; }
.post-photo :deep(.el-image) { display: block; }
.post-photo-placeholder { height: 160px; border-radius: 6px; background: linear-gradient(135deg, #eef7ff, #fff8ed); display: flex; align-items: center; justify-content: center; color: #98a2b3; font-size: 13px; margin-bottom: 10px; border: 1px dashed #d9e3ee; }
.post-photo-placeholder span { padding: 7px 12px; border-radius: 999px; background: rgba(255,255,255,0.74); }
.post-title { font-size: 18px; line-height: 1.4; color: var(--color-text); }
.post-meta { font-size: 13px; color: var(--color-muted); margin-top: 8px; line-height: 1.6; min-height: 42px; }
.post-desc { font-size: 13px; color: #606266; margin-top: 4px; line-height: 1.6; min-height: 40px; }
.post-status { position: absolute; top: 14px; right: 14px; padding: 4px 9px; border-radius: 999px; font-size: 12px; z-index: 1; font-weight: 700; box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12); }
.post-status.ACTIVE { background: #e6f7ff; color: #1890ff; }
.post-status.RESOLVED { background: #f6ffed; color: #52c41a; }
.post-status.REJECTED { background: #fff2f0; color: #ff4d4f; }
.post-status.PENDING { background: #fffbe6; color: #faad14; }
.post-similarity { position: absolute; top: 12px; left: 12px; padding: 2px 8px; border-radius: 4px; font-size: 12px; z-index: 1; background: #e6f7ff; color: #1890ff; font-weight: bold; }
.image-search-item { display: flex; align-items: center; gap: 12px; padding: 12px; cursor: pointer; border-radius: 8px; margin-bottom: 8px; transition: background 0.2s; border: 1px solid #ebeef5; }
.image-search-item:hover { background: #f5f7fa; }
.similarity-tag { padding: 4px 10px; border-radius: 12px; font-size: 13px; font-weight: bold; flex-shrink: 0; }
.similarity-tag.sim-high { background: #f6ffed; color: #52c41a; }
.similarity-tag.sim-mid { background: #fffbe6; color: #faad14; }
.similarity-tag.sim-low { background: #fff7e6; color: #fa8c16; }

@media (max-width: 768px) {
  .page-heading { display: grid; gap: 12px; }
  .primary-cta { width: 100%; }
  .post-photo-placeholder,
  .post-photo :deep(.el-image) { height: 210px !important; }
  .post-meta,
  .post-desc { min-height: 0; }
}
</style>
