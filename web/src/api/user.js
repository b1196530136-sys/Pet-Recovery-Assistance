import request from '@/utils/request'

export const userApi = {
  register(data) {
    return request.post('/user/register', data)
  },
  login(data) {
    return request.post('/user/login', data)
  },
  loginByCode(data) {
    return request.post('/user/login/code', data)
  },
  getProfile() {
    return request.get('/user/profile')
  },
  applyCertification(credentials) {
    return request.post('/user/apply-certification', { credentials })
  },
  getUserInfo(id) {
    return request.get(`/user/info/${id}`)
  },
  updateAvatar(avatar) {
    return request.post('/user/update-avatar', { avatar })
  },
  updatePhone(phone) {
    return request.post('/user/update-phone', { phone })
  },
}
