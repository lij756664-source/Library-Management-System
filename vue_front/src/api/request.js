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

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (!response.config.silentError) ElMessage.error(res.message || '请求失败')
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
    const message = error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
      ? '请求超时，请稍后重试'
      : !error.response ? '网络连接异常，请检查连接后重试' : '服务暂不可用，请稍后重试'
    if (!error.config?.silentError) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
