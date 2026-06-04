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
          <el-upload action="#" list-type="picture-card" :auto-upload="false">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="丢失时间" required>
          <el-date-picker v-model="form.lostTime" type="datetime" placeholder="选择丢失时间" />
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
import { reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import AmapPicker from '@/components/map/AmapPicker.vue'

const router = useRouter()
const form = reactive({ petType: 'cat', breed: '', petName: '', lostTime: '', reward: '', description: '' })
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
