import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isCertified = computed(() => userInfo.value?.role === 'CERTIFIED' || userInfo.value?.role === 'ADMIN')

  function setToken(t) {
    token.value = t
    sessionStorage.setItem('token', t)
  }

  function clearToken() {
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  function restoreSession() {
    const t = sessionStorage.getItem('token')
    if (t) token.value = t
    const u = sessionStorage.getItem('userInfo')
    if (u) {
      try {
        userInfo.value = JSON.parse(u)
      } catch {
      }
    }
  }

  async function fetchProfile() {
    try {
      const res = await userApi.getProfile()
      userInfo.value = res.data
      sessionStorage.setItem('userInfo', JSON.stringify(res.data))
    } catch {
    }
  }

  async function login(credentials) {
    const res = await userApi.login(credentials)
    setToken(res.data.token)
    userInfo.value = res.data.user
    sessionStorage.setItem('userInfo', JSON.stringify(res.data.user))
  }

  async function loginByCode(data) {
    const res = await userApi.loginByCode(data)
    setToken(res.data.token)
    userInfo.value = res.data.user
    sessionStorage.setItem('userInfo', JSON.stringify(res.data.user))
  }

  function logout() {
    clearToken()
  }

  return { token, userInfo, isLoggedIn, isAdmin, isCertified, setToken, clearToken, restoreSession, fetchProfile, login, loginByCode, logout }
})
