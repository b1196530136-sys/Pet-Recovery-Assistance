import request from '@/utils/request'

export const messageApi = {
  send(data) {
    return request.post('/im/send', data)
  },
  conversation(otherUserId) {
    return request.get(`/im/conversation/${otherUserId}`)
  },
  conversations() {
    return request.get('/im/conversations')
  },
  systemMessages() {
    return request.get('/im/system')
  },
  unread() {
    return request.get('/im/unread')
  },
  contact(otherUserId) {
    return request.get(`/im/contact/${otherUserId}`)
  },
  meta(otherUserId) {
    return request.get(`/im/meta/${otherUserId}`)
  },
  markRead(messageId) {
    return request.post(`/im/read/${messageId}`)
  },
  block(data) {
    return request.post('/im/block', data)
  },
  unblock(blockedUserId) {
    return request.post(`/im/unblock/${blockedUserId}`)
  },
  report(data) {
    return request.post('/im/report', data)
  },
}
