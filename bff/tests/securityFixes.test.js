import { afterEach, describe, expect, it, vi } from 'vitest'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
})

describe('security fixes', () => {
  it('login sets backend-issued tokens in HttpOnly cookies and strips tokens from response body', async () => {
    const backendAccessToken = 'backend-access-token'
    const backendRefreshToken = 'backend-refresh-token'

    globalThis.fetch = vi.fn().mockResolvedValue({
      status: 200,
      headers: {
        get: (key) => key.toLowerCase() === 'content-type' ? 'application/json' : null,
      },
      json: async () => ({
        success: true,
        message: 'ok',
        accessToken: backendAccessToken,
        refreshToken: backendRefreshToken,
        data: {
          id: 7,
          studentNo: 'S001',
          name: 'Student',
          accessToken: 'nested-access-token',
          refreshToken: 'nested-refresh-token',
          password: 'hash',
        },
      }),
    })

    const { buildApp } = await import('../src/index.js')
    const { config } = await import('../src/config.js')
    const app = await buildApp()

    try {
      const response = await app.inject({
        method: 'POST',
        url: '/api/student/login',
        payload: { studentNo: 'S001', password: 'pw' },
      })

      expect(response.statusCode).toBe(200)
      const body = response.json()
      expect(body.data.accessToken).toBeUndefined()
      expect(body.data.refreshToken).toBeUndefined()
      expect(body.data.password).toBeUndefined()

      const setCookie = Array.isArray(response.headers['set-cookie'])
        ? response.headers['set-cookie'].join('; ')
        : response.headers['set-cookie']
      expect(setCookie).toContain(`${config.jwt.accessTokenCookieName}=${backendAccessToken}`)
      expect(setCookie).toContain(`${config.jwt.refreshTokenCookieName}=${backendRefreshToken}`)
      expect(setCookie).toContain('HttpOnly')
    } finally {
      await app.close()
    }
  })

  it('fetchWithTimeout aborts hung backend requests', async () => {
    const { fetchWithTimeout } = await import('../src/utils/fetchWithTimeout.js')
    globalThis.fetch = vi.fn((_url, options) => new Promise((resolve, reject) => {
      options.signal.addEventListener('abort', () => reject(Object.assign(new Error('aborted'), {
        name: 'AbortError',
      })))
    }))

    await expect(fetchWithTimeout('http://localhost:1/hang', {}, 5)).rejects.toMatchObject({
      name: 'AbortError',
    })
  })

  it('config rejects weak JWT secrets outside test mode', async () => {
    const originalNodeEnv = process.env.NODE_ENV
    const originalJwtSecret = process.env.JWT_SECRET
    process.env.NODE_ENV = 'production'
    process.env.JWT_SECRET = 'lab-course-system-secret-key-2024'

    try {
      vi.resetModules()
      await expect(import('../src/config.js')).rejects.toThrow(/default or example/)
    } finally {
      if (originalNodeEnv === undefined) delete process.env.NODE_ENV
      else process.env.NODE_ENV = originalNodeEnv

      if (originalJwtSecret === undefined) delete process.env.JWT_SECRET
      else process.env.JWT_SECRET = originalJwtSecret

      vi.resetModules()
    }
  })
})
