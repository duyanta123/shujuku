import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
})

let refreshPromise = null

function isAuthEndpoint(config) {
  const url = config?.url || ''
  return url.includes('/auth/refresh')
    || url.includes('/student/login')
    || url.includes('/teacher/login')
    || url.includes('/admin/login')
}

function shouldSkipAuthRefresh(config) {
  return Boolean(config?.skipAuthRefresh)
}

function shouldSkipErrorMessage(config) {
  return Boolean(config?.skipErrorMessage)
}

function shouldSkipAuthRedirect(config) {
  return Boolean(config?.skipAuthRedirect)
}

function showError(config, message) {
  if (!shouldSkipErrorMessage(config)) {
    ElMessage.error(message)
  }
}

async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = axios.post('/api/auth/refresh', {}, { withCredentials: true })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

async function clearSessionAndRedirect() {
  localStorage.removeItem('user')
  try {
    const { default: userStore } = await import('@/stores/userStore')
    userStore.reset()
  } catch {
    // Route cleanup still proceeds if the store cannot be loaded.
  }
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

request.interceptors.request.use(
  config => {
    config.withCredentials = true
    if (config.headers) {
      delete config.headers.Authorization
      delete config.headers.authorization
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => response.data,
  async error => {
    const status = error.response?.status
    const originalConfig = error.config || {}

    if (
      status === 401
      && !originalConfig._retry
      && !isAuthEndpoint(originalConfig)
      && !shouldSkipAuthRefresh(originalConfig)
    ) {
      originalConfig._retry = true
      try {
        await refreshSession()
        return request(originalConfig)
      } catch (refreshError) {
        showError(originalConfig, '登录已过期，请重新登录')
        await clearSessionAndRedirect()
        return Promise.reject(refreshError)
      }
    }

    if (status === 401) {
      if (shouldSkipAuthRedirect(originalConfig)) {
        return Promise.reject(error)
      }
      await clearSessionAndRedirect()
    } else if (status === 403) {
      showError(originalConfig, '没有权限执行此操作')
    } else if (status === 423) {
      const msg = error.response?.data?.message || '账号已被锁定'
      showError(originalConfig, msg)
    } else if (!error.response) {
      showError(originalConfig, '网络连接失败，请检查网络')
    } else {
      const message = error.response?.data?.message || '请求失败'
      showError(originalConfig, message)
    }
    return Promise.reject(error)
  }
)

export default request
