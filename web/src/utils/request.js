import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

let isRedirecting = false

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // Blob 响应（文件下载）直接返回
    if (res instanceof Blob) return response
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.clearToken()
      if (!isRedirecting) {
        isRedirecting = true
        ElMessage.warning('登录已过期，请重新登录')
        import('@/router').then(({ default: router }) => {
          router.push('/auth/login').finally(() => {
            isRedirecting = false
          })
        })
      }
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
