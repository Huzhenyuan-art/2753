import axios, { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearAllTokens,
  shouldRefreshToken,
  isRefreshTokenExpired,
} from './token'

interface PendingRequest {
  resolve: (value: any) => void
  reject: (reason: any) => void
  config: InternalAxiosRequestConfig
}

const REFRESH_URL = '/user/refresh'
const REFRESH_THRESHOLD_MS = 5 * 60 * 1000

let isRefreshing = false
let pendingRequests: PendingRequest[] = []

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 10000,
})

function forceLogout(message: string = '登录已过期，请重新登录') {
  clearAllTokens()
  const currentPath = router.currentRoute.value.fullPath
  if (currentPath !== '/login') {
    ElMessage.error(message)
    router.push({ path: '/login', query: { redirect: currentPath } })
  }
}

async function doRefreshToken(): Promise<void> {
  const refreshToken = getRefreshToken()
  if (!refreshToken || isRefreshTokenExpired()) {
    throw new Error('REFRESH_TOKEN_EXPIRED')
  }

  const res = await axios.post(
    (import.meta.env.VITE_API_URL || '/api') + REFRESH_URL,
    { refreshToken },
    { timeout: 10000 }
  )

  const data = res.data
  if (data.code === 200 && data.data) {
    const { token, refreshToken: newRefreshToken, accessTokenExpiresIn } = data.data
    setTokens(token, newRefreshToken, accessTokenExpiresIn)
  } else {
    const err: any = new Error(data.message || 'REFRESH_FAILED')
    err.code = data.code
    err.businessError = true
    throw err
  }
}

function flushPendingRequests(error: any | null) {
  const requests = [...pendingRequests]
  pendingRequests = []
  isRefreshing = false

  if (error) {
    requests.forEach((req) => req.reject(error))
  } else {
    const newToken = getAccessToken()
    requests.forEach((req) => {
      if (newToken) {
        req.config.headers['Authorization'] = `Bearer ${newToken}`
      }
      instance(req.config).then(req.resolve).catch(req.reject)
    })
  }
}

instance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const url = config.url || ''
    const isRefreshRequest = url.includes(REFRESH_URL)

    if (!isRefreshRequest) {
      const accessToken = getAccessToken()
      if (accessToken) {
        if (shouldRefreshToken(REFRESH_THRESHOLD_MS)) {
          if (!isRefreshing) {
            isRefreshing = true
            try {
              await doRefreshToken()
              const newToken = getAccessToken()
              if (newToken) {
                config.headers['Authorization'] = `Bearer ${newToken}`
              }
              flushPendingRequests(null)
            } catch (err: any) {
              flushPendingRequests(err)
              forceLogout(err.message || '登录已过期，请重新登录')
              return Promise.reject(err)
            }
          } else {
            return new Promise<any>((resolve, reject) => {
              pendingRequests.push({ resolve, reject, config })
            })
          }
        } else {
          config.headers['Authorization'] = `Bearer ${accessToken}`
        }
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    const res = response.data
    const skipErrorToast = (response.config as any).skipErrorToast

    if (res.code !== 200) {
      if (!skipErrorToast) {
        ElMessage.error(res.message || 'Error')
      }
      if (res.code === 401 || res.code === 403) {
        forceLogout(res.message || '登录已过期，请重新登录')
      }
      const err: any = new Error(res.message || 'Error')
      err.code = res.code
      err.type = 'business'
      err.data = res
      return Promise.reject(err)
    }
    return res
  },
  async (error) => {
    const originalConfig = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    const skipErrorToast = (error.config as any)?.skipErrorToast

    if (error.response) {
      const status = error.response.status

      if (status === 401 && !originalConfig?._retry) {
        const url = originalConfig?.url || ''
        if (!url.includes(REFRESH_URL)) {
          if (!isRefreshing) {
            originalConfig._retry = true
            isRefreshing = true
            try {
              await doRefreshToken()
              const newToken = getAccessToken()
              if (newToken && originalConfig.headers) {
                originalConfig.headers['Authorization'] = `Bearer ${newToken}`
              }
              flushPendingRequests(null)
              return instance(originalConfig)
            } catch (err: any) {
              flushPendingRequests(err)
              forceLogout(err.message || '登录已过期，请重新登录')
              return Promise.reject(err)
            }
          } else {
            return new Promise<any>((resolve, reject) => {
              pendingRequests.push({ resolve, reject, config: originalConfig })
            })
          }
        }
      }

      if (!skipErrorToast) {
        if (status === 413) {
          ElMessage.error('上传的文件大小超出服务器限制，请选择小于20MB的图片后重试')
        } else if (status === 401 || status === 403) {
          ElMessage.error('登录已过期，请重新登录')
        } else if (status === 404) {
          ElMessage.error('请求的资源不存在')
        } else if (status === 500) {
          ElMessage.error('服务器内部错误，请稍后重试')
        } else if (status === 502 || status === 503) {
          ElMessage.error('服务暂时不可用，请稍后重试')
        } else {
          ElMessage.error(error.response.data?.message || '请求失败，请稍后重试')
        }
      }
      if (status === 401 || status === 403) {
        forceLogout('登录已过期，请重新登录')
      }
      error.type = 'http'
      error.status = status
    } else if (error.code === 'ECONNABORTED') {
      if (!skipErrorToast) {
        ElMessage.error('请求超时，请检查网络后重试')
      }
      error.type = 'timeout'
    } else {
      if (!skipErrorToast) {
        ElMessage.error('网络连接异常，请检查网络设置')
      }
      error.type = 'network'
    }
    return Promise.reject(error)
  }
)

export default instance
