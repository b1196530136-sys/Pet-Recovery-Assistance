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
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="item in list" :key="item.id" :span="8" style="margin-bottom: 16px">
        <el-card @click="$router.push(`/archives/${item.id}`)" style="cursor: pointer">
          <h3>{{ typeMap[item.animalType] }}</h3>
          <p style="font-size: 13px; color: #909399; margin-top: 8px">
            {{ item.address }} · {{ placementMap[item.placementStatus] }}
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!list.length" description="暂无档案" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { archiveApi } from '@/api/archive'

const list = ref([])
const filters = ref({ animalType: '', placementStatus: '' })
const typeMap = { cat: '猫', dog: '狗', other: '其他' }
const placementMap = { observing: '原地观察', sheltered: '基地收容', adoptable: '开放领养' }

async function search() {
  const res = await archiveApi.search({ ...filters.value, page: 1, size: 20 })
  list.value = res.data.records
}

onMounted(search)
</script>
