import request from '@/utils/request'

export const archiveApi = {
  create(data) {
    return request.post('/archive/create', data)
  },
  search(params) {
    return request.get('/archive/search', { params })
  },
  detail(id) {
    return request.get(`/archive/detail/${id}`)
  },
  pending(params) {
    return request.get('/archive/pending', { params })
  },
}
