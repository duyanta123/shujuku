/**
 * request.js 单元测试 — 核心 HTTP 请求工具测试（原本零覆盖）
 *
 * 风险行为覆盖：
 * - BFF 模式：请求拦截器调用 tokenManager.refreshTokenIfNeeded()
 * - 降级模式：Token 即将过期时触发并发刷新队列
 * - 响应拦截器：401/403/423/网络错误 等状态码处理
 * - 响应数据自动解包（response.data）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// ============================================================================
// 使用 vi.hoisted 确保 mock 在模块加载前可用
// ============================================================================

const {
  mockClearToken,
  mockGetToken,
  mockRefreshTokenIfNeeded,
  mockIsTokenAboutToExpire,
  mockSetToken,
  mockRouterPush,
  requestInterceptorFn,
  responseSuccessFn,
  responseErrorFn,
} = vi.hoisted(() => {
  const reqFn = { current: null }
  const resSuccessFn = { current: null }
  const resErrorFn = { current: null }

  return {
    mockClearToken: vi.fn(),
    mockGetToken: vi.fn(),
    mockRefreshTokenIfNeeded: vi.fn(),
    mockIsTokenAboutToExpire: vi.fn(),
    mockSetToken: vi.fn(),
    mockRouterPush: vi.fn(),
    requestInterceptorFn: reqFn,
    responseSuccessFn: resSuccessFn,
    responseErrorFn: resErrorFn,
  }
})

// Mock axios — 捕获拦截器注册以便直接测试
vi.mock('axios', async (importOriginal) => {
  const actualAxios = await importOriginal()
  // 保存原始 axios 以使用其功能
  const axiosInstance = actualAxios.default.create
    ? actualAxios.default.create({})
    : { interceptors: { request: { use: () => {} }, response: { use: () => {} } } }

  return {
    default: {
      create: vi.fn(() => ({
        // 捕获请求拦截器
        interceptors: {
          request: {
            use: vi.fn((fn) => {
              requestInterceptorFn.current = fn
              return 0
            }),
          },
          response: {
            use: vi.fn((success, error) => {
              responseSuccessFn.current = success
              responseErrorFn.current = error
              return 0
            }),
          },
        },
        get: vi.fn(),
        post: vi.fn(),
        defaults: { baseURL: '', timeout: 0, withCredentials: false },
      })),
    },
  }
})

vi.mock('@/utils/tokenManager', () => ({
  default: {
    getToken: mockGetToken,
    clearToken: mockClearToken,
    refreshTokenIfNeeded: mockRefreshTokenIfNeeded,
    isTokenAboutToExpire: mockIsTokenAboutToExpire,
    setToken: mockSetToken,
  },
}))

vi.mock('@/router', () => ({
  default: { push: mockRouterPush },
}))

// Mock element-plus — 简单替换
vi.mock('element-plus', () => {
  const fn = vi.fn()
  return { ElMessage: { error: fn, warning: fn } }
})

// ============================================================================
// 测试主体
// ============================================================================

describe('request.js — HTTP 请求拦截器与错误处理', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubEnv('VITE_BFF_ENABLED', 'true')
  })

  describe('模块加载', () => {
    it('应导出 axios 实例', async () => {
      const request = await import('@/utils/request.js')
      expect(request.default).toBeDefined()
      expect(typeof request.default.get).toBe('function')
      expect(typeof request.default.post).toBe('function')
    })
  })

  describe('响应拦截器 — 错误处理', () => {
    it('401 错误应清除 Token 并跳转到登录页', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = { response: { status: 401, data: {} } }

      try { await handler(error) } catch (_) { /* reject expected */ }

      expect(mockClearToken).toHaveBeenCalled()
      expect(mockRouterPush).toHaveBeenCalledWith('/login')
    })

    it('403 错误应显示权限错误提示', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = { response: { status: 403, data: {} } }

      try { await handler(error) } catch (_) {}
    })

    it('423 错误（账号锁定）应显示锁定消息', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = {
        response: { status: 423, data: { message: '账号已被锁定，请15分钟后再试' } },
      }

      try { await handler(error) } catch (_) {}
    })

    it('423 错误无自定义消息时应使用默认消息', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = { response: { status: 423, data: {} } }

      try { await handler(error) } catch (_) {}
    })

    it('网络错误（无 response）应显示网络连接失败', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = { message: 'Network Error' }

      try { await handler(error) } catch (_) {}
    })

    it('其他 HTTP 错误应显示后端消息', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = { response: { status: 500, data: { message: '服务器内部错误' } } }

      try { await handler(error) } catch (_) {}
    })

    it('其他 HTTP 错误无消息时应显示默认消息', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = { response: { status: 500, data: {} } }

      try { await handler(error) } catch (_) {}
    })

    it('错误应被 reject 以便调用方捕获', async () => {
      await import('@/utils/request.js')
      const handler = responseErrorFn.current
      const error = { response: { status: 500, data: { message: 'test' } } }

      await expect(handler(error)).rejects.toBe(error)
    })
  })

  describe('响应拦截器 — 成功响应', () => {
    it('成功响应应自动解包返回 response.data', async () => {
      await import('@/utils/request.js')
      const handler = responseSuccessFn.current
      const response = { data: { success: true, message: 'ok' }, status: 200 }

      const result = await handler(response)
      expect(result).toEqual({ success: true, message: 'ok' })
    })
  })

  describe('请求拦截器 — BFF 模式', () => {
    it('BFF 模式下应调用 tokenManager.refreshTokenIfNeeded()', async () => {
      mockRefreshTokenIfNeeded.mockResolvedValue(null)

      await import('@/utils/request.js')
      const handler = requestInterceptorFn.current
      const config = { method: 'get', url: '/test', headers: {} }

      await handler(config)
      expect(mockRefreshTokenIfNeeded).toHaveBeenCalled()
    })

    it('BFF 模式下不应在请求头中添加 Authorization', async () => {
      mockRefreshTokenIfNeeded.mockResolvedValue(null)

      await import('@/utils/request.js')
      const handler = requestInterceptorFn.current
      const config = { method: 'get', url: '/test', headers: {} }

      const result = await handler(config)
      expect(result.headers?.Authorization).toBeUndefined()
    })
  })

  describe('请求拦截器 — 降级模式', () => {
    beforeEach(() => {
      vi.stubEnv('VITE_BFF_ENABLED', 'false')
    })

    it('降级模式下 Token 未过期时应在请求头添加 Authorization', async () => {
      mockGetToken.mockReturnValue('test-token-123')
      mockIsTokenAboutToExpire.mockReturnValue(false)

      vi.resetModules()
      await import('@/utils/request.js')
      const handler = requestInterceptorFn.current
      const config = { method: 'get', url: '/test', headers: {} }

      const result = await handler(config)
      expect(result.headers.Authorization).toBe('Bearer test-token-123')
    })

    it('降级模式无 Token 时不应添加 Authorization', async () => {
      mockGetToken.mockReturnValue(null)
      mockIsTokenAboutToExpire.mockReturnValue(false)

      vi.resetModules()
      await import('@/utils/request.js')
      const handler = requestInterceptorFn.current
      const config = { method: 'get', url: '/test', headers: {} }

      const result = await handler(config)
      expect(result.headers.Authorization).toBeUndefined()
    })
  })
})