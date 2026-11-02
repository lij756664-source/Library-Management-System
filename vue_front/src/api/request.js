import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 统一把异常转换为面向用户的友好提示，避免把底层错误信息暴露给用户 */
function resolveErrorMessage(error) {
  if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
    return '请求超时，请检查网络后重试'
  }
  if (!error.response) {
    return '网络连接异常，请稍后重试'
  }
  const status = error.response.status
  if (status === 401) return '登录已过期，请重新登录'
  if (status === 403) return /\/(books|categories)(?:\/|$|\?)/.test(error.config?.url || '')
    ? '无权限执行该操作，仅图书管理员可维护馆藏数据' : '无权限执行该操作'
  if (status === 404) return '请求的资源不存在'
  if (status >= 500) return '服务器繁忙，请稍后重试'
  return '请求失败，请稍后重试'
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (!response.config.silentError) {
        const catalogRequest = /\/(books|categories)(?:\/|$|\?)/.test(response.config.url || '')
        const message = res.code === 403 && catalogRequest
          ? `${res.message || '无权限访问'}，仅图书管理员可维护馆藏数据`
          : res.message || '请求失败'
        ElMessage.error(message)
      }

      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
      }
      const error = new Error(res.message || '请求失败')
      error.businessCode = res.code
      return Promise.reject(error)
    }
    return res
  },
  (error) => {
    if (!error.config?.silentError) {
      ElMessage.error(error.response?.data?.message || resolveErrorMessage(error))
    }

    return Promise.reject(error)
  }
)

export default request
