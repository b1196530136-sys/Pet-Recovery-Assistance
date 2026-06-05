<template>
  <div style="height: calc(100vh - 100px); margin: -20px;">
    <iframe :src="src" style="width: 100%; height: 100%; border: none;" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const pathMap = { AdminFrontPosts: '/posts', AdminFrontArchives: '/archives' }
const src = computed(() => window.location.origin + (pathMap[route.name] || '/'))
</script>
