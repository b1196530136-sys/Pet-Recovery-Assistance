<template>
  <div class="archive-detail-page" style="max-width: 900px; margin: 0 auto">
    <el-card v-if="archive">
      <h2>{{ typeMap[archive.animalType] }} · 电子档案</h2>
      <el-descriptions :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="动物类型">{{ typeMap[archive.animalType] }}</el-descriptions-item>
        <el-descriptions-item label="安置状态">{{ placementMap[archive.placementStatus] }}</el-descriptions-item>
        <el-descriptions-item label="健康状况">{{ archive.healthStatus || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="绝育/免疫">{{ archive.neuteredStatus || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="发现地点" :span="2">{{ archive.address }}</el-descriptions-item>
      </el-descriptions>
      <p style="margin-top: 16px">{{ archive.description }}</p>
      <div ref="mapContainer" style="width: 100%; height: 300px; margin-top: 20px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { archiveApi } from '@/api/archive'
import AMapLoader from '@amap/amap-jsapi-loader'

const route = useRoute()
const archive = ref(null)
const mapContainer = ref(null)
const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const placementMap = { observing: '原地观察', sheltered: '基地收容', adoptable: '开放领养' }

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
