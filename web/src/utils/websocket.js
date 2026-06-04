let ws = null
let reconnectTimer = null
const listeners = new Set()

export function initWebSocket(token) {
  if (ws && ws.readyState === WebSocket.OPEN) return

  ws = new WebSocket(`ws://${location.host}/ws/chat?token=${token}`)

  ws.onopen = () => {
    console.log('[WS] Connected')
    clearTimeout(reconnectTimer)
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      listeners.forEach(fn => fn(data))
    } catch { /* ignore */ }
  }

  ws.onclose = () => {
    console.log('[WS] Disconnected, reconnecting in 5s...')
    reconnectTimer = setTimeout(() => {
      import('@/store/user').then(({ useUserStore }) => {
        const userStore = useUserStore()
        if (userStore.token) initWebSocket(userStore.token)
      })
    }, 5000)
  }

  ws.onerror = () => { ws?.close() }
}

export function closeWebSocket() {
  clearTimeout(reconnectTimer)
  ws?.close()
  ws = null
}

export function onMessage(callback) {
  listeners.add(callback)
  return () => listeners.delete(callback)
}
