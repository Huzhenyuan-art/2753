import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 10000
})

// Request interceptor
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Error')
      if (res.code === 401 || res.code === 403) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 413) {
        ElMessage.error('上传的文件大小超出服务器限制，请选择小于20MB的图片后重试')
      } else if (status === 401 || status === 403) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        router.push('/login')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else if (status === 502 || status === 503) {
        ElMessage.error('服务暂时不可用，请稍后重试')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败，请稍后重试')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络后重试')
    } else {
      ElMessage.error('网络连接异常，请检查网络设置')
    }
    return Promise.reject(error)
  }
)

export default request
