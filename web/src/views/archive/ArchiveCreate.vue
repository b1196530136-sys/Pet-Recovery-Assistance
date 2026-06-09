<template>
  <div class="archive-create-page form-page">
    <el-button :icon="ArrowLeft" @click="$router.back()" style="margin-bottom: 8px">返回</el-button>
    <h2 class="page-title">{{ isEdit ? '编辑流浪动物档案' : '登记流浪动物档案' }}</h2>
    <el-alert title="仅认证用户可以登记动物档案" type="warning" :closable="false" show-icon style="margin-bottom: 20px" />
    <el-card class="form-card">
      <el-form :model="form" label-width="120px">
        <section class="form-section">
          <h3>基础信息</h3>
          <el-form-item label="动物类型" required>
            <el-radio-group v-model="form.animalType">
              <el-radio value="cat">猫</el-radio>
              <el-radio value="dog">狗</el-radio>
              <el-radio value="other">其他</el-radio>
            </el-radio-group>
            <el-input v-if="form.animalType === 'other'" v-model="form.customAnimalType" placeholder="请填写具体类型" style="width: 200px; margin-top: 8px;" />
          </el-form-item>
          <el-form-item label="动物昵称">
            <el-input v-model="form.name" placeholder="如有多只猫狗可区别称呼" />
          </el-form-item>
        </section>

        <section v-if="!isEdit" class="form-section">
          <h3>发现位置</h3>
          <el-form-item label="发现位置" required class="map-form-item">
            <AmapPicker v-model="mapLocation" />
          </el-form-item>
        </section>

        <section class="form-section">
          <h3>健康与安置</h3>
          <el-form-item label="健康状况">
            <el-select v-model="form.healthStatus" placeholder="请选择" clearable>
              <el-option label="良好" value="良好" />
              <el-option label="受伤" value="受伤" />
              <el-option label="体弱" value="体弱" />
            </el-select>
          </el-form-item>
          <el-form-item label="绝育状态">
            <el-select v-model="form.neuteredStatus" placeholder="请选择" clearable>
              <el-option label="已绝育" value="已绝育" />
              <el-option label="未绝育" value="未绝育" />
            </el-select>
          </el-form-item>
          <el-form-item label="免疫状态">
            <el-select v-model="form.immuneStatus" placeholder="请选择" clearable>
              <el-option label="已免疫" value="已免疫" />
              <el-option label="未免疫" value="未免疫" />
            </el-select>
          </el-form-item>
          <el-form-item label="安置状态" required>
            <el-select v-model="form.placementStatus">
              <el-option label="原地流浪观察" value="observing" />
              <el-option label="救助基地收容" value="sheltered" />
              <el-option label="已开放领养" value="adoptable" />
            </el-select>
          </el-form-item>
        </section>

        <section class="form-section">
          <h3>照片与备注</h3>
          <el-form-item label="现场照片">
            <el-upload
              action="/api/upload/image"
              list-type="picture-card"
              accept="image/*"
              name="file"
              :headers="uploadHeaders"
              :on-success="onPhotoSuccess"
              :on-remove="onPhotoRemove"
              :before-upload="beforePhotoUpload"
            >
              <div style="display: flex; flex-direction: column; align-items: center;">
                <el-icon><Plus /></el-icon>
                <span style="font-size: 12px; color: #909399; margin-top: 4px;">点击上传图片</span>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="备注描述">
            <el-input v-model="form.description" type="textarea" :rows="3" />
          </el-form-item>
        </section>

        <el-form-item class="submit-row">
          <el-button type="primary" size="large" @click="submit">{{ isEdit ? '更新档案' : '提交审核' }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { archiveApi } from '@/api/archive'
import { useUserStore } from '@/store/user'
import AmapPicker from '@/components/map/AmapPicker.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isEdit = computed(() => !!route.query.id)
const form = reactive({ id: null, animalType: 'cat', customAnimalType: '', name: '', healthStatus: '', neuteredStatus: '', immuneStatus: '', placementStatus: 'observing', description: '', photos: '', longitude: '', latitude: '', address: '' })
const photoUrls = reactive([])
const mapLocation = reactive({ lng: '', lat: '', address: '' })

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

function beforePhotoUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 >= 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

function onPhotoSuccess(response) {
  if (response.code === 200 && response.data) {
    photoUrls.push(response.data)
    form.photos = photoUrls.join(',')
  }
}

function onPhotoRemove() {
  photoUrls.pop()
  form.photos = photoUrls.join(',')
}

watch(mapLocation, (val) => {
  form.longitude = val.lng
  form.latitude = val.lat
  form.address = val.address
}, { deep: true })

async function submit() {
  form.longitude = mapLocation.lng
  form.latitude = mapLocation.lat
  form.address = mapLocation.address
  if (form.animalType === 'other' && form.customAnimalType) form.animalType = form.customAnimalType
  if (isEdit.value) {
    await archiveApi.update(form)
    ElMessage.success('更新成功，请等待后台审核')
  } else {
    await archiveApi.create(form)
    ElMessage.success('提交成功，请等待后台审核')
  }
  router.push('/archives')
}

onMounted(async () => {
  if (isEdit.value) {
    const res = await archiveApi.detail(route.query.id)
    const data = res.data
    Object.assign(form, {
      id: data.id,
      animalType: data.animalType,
      name: data.name || '',
      healthStatus: data.healthStatus || '',
      neuteredStatus: data.neuteredStatus || '',
      immuneStatus: data.immuneStatus || '',
      placementStatus: data.placementStatus,
      description: data.description || '',
      photos: data.photos || '',
      longitude: data.longitude || '',
      latitude: data.latitude || '',
      address: data.address || '',
    })
    if (data.photos) {
      photoUrls.splice(0, photoUrls.length, ...data.photos.split(',').filter(Boolean))
    }
    Object.assign(mapLocation, { lng: data.longitude || '', lat: data.latitude || '', address: data.address || '' })
  }
})
</script>

<style scoped>
.form-page { max-width: 860px; margin: 0 auto; }
.form-card :deep(.el-card__body) { padding: 28px 32px; }
.form-section { padding: 4px 0 8px; border-bottom: 1px solid #edf1f5; margin-bottom: 22px; }
.form-section h3 { font-size: 17px; margin-bottom: 18px; color: var(--color-text); }
.form-section:last-of-type { border-bottom: 0; margin-bottom: 8px; }
.map-form-item { flex-direction: column; align-items: stretch; }
.map-form-item :deep(.el-form-item__content) { display: block; width: 100%; margin-left: 0 !important; }
.submit-row { margin-bottom: 0; }

@media (max-width: 768px) {
  .form-card :deep(.el-card__body) { padding: 18px 16px; }
  .archive-create-page :deep(.el-form-item) { display: block; }
  .archive-create-page :deep(.el-form-item__label) { width: 100% !important; justify-content: flex-start; margin-bottom: 6px; }
  .archive-create-page :deep(.el-form-item__content) { margin-left: 0 !important; }
  .archive-create-page :deep(.el-input),
  .archive-create-page :deep(.el-select) { width: 100% !important; }
  .submit-row { position: sticky; bottom: 0; z-index: 5; padding: 12px 0 2px; background: #fff; border-top: 1px solid #edf1f5; }
  .submit-row :deep(.el-button) { width: 100%; }
}
</style>
