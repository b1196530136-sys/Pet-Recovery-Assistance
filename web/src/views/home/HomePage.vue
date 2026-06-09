<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-content">
        <span class="hero-kicker">社区协作 · 真实线索 · 温暖找回</span>
        <h1>让每一只走丢的宠物都能回家</h1>
        <p>整合寻宠启事、目击线索和流浪动物档案，让求助者和爱心人士更快建立连接。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/posts')">进入寻宠大厅</el-button>
          <el-button size="large" @click="$router.push('/archives')">浏览动物档案</el-button>
        </div>
      </div>
      <div class="hero-panel">
        <div class="hero-panel-title">今日可做</div>
        <button type="button" @click="$router.push('/posts/create')">
          <strong>发布寻宠</strong>
          <span>上传照片和丢失地点</span>
        </button>
        <button type="button" @click="$router.push('/posts')">
          <strong>提供线索</strong>
          <span>查看附近启事并联系失主</span>
        </button>
        <button type="button" @click="handleCreateArchive">
          <strong>登记档案</strong>
          <span>记录流浪动物状态</span>
        </button>
      </div>
    </section>

    <section class="stats">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon active">寻</div>
            <div class="stat-num">{{ stats.activePosts }}</div>
            <div class="stat-label">正在寻找</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon success">回</div>
            <div class="stat-num">{{ stats.resolvedPosts }}</div>
            <div class="stat-label">成功找回</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon archive">档</div>
            <div class="stat-num">{{ stats.totalArchives }}</div>
            <div class="stat-label">动物档案</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon user">助</div>
            <div class="stat-num">{{ stats.totalUsers }}</div>
            <div class="stat-label">注册用户</div>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="trust-banner">
      <div class="trust-image" aria-hidden="true"></div>
      <div class="trust-copy">
        <span>公益协作网络</span>
        <h2>把分散的照片、地点和线索整理成可跟进的信息流</h2>
        <p>平台面向失主、志愿者和认证救助人，帮助每条线索被看见、被记录、被继续追踪。</p>
      </div>
    </section>

    <section class="how-it-works">
      <h2>如何帮助宠物回家</h2>
      <el-row :gutter="40">
        <el-col :xs="24" :md="8">
          <div class="step-card">
            <div class="step-icon">1</div>
            <h3>发布寻宠启事</h3>
            <p>填写丢失宠物信息，地图标注位置，提交后台审核</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="step-card">
            <div class="step-icon">2</div>
            <h3>爱心人士提供线索</h3>
            <p>浏览启事详情，提交目击时间、地点和照片</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
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
import { useRouter } from 'vue-router'
import { statsApi } from '@/api/stats'
import { useUserStore } from '@/store/user'
import { alertArchiveCreateDenied, canCreateArchive } from '@/utils/archivePermission'

const router = useRouter()
const userStore = useUserStore()
const stats = ref({ activePosts: 0, resolvedPosts: 0, totalArchives: 0, totalUsers: 0 })

function handleCreateArchive() {
  if (!canCreateArchive(userStore)) {
    alertArchiveCreateDenied()
    return
  }
  router.push('/archives/create')
}

