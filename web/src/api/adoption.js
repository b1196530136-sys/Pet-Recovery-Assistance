import request from '@/utils/request'

export const adoptionApi = {
  create(data) {
    return request.post('/adoption/create', data)
  },
  incoming() {
    return request.get('/adoption/incoming')
  },
  my() {
    return request.get('/adoption/my')
  },
  incomingFrom(applicantId) {
    return request.get(`/adoption/incoming-from/${applicantId}`)
  },
  records() {
    return request.get('/adoption/records')
  },
  review(id, action) {
    return request.post(`/adoption/review/${id}`, null, { params: { action } })
  },
  updateFollowUp(id, status) {
    return request.post(`/adoption/record/${id}/follow-up`, null, { params: { status } })
  },
}
