<template>
  <div class="archive-list-page">
    <div class="page-heading list-heading">
      <div>
        <h2 class="page-title">流浪动物电子档案</h2>
        <p class="page-lead">记录发现地点、健康状态和安置信息，方便救助与领养跟进。</p>
      </div>
      <el-button type="success" size="large" class="primary-cta" @click="handleRegister">登记</el-button>
    </div>

    <el-card class="search-bar">
      <el-form :model="filters" inline>
        <el-form-item label="动物类型">
          <el-select v-model="filters.animalType" placeholder="全部" clearable style="width: 120px">
            <el-option label="猫" value="cat" />
            <el-option label="狗" value="dog" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-input v-if="filters.animalType === 'other'" v-model="filters.customAnimalType" placeholder="具体类型" style="width: 120px; margin-left: 8px;" />
        </el-form-item>
        <el-form-item label="安置状态">
          <el-select v-model="filters.placementStatus" placeholder="全部" clearable style="width: 150px">
            <el-option label="原地观察" value="observing" />
            <el-option label="基地收容" value="sheltered" />
            <el-option label="开放领养" value="adoptable" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="search-action" @click="search">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <section class="care-panel">
      <div class="care-photo" aria-hidden="true"></div>
      <div class="care-copy">
        <span>认证救助协作</span>
        <strong>把发现位置、健康状况和安置进展记录清楚，后续领养与回访才有依据。</strong>
      </div>
    </section>

    <el-row :gutter="18" class="archive-grid">
      <el-col v-for="item in list" :key="item.id" :xs="24" :sm="12" :md="8" style="margin-bottom: 18px">
        <el-card class="archive-card" @click="$router.push(`/archives/${item.id}`)" style="cursor: pointer">
          <el-image
            v-if="getFirstPhoto(item.photos)"
            :src="getPhotoUrl(getFirstPhoto(item.photos))"
            fit="cover"
            style="width: 100%; height: 180px; border-radius: 6px; margin-bottom: 12px"
          />
          <div v-else class="archive-photo-placeholder"><span>暂无图片</span></div>
          <h3 class="archive-title line-clamp-1">{{ typeMap[item.animalType] || item.animalType || '未知' }}<span v-if="item.name">{{ item.name }}</span></h3>
          <el-tag size="small" :type="placementTagType[item.placementStatus]" style="margin-top: 8px">{{ placementMap[item.placementStatus] || '未知' }}</el-tag>
          <p class="archive-address line-clamp-2">
            {{ item.address }}
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!list.length" description="暂无符合条件的档案，可以尝试更换筛选条件" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { archiveApi } from '@/api/archive'
import { useUserStore } from '@/store/user'
import { alertArchiveCreateDenied, canCreateArchive } from '@/utils/archivePermission'

const router = useRouter()
const userStore = useUserStore()

const list = ref([])
const filters = ref({ animalType: '', customAnimalType: '', placementStatus: '' })
const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const placementMap = { observing: '原地观察', sheltered: '基地收容', adoptable: '开放领养', adopted: '已被领养' }
const placementTagType = { observing: 'info', sheltered: 'warning', adoptable: 'success', adopted: 'success' }

function getFirstPhoto(photos) {
  if (!photos) return ''
  return photos.split(',')[0] || ''
}

function getPhotoUrl(photo) {
  if (!photo) return ''
  if (photo.startsWith('http')) return photo
  if (photo.startsWith('/')) return photo
  return `/upload/${photo}`
}

function handleRegister() {
  if (!canCreateArchive(userStore)) {
    alertArchiveCreateDenied()
    return
  }
  router.push('/archives/create')
}

async function search() {
  const params = { ...filters.value, page: 1, size: 20 }
  if (params.animalType === 'other' && params.customAnimalType) params.animalType = params.customAnimalType
  delete params.customAnimalType
  const res = await archiveApi.search(params)
  list.value = res.data.records
}

onMounted(search)
</script>

<style scoped>
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.list-heading { padding: 4px 0 2px; }
.primary-cta { min-width: 132px; }
.search-bar { margin-bottom: 22px; border-left: 4px solid var(--color-rescue); }
.search-bar :deep(.el-form-item__label) { font-weight: 700; color: #475467; }
.search-action { min-width: 92px; }
.care-panel { display: grid; grid-template-columns: 190px 1fr; gap: 18px; align-items: center; margin-bottom: 22px; padding: 14px; border: 1px solid var(--color-line); border-radius: var(--radius-md); background: #fff; box-shadow: var(--shadow-soft); }
.care-photo { height: 118px; border-radius: 8px; background: url('/images/archive-care.jpg') center/cover no-repeat; }
.care-copy { display: grid; gap: 8px; }
.care-copy span { color: var(--color-rescue); font-size: 13px; font-weight: 800; }
.care-copy strong { max-width: 680px; color: var(--color-text); font-size: 17px; line-height: 1.6; }
.archive-card { height: 100%; transition: transform 0.18s ease, box-shadow 0.18s ease; }
.archive-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-card); }
.archive-card :deep(.el-card__body) { padding: 18px; }
.archive-card :deep(.el-image) { display: block; }
.archive-photo-placeholder { width: 100%; height: 180px; border-radius: 6px; margin-bottom: 12px; background: linear-gradient(135deg, #eef8f0, #fff8ed); display: flex; align-items: center; justify-content: center; color: #98a2b3; font-size: 13px; border: 1px dashed #d9e3ee; }
.archive-photo-placeholder span { padding: 7px 12px; border-radius: 999px; background: rgba(255,255,255,0.74); }
.archive-title { display: flex; align-items: baseline; gap: 6px; font-size: 18px; line-height: 1.4; color: var(--color-text); }
.archive-title span { font-weight: 400; font-size: 14px; color: var(--color-muted); }
.archive-address { font-size: 13px; color: var(--color-muted); margin-top: 8px; line-height: 1.6; min-height: 42px; }

@media (max-width: 768px) {
  .page-heading { display: grid; gap: 12px; }
  .primary-cta { width: 100%; }
  .care-panel { grid-template-columns: 1fr; }
  .care-photo { height: 170px; }
  .archive-card :deep(.el-image),
  .archive-photo-placeholder { height: 210px !important; }
  .archive-address { min-height: 0; }
}
</style>
