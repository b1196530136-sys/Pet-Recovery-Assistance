import request from '@/utils/request'

export const archiveApi = {
  create(data) {
    return request.post('/archive/create', data)
  },
  update(data) {
    return request.put('/archive/update', data)
  },
  delete(id) {
    return request.delete(`/archive/delete/${id}`)
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
