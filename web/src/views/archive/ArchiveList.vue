<template>
  <div class="archive-list-page">
    <h2 style="margin-bottom: 20px">流浪动物电子档案</h2>

    <el-card class="search-bar" style="margin-bottom: 20px">
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
          <el-button type="primary" @click="search">搜索</el-button>
        </el-form-item>
        <div style="flex: 1" />
        <el-form-item>
          <el-button type="success" size="large" style="padding: 10px 30px; font-size: 16px" @click="handleRegister">登记</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="item in list" :key="item.id" :span="8" style="margin-bottom: 16px">
        <el-card @click="$router.push(`/archives/${item.id}`)" style="cursor: pointer">
          <el-image
            v-if="getFirstPhoto(item.photos)"
            :src="getPhotoUrl(getFirstPhoto(item.photos))"
            fit="cover"
            style="width: 100%; height: 180px; border-radius: 6px; margin-bottom: 12px"
          />
          <div v-else style="width: 100%; height: 180px; border-radius: 6px; margin-bottom: 12px; background: #f5f7fa; display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 13px">暂无图片</div>
          <h3>{{ typeMap[item.animalType] }}</h3>
          <el-tag size="small" :type="placementTagType[item.placementStatus]" style="margin-top: 8px">{{ placementMap[item.placementStatus] || '未知' }}</el-tag>
          <p style="font-size: 13px; color: #909399; margin-top: 8px">
            {{ item.address }}
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!list.length" description="暂无档案" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { archiveApi } from '@/api/archive'
import { useUserStore } from '@/store/user'

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
  if (!userStore.isLoggedIn || (userStore.userInfo?.role !== 'CERTIFIED' && userStore.userInfo?.role !== 'ADMIN')) {
    ElMessageBox.alert('权限不足！（需要成为认证用户）', '提示', { type: 'warning', confirmButtonText: '确定' })
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
