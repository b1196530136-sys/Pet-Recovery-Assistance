<template>
  <div class="not-found">
    <div class="not-found-mark">寻</div>
    <h1>404</h1>
    <p>页面未找到，可能链接已经失效或地址输入有误。</p>
    <div class="not-found-actions">
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      <el-button @click="$router.push('/posts')">去寻宠大厅</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 68vh; text-align: center; padding: 40px 16px; }
.not-found-mark { width: 54px; height: 54px; display: flex; align-items: center; justify-content: center; border-radius: 50%; margin-bottom: 16px; background: #e9f4ff; color: var(--color-primary); font-size: 24px; font-weight: 800; box-shadow: inset 0 0 0 1px #c9e4ff; }
.not-found h1 { font-size: 80px; line-height: 1; color: var(--color-primary); margin-bottom: 16px; }
.not-found p { max-width: 360px; font-size: 16px; color: var(--color-muted); line-height: 1.8; margin-bottom: 24px; }
.not-found-actions { display: flex; gap: 12px; flex-wrap: wrap; justify-content: center; }
</style>
