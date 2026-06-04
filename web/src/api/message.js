import request from '@/utils/request'

export const messageApi = {
  send(data) {
    return request.post('/im/send', data)
  },
  conversation(otherUserId) {
    return request.get(`/im/conversation/${otherUserId}`)
  },
  unread() {
    return request.get('/im/unread')
  },
  markRead(messageId) {
    return request.post(`/im/read/${messageId}`)
  },
}
