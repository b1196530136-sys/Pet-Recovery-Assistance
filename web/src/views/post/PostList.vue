<template>
  <div class="post-list-page">
    <h2 style="margin-bottom: 20px">寻宠大厅</h2>

    <!-- 搜索筛选 -->
    <el-card class="search-bar" style="margin-bottom: 20px">
      <el-form :model="filters" inline>
        <el-form-item label="宠物类型">
          <el-select v-model="filters.petType" placeholder="全部" clearable style="width: 120px">
            <el-option label="猫" value="cat" />
            <el-option label="狗" value="dog" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="filters.province" placeholder="省/市" style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-row :gutter="16">
      <el-col v-for="post in posts" :key="post.id" :span="8" style="margin-bottom: 16px">
        <el-card class="post-card" @click="$router.push(`/posts/${post.id}`)" style="cursor: pointer">
          <div class="post-status" :class="post.status">{{ statusMap[post.status] }}</div>
          <h3>{{ post.petName || ' unnamed' }} ({{ typeMap[post.petType] }})</h3>
          <p style="font-size: 13px; color: #909399; margin-top: 8px">
            {{ post.address }} · {{ formatDate(post.lostTime) }}
          </p>
          <p style="font-size: 13px; color: #606266; margin-top: 4px">
            {{ post.description?.slice(0, 60) }}...
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!posts.length" description="暂无寻宠启事" />

    <div class="pagination" style="margin-top: 20px; text-align: center">
      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        layout="prev, pager, next"
        @current-change="search"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { postApi } from '@/api/post'

const posts = ref([])
const page = ref(1)
const total = ref(0)
const filters = ref({ petType: '', province: '' })

const statusMap = { PENDING: '待审核', ACTIVE: '寻找中', REJECTED: '已驳回', RESOLVED: '已找到' }
const typeMap = { cat: '猫', dog: '狗', other: '其他' }

function formatDate(d) {
  return d ? d.slice(0, 10) : ''
}

async function search() {
  const res = await postApi.search({ ...filters.value, page: page.value, size: 20, status: 'ACTIVE' })
  posts.value = res.data.records
  total.value = res.data.total
}

onMounted(search)
</script>

<style scoped>
.post-card { position: relative; }
.post-status { position: absolute; top: 12px; right: 12px; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.post-status.ACTIVE { background: #e6f7ff; color: #1890ff; }
.post-status.RESOLVED { background: #f6ffed; color: #52c41a; }
.post-status.REJECTED { background: #fff2f0; color: #ff4d4f; }
.post-status.PENDING { background: #fffbe6; color: #faad14; }
</style>
