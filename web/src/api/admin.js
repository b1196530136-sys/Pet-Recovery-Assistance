import request from '@/utils/request'

export const adminApi = {
  // 寻宠启事审核
  reviewPost(postId, action, reason) {
    return request.post(`/admin/post/review/${postId}`, action, { params: { reason } })
  },
  // 档案审核
  reviewArchive(archiveId, action, reason) {
    return request.post(`/admin/archive/review/${archiveId}`, action, { params: { reason } })
  },
  // 认证审批
  reviewCertification(userId, action) {
    return request.post(`/admin/certification/review/${userId}`, action)
  },
  // 数据大盘
  dashboard() {
    return request.get('/admin/dashboard')
  },
}
