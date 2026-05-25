import request from '@/utils/request'

/**
 * 修改当前登录用户密码（全角色）
 */
export function changePassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}
