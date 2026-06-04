import request from '@/utils/request'

export const adminApi = {
  // 寻宠启事审核
  reviewPost(postId, action, reason) {
    return request.post(`/admin/post/review/${postId}`, null, { params: { action, reason } })
  },
  // 档案审核
  reviewArchive(archiveId, action, reason) {
    return request.post(`/admin/archive/review/${archiveId}`, null, { params: { action, reason } })
  },
  // 认证审批
  reviewCertification(userId, action) {
    return request.post(`/admin/certification/review/${userId}`, null, { params: { action } })
  },
  // 数据大盘
  dashboard() {
    return request.get('/admin/dashboard')
  },
}
