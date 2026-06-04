<template>
  <div class="archive-create-page" style="max-width: 800px; margin: 0 auto">
    <h2 style="margin-bottom: 20px">登记流浪动物档案</h2>
    <el-alert title="仅认证用户可以登记动物档案" type="warning" :closable="false" show-icon style="margin-bottom: 20px" />
    <el-card>
      <el-form :model="form" label-width="120px">
        <el-form-item label="动物类型" required>
          <el-radio-group v-model="form.animalType">
            <el-radio value="cat">猫</el-radio>
            <el-radio value="dog">狗</el-radio>
            <el-radio value="other">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发现位置" required>
          <AmapPicker v-model="mapLocation" />
        </el-form-item>
        <el-form-item label="健康状况">
          <el-input v-model="form.healthStatus" placeholder="如: 良好/受伤/体弱" />
        </el-form-item>
        <el-form-item label="绝育/免疫">
          <el-input v-model="form.neuteredStatus" placeholder="如: 已绝育/已免疫" />
        </el-form-item>
        <el-form-item label="安置状态" required>
          <el-select v-model="form.placementStatus">
            <el-option label="原地流浪观察" value="observing" />
            <el-option label="救助基地收容" value="sheltered" />
            <el-option label="已开放领养" value="adoptable" />
          </el-select>
        </el-form-item>
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
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
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
import { archiveApi } from '@/api/archive'
import { useUserStore } from '@/store/user'
import AmapPicker from '@/components/map/AmapPicker.vue'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ animalType: 'cat', healthStatus: '', neuteredStatus: '', placementStatus: 'observing', description: '', photos: '' })
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
  await archiveApi.create(form)
  ElMessage.success('提交成功，请等待后台审核')
  router.push('/archives')
}
</script>
