<template>
  <div class="post-create-page form-page">
    <h2 class="page-title">发布寻宠启事</h2>
    <el-card class="form-card">
      <el-form :model="form" label-width="100px">
        <section class="form-section">
          <h3>基础信息</h3>
          <el-form-item label="宠物类型" required>
            <el-radio-group v-model="form.petType">
              <el-radio value="cat">猫</el-radio>
              <el-radio value="dog">狗</el-radio>
              <el-radio value="other">其他</el-radio>
            </el-radio-group>
            <el-input v-if="form.petType === 'other'" v-model="form.customPetType" placeholder="请填写具体类型" style="width: 200px; margin-top: 8px;" />
          </el-form-item>
          <el-form-item label="品种">
            <el-input v-model="form.breed" placeholder="如已知品种请填写" />
          </el-form-item>
          <el-form-item label="宠物昵称">
            <el-input v-model="form.petName" placeholder="宠物的名字" />
          </el-form-item>
        </section>

        <section class="form-section">
          <h3>照片与时间</h3>
          <el-form-item label="照片">
            <el-upload
              action="/api/upload/image"
              list-type="picture-card"
              :headers="uploadHeaders"
              name="file"
              accept="image/*"
              :on-success="onUploadSuccess"
              :on-remove="onUploadRemove"
              :before-upload="beforeUpload"
            >
              <div style="display: flex; flex-direction: column; align-items: center;">
                <el-icon><Plus /></el-icon>
                <span style="font-size: 12px; color: #909399; margin-top: 4px;">点击上传照片</span>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="丢失时间" required>
            <el-date-picker
              v-model="form.lostTime"
              type="datetime"
              placeholder="选择丢失时间"
              :disabled-date="disabledFutureDate"
            />
          </el-form-item>
        </section>

        <section class="form-section">
          <h3>丢失位置</h3>
          <el-form-item label="丢失地点" required class="map-form-item">
            <AmapPicker v-model="mapLocation" />
          </el-form-item>
        </section>

        <section class="form-section">
          <h3>补充信息</h3>
          <el-form-item label="酬金说明">
            <el-input v-model="form.reward" placeholder="可选" />
          </el-form-item>
          <el-form-item label="特征描述" required>
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述宠物的外形特征、性格特点等" />
          </el-form-item>
        </section>

        <el-form-item class="submit-row">
          <el-button type="primary" size="large" @click="submit">提交审核</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { postApi } from '@/api/post'
import { useUserStore } from '@/store/user'
import AmapPicker from '@/components/map/AmapPicker.vue'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ petType: 'cat', customPetType: '', breed: '', petName: '', lostTime: '', reward: '', description: '', photos: '', longitude: '', latitude: '', address: '' })
const photoUrls = reactive([])

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

function onUploadSuccess(response) {
  if (response.code === 200 && response.data) {
    photoUrls.push(response.data)
    form.photos = photoUrls.join(',')
  }
}

function onUploadRemove() {
  // 重新从当前上传列表构建photoUrls（简化处理）
  photoUrls.pop()
  form.photos = photoUrls.join(',')
}

function disabledFutureDate(time) {
  return time.getTime() > Date.now()
}

const mapLocation = reactive({ lng: '', lat: '', address: '' })

watch(mapLocation, (val) => {
  form.longitude = val.lng
  form.latitude = val.lat
  form.address = val.address
}, { deep: true })

async function submit() {
  form.longitude = mapLocation.lng
  form.latitude = mapLocation.lat
  form.address = mapLocation.address
  if (form.petType === 'other' && form.customPetType) form.petType = form.customPetType
  await postApi.create(form)
  ElMessage.success('提交成功，请等待后台审核')
  router.push('/posts')
}
</script>

<style scoped>
.form-page { max-width: 950px; margin: 0 auto; }
.form-card :deep(.el-card__body) { padding: 28px 32px; }
.form-section { padding: 4px 0 8px; border-bottom: 1px solid #edf1f5; margin-bottom: 22px; }
.form-section h3 { font-size: 17px; margin-bottom: 18px; color: var(--color-text); }
.form-section:last-of-type { border-bottom: 0; margin-bottom: 8px; }
.map-form-item { flex-direction: column; align-items: stretch; }
.map-form-item :deep(.el-form-item__content) { display: block; width: 100%; margin-left: 0 !important; }
.submit-row { margin-bottom: 0; }

@media (max-width: 768px) {
  .form-card :deep(.el-card__body) { padding: 18px 16px; }
  .post-create-page :deep(.el-form-item) { display: block; }
  .post-create-page :deep(.el-form-item__label) { width: 100% !important; justify-content: flex-start; margin-bottom: 6px; }
  .post-create-page :deep(.el-form-item__content) { margin-left: 0 !important; }
  .post-create-page :deep(.el-input),
  .post-create-page :deep(.el-select),
  .post-create-page :deep(.el-date-editor) { width: 100% !important; }
  .submit-row { position: sticky; bottom: 0; z-index: 5; padding: 12px 0 2px; background: #fff; border-top: 1px solid #edf1f5; }
  .submit-row :deep(.el-button) { width: 100%; }
}
</style>
