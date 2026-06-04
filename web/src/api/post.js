import request from '@/utils/request'

export const postApi = {
  create(data) {
    return request.post('/post/create', data)
  },
  search(params) {
    return request.post('/post/search', params)
  },
  detail(id) {
    return request.get(`/post/detail/${id}`)
  },
  resolve(id) {
    return request.post(`/post/resolve/${id}`)
  },
  my() {
    return request.get('/post/my')
  },
  clued() {
    return request.get('/post/clued')
  },
  delete(id) {
    return request.post(`/post/delete/${id}`)
  },
}
