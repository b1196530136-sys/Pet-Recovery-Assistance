import { ElMessageBox } from 'element-plus'

export const ARCHIVE_CREATE_DENY_MESSAGE = '权限不足，需要成为认证用户'

export function canCreateArchive(userStore) {
  return userStore.userInfo?.role === 'CERTIFIED' || userStore.userInfo?.role === 'ADMIN'
}

export function alertArchiveCreateDenied() {
  return ElMessageBox.alert(ARCHIVE_CREATE_DENY_MESSAGE, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
  })
}

