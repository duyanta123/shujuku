import { describe, it, expect, vi, beforeEach } from 'vitest'

const {
  axiosPost,
  axiosInstance,
  requestInterceptorFn,
  responseSuccessFn,
  responseErrorFn,
  routerPush,
  currentRoute,
} = vi.hoisted(() => {
  const reqFn = { current: null }
  const resSuccessFn = { current: null }
  const resErrorFn = { current: null }
  const instance = vi.fn().mockResolvedValue({ data: { success: true } })
  instance.interceptors = {
    request: { use: vi.fn((fn) => { reqFn.current = fn }) },
    response: { use: vi.fn((success, error) => { resSuccessFn.current = success; resErrorFn.current = error }) },
  }
  return {
    axiosPost: vi.fn(),
    axiosInstance: instance,
    requestInterceptorFn: reqFn,
    responseSuccessFn: resSuccessFn,
    responseErrorFn: resErrorFn,
    routerPush: vi.fn(),
    currentRoute: { value: { path: '/student/course' } },
  }
})

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => axiosInstance),
    post: axiosPost,
  },
}))

vi.mock('@/router', () => ({
  default: {
    push: routerPush,
    currentRoute,
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn() },
}))

describe('request.js cookie-only client', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    localStorage.clear()
    currentRoute.value.path = '/student/course'
    axiosPost.mockResolvedValue({ data: { success: true } })
    axiosInstance.mockResolvedValue({ data: { success: true } })
    vi.resetModules()
    await import('@/utils/request.js')
  })

  it('sets withCredentials and strips Authorization headers', async () => {
    const config = {
      url: '/course/list',
      headers: {
        Authorization: 'Bearer client-token',
        authorization: 'Bearer client-token',
      },
    }

    const result = await requestInterceptorFn.current(config)
    expect(result.withCredentials).toBe(true)
    expect(result.headers.Authorization).toBeUndefined()
    expect(result.headers.authorization).toBeUndefined()
  })

  it('unwraps successful responses', async () => {
    const result = responseSuccessFn.current({ data: { success: true, data: [1] } })
    expect(result).toEqual({ success: true, data: [1] })
  })

  it('refreshes once and retries protected requests after a 401', async () => {
    const error = {
      config: { url: '/course/list', headers: {} },
      response: { status: 401, data: {} },
    }

    await responseErrorFn.current(error)

    expect(axiosPost).toHaveBeenCalledWith('/api/auth/refresh', {}, { withCredentials: true })
    expect(axiosInstance).toHaveBeenCalledWith(expect.objectContaining({
      url: '/course/list',
      _retry: true,
    }))
  })

  it('shares a single refresh request for concurrent 401 responses', async () => {
    let resolveRefresh
    axiosPost.mockReturnValue(new Promise(resolve => { resolveRefresh = resolve }))
    const first = responseErrorFn.current({
      config: { url: '/course/list', headers: {} },
      response: { status: 401, data: {} },
    })
    const second = responseErrorFn.current({
      config: { url: '/user/profile', headers: {} },
      response: { status: 401, data: {} },
    })

    expect(axiosPost).toHaveBeenCalledTimes(1)
    resolveRefresh({ data: { success: true } })
    await Promise.all([first, second])
    expect(axiosInstance).toHaveBeenCalledTimes(2)
  })

  it('does not refresh login failures', async () => {
    const error = {
      config: { url: '/student/login', headers: {} },
      response: { status: 401, data: {} },
    }

    await expect(responseErrorFn.current(error)).rejects.toBe(error)
    expect(axiosPost).not.toHaveBeenCalled()
  })

  it('can skip refresh and messages for silent auth probes', async () => {
    const error = {
      config: {
        url: '/user/profile',
        headers: {},
        skipAuthRefresh: true,
        skipAuthRedirect: true,
        skipErrorMessage: true,
      },
      response: { status: 401, data: {} },
    }

    await expect(responseErrorFn.current(error)).rejects.toBe(error)
    expect(axiosPost).not.toHaveBeenCalled()
    expect(routerPush).not.toHaveBeenCalled()
  })

  it('clears UI cache and redirects when refresh fails', async () => {
    localStorage.setItem('user', '{"name":"cached"}')
    axiosPost.mockRejectedValue(new Error('refresh failed'))
    const error = {
      config: { url: '/course/list', headers: {} },
      response: { status: 401, data: {} },
    }

    await expect(responseErrorFn.current(error)).rejects.toThrow('refresh failed')
    expect(localStorage.getItem('user')).toBeNull()
    expect(routerPush).toHaveBeenCalledWith('/login')
  })
})
