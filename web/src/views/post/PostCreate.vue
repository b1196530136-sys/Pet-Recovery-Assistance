<template>
  <div class="post-create-page" style="max-width: 950px; margin: 0 auto">
    <h2 style="margin-bottom: 20px">发布寻宠启事</h2>
    <el-card>
      <el-form :model="form" label-width="100px">
        <el-form-item label="宠物类型" required>
          <el-radio-group v-model="form.petType">
            <el-radio value="cat">猫</el-radio>
            <el-radio value="dog">狗</el-radio>
            <el-radio value="other">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="品种">
          <el-input v-model="form.breed" placeholder="如已知品种请填写" />
        </el-form-item>
        <el-form-item label="宠物昵称">
          <el-input v-model="form.petName" placeholder="宠物的名字" />
        </el-form-item>
        <el-form-item label="照片">
          <el-upload
            action="/api/upload/image"
            list-type="picture-card"
            :headers="uploadHeaders"
            name="file"
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
        <el-form-item label="丢失地点" required>
          <AmapPicker v-model="mapLocation" />
        </el-form-item>
        <el-form-item label="酬金说明">
          <el-input v-model="form.reward" placeholder="可选" />
        </el-form-item>
        <el-form-item label="特征描述" required>
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述宠物的外形特征、性格特点等" />
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
import { postApi } from '@/api/post'
import { useUserStore } from '@/store/user'
import AmapPicker from '@/components/map/AmapPicker.vue'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ petType: 'cat', breed: '', petName: '', lostTime: '', reward: '', description: '', photos: '' })
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
  await postApi.create(form)
  ElMessage.success('提交成功，请等待后台审核')
  router.push('/posts')
}
</script>
