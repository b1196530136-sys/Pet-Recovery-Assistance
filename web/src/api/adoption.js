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
  review(id, action) {
    return request.post(`/adoption/review/${id}`, null, { params: { action } })
  },
}
