import request from '@/utils/request'

export const statsApi = {
  dashboard() {
    return request.get('/stats/dashboard')
  },
}
