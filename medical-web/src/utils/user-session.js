/**
 * 从 sessionStorage 读取登录用户信息（与 AppHeader / 登录页写入结构一致）
 */
export function getUserInfoFromStorage() {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
}

export function getUserDisplayName(userInfo) {
  const u = userInfo || getUserInfoFromStorage()
  return u.name || u.username || '患'
}

export function getUserAvatarInitial(userInfo) {
  const name = getUserDisplayName(userInfo)
  return name.charAt(0).toUpperCase()
}
