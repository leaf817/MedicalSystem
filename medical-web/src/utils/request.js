import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
})

request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value?.fullPath } })
        return Promise.reject(new Error(res.message || '未登录'))
      }
      if (res.code === 400) {
        ElMessage.warning(res.message || '操作提示')
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    if (error.response?.status === 401) {
      router.replace({ path: '/login', query: { redirect: router.currentRoute.value?.fullPath } })
    } else if (error.response?.status === 403) {
      ElMessage.warning('权限不足')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
