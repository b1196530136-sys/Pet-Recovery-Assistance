import request from '@/utils/request'

export const postApi = {
  create(data) {
    return request.post('/post/create', data)
  },
  update(data) {
    return request.put('/post/update', data)
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
  clueTrail(postId) {
    return request.get(`/post/clue-trail/${postId}`)
  },
  /** 以图搜宠：上传图片调用阿里云API进行宠物相似度对比 */
  searchByImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/post/search-by-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,  // API调用可能较慢，设置30秒超时
    })
  },
  /**
   * 以图搜宠（图床中转模式）：传入已上传到图床的图片URL进行宠物相似度对比
   * 推荐使用此方式，先通过 /api/upload/image 上传图片获取URL，再调用此接口
   */
  searchByImageUrl(imageUrl) {
    return request.post('/post/search-by-image-url', { imageUrl }, {
      timeout: 30000,
    })
  },
}
