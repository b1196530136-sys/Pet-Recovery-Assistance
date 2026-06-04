<template>
  <div class="home-page">
    <section class="hero">
      <h1>让每一只走丢的宠物都能回家</h1>
      <p>全网寻宠互助平台 · 隐私线索直达失主</p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="$router.push('/posts')">进入寻宠大厅</el-button>
        <el-button size="large" @click="$router.push('/archives')">浏览动物档案</el-button>
      </div>
    </section>

    <section class="stats">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-num">{{ stats.activePosts }}</div>
            <div class="stat-label">正在寻找</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-num">{{ stats.resolvedPosts }}</div>
            <div class="stat-label">成功找回</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-num">{{ stats.totalArchives }}</div>
            <div class="stat-label">动物档案</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-num">{{ stats.totalUsers }}</div>
            <div class="stat-label">注册用户</div>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="how-it-works">
      <h2>如何帮助宠物回家</h2>
      <el-row :gutter="40">
        <el-col :span="8">
          <div class="step-card">
            <div class="step-icon">1</div>
            <h3>发布寻宠启事</h3>
            <p>填写丢失宠物信息，地图标注位置，提交后台审核</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="step-card">
            <div class="step-icon">2</div>
            <h3>爱心人士提供线索</h3>
            <p>浏览启事详情，提交目击时间、地点和照片</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="step-card">
            <div class="step-icon">3</div>
            <h3>私信核实·成功找回</h3>
            <p>线索通过站内私信直达失主，在线沟通确认后结案</p>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { statsApi } from '@/api/stats'

const stats = ref({ activePosts: 0, resolvedPosts: 0, totalArchives: 0, totalUsers: 0 })

onMounted(async () => {
  try {
    const res = await statsApi.dashboard()
    stats.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped>
.hero { text-align: center; padding: 80px 20px; background: linear-gradient(135deg, #409eff 0%, #337ecc 100%); color: #fff; border-radius: 12px; margin-bottom: 40px; }
.hero h1 { font-size: 36px; margin-bottom: 12px; }
.hero p { font-size: 16px; margin-bottom: 30px; opacity: 0.9; }
.hero-actions { display: flex; gap: 16px; justify-content: center; }
.stats { margin-bottom: 40px; }
.stat-card { background: #fff; border-radius: 8px; padding: 24px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.stat-num { font-size: 36px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.how-it-works h2 { text-align: center; margin-bottom: 40px; font-size: 24px; }
.step-card { text-align: center; padding: 30px 20px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.step-icon { width: 48px; height: 48px; border-radius: 50%; background: #409eff; color: #fff; font-size: 20px; line-height: 48px; margin: 0 auto 16px; font-weight: bold; }
.step-card h3 { margin-bottom: 12px; }
.step-card p { color: #909399; font-size: 14px; }
</style>