onMounted(async () => {
  try {
    const res = await statsApi.dashboard()
    stats.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped>
.home-page { padding-bottom: 12px; }
.hero { position: relative; min-height: 390px; display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 30px; align-items: center; padding: 56px; background: url('/images/hero-rescue.jpg') center/cover no-repeat; color: #fff; border-radius: var(--radius-lg); margin-bottom: 32px; overflow: hidden; box-shadow: var(--shadow-card); }
.hero::before { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg, rgba(10, 24, 42, 0.76), rgba(10, 24, 42, 0.44) 48%, rgba(10, 24, 42, 0.18)); }
.hero-content { position: relative; max-width: 640px; text-align: left; }
.hero-kicker { display: inline-flex; align-items: center; margin-bottom: 14px; padding: 6px 12px; border-radius: 999px; background: rgba(255,255,255,0.16); border: 1px solid rgba(255,255,255,0.28); font-size: 13px; font-weight: 700; }
.hero h1 { font-size: 44px; line-height: 1.18; margin-bottom: 14px; letter-spacing: 0; }
.hero p { max-width: 560px; font-size: 17px; line-height: 1.8; margin-bottom: 28px; opacity: 0.94; }
.hero-actions { display: flex; gap: 14px; justify-content: flex-start; flex-wrap: wrap; }
.hero-actions .el-button { min-width: 150px; }
.hero-panel { position: relative; display: grid; gap: 10px; padding: 18px; border-radius: 10px; background: rgba(255,255,255,0.94); color: var(--color-text); box-shadow: 0 16px 36px rgba(15, 23, 42, 0.18); }
.hero-panel-title { color: var(--color-muted); font-size: 13px; font-weight: 700; }
.hero-panel button { display: grid; gap: 3px; width: 100%; padding: 13px 14px; border: 1px solid #e7edf4; border-radius: 8px; background: #fff; color: inherit; text-align: left; cursor: pointer; transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease; }
.hero-panel button:hover { transform: translateY(-1px); border-color: #c9e4ff; box-shadow: var(--shadow-soft); }
.hero-panel strong { font-size: 15px; color: var(--color-text); }
.hero-panel span { font-size: 12px; color: var(--color-muted); }
.stats { margin-bottom: 42px; }
.stat-card { min-height: 142px; background: #fff; border: 1px solid var(--color-line); border-radius: var(--radius-md); padding: 22px; text-align: center; box-shadow: var(--shadow-soft); }
.stat-icon { width: 34px; height: 34px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; margin-bottom: 10px; font-weight: 700; font-size: 14px; }
.stat-icon.active { background: #e9f4ff; color: var(--color-primary); }
.stat-icon.success { background: #eaf8ee; color: var(--color-rescue); }
.stat-icon.archive { background: #fff6e6; color: var(--color-warning); }
.stat-icon.user { background: #eef2ff; color: #536dca; }
.stat-num { font-size: 34px; line-height: 1; font-weight: bold; color: var(--color-primary); }
.stat-label { font-size: 14px; color: var(--color-muted); margin-top: 8px; }
.trust-banner { display: grid; grid-template-columns: 38% 1fr; gap: 28px; align-items: center; margin-bottom: 42px; padding: 22px; border-radius: var(--radius-lg); background: #fff; border: 1px solid var(--color-line); box-shadow: var(--shadow-soft); }
.trust-image { min-height: 220px; border-radius: 8px; background: url('/images/shelter-community.jpg') center/cover no-repeat; }
.trust-copy span { color: var(--color-rescue); font-size: 13px; font-weight: 800; }
.trust-copy h2 { margin: 10px 0 12px; font-size: 25px; line-height: 1.35; }
.trust-copy p { color: var(--color-muted); font-size: 15px; line-height: 1.8; }
.how-it-works h2 { text-align: center; margin-bottom: 28px; font-size: 26px; }
.step-card { min-height: 184px; text-align: center; padding: 30px 22px; background: #fff; border: 1px solid var(--color-line); border-radius: var(--radius-md); box-shadow: var(--shadow-soft); }
.step-icon { width: 48px; height: 48px; border-radius: 50%; background: var(--color-primary); color: #fff; font-size: 20px; line-height: 48px; margin: 0 auto 16px; font-weight: bold; }
.step-card h3 { margin-bottom: 12px; font-size: 18px; }
.step-card p { color: var(--color-muted); font-size: 14px; line-height: 1.7; }

@media (max-width: 768px) {
  .hero { grid-template-columns: 1fr; min-height: auto; padding: 34px 20px; background-position: center; margin-bottom: 24px; }
  .hero::before { background: rgba(10, 24, 42, 0.58); }
  .hero-content { text-align: left; }
  .hero h1 { font-size: 30px; }
  .hero p { font-size: 15px; }
  .hero-actions { gap: 10px; }
  .hero-actions .el-button { flex: 1; min-width: 132px; }
  .hero-panel { padding: 14px; }
  .stats { margin-bottom: 32px; }
  .stat-card { min-height: 128px; padding: 18px 12px; }
  .stat-num { font-size: 30px; }
  .trust-banner { grid-template-columns: 1fr; padding: 16px; margin-bottom: 32px; }
  .trust-image { min-height: 190px; }
  .trust-copy h2 { font-size: 22px; }
  .how-it-works h2 { margin-bottom: 18px; font-size: 23px; }
  .step-card { min-height: auto; }
}
</style>
