<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { initWebSocket } from '@/utils/websocket'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      await userStore.fetchProfile()
      initWebSocket(userStore.token)
    } catch {
      userStore.clearToken()
    }
  }
})
</script>
