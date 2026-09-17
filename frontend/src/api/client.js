import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const payload = error.response?.data
    const msg = payload?.message || error.message || '请求失败'
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default api
