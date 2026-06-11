import AMapLoader from '@amap/amap-jsapi-loader'

const amapKey = import.meta.env.VITE_AMAP_KEY
const amapSecurityCode = import.meta.env.VITE_AMAP_SECURITY_CODE

export function loadAmap(options = {}) {
  if (!amapKey) {
    return Promise.reject(new Error('未配置 VITE_AMAP_KEY'))
  }
  if (amapSecurityCode) {
    window._AMapSecurityConfig = { securityJsCode: amapSecurityCode }
  }
  return AMapLoader.load({
    key: amapKey,
    version: '2.0',
    ...options,
  })
}
