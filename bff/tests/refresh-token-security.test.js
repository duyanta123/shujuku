/**
 * Security tests for Refresh Token leakage from BFF login response (MEDIUM-002).
 *
 * Verifies that the BFF login handler strips refreshToken and password
 * from the user data object before sending it to the frontend.
 */
import { describe, it, expect, beforeAll, afterAll, vi } from 'vitest'

describe('RefreshToken Security - BFF login response', () => {
  let app

  beforeAll(async () => {
    // Mock fetch to simulate backend response that includes refreshToken in data
    global.fetch = vi.fn().mockResolvedValue({
      status: 200,
      headers: {
        get: (name) => name === 'content-type' ? 'application/json' : null,
      },
      json: () => Promise.resolve({
        success: true,
        message: '登录成功',
        accessToken: 'eyJhbGciOiJIUzI1NiJ9.test-access-token',
        refreshToken: 'eyJhbGciOiJIUzI1NiJ9.test-refresh-token',
        data: {
          id: 1,
          studentNo: 'S001',
          name: '张三',
          password: 'should-be-stripped',
          refreshToken: 'leaked-refresh-token-value',
          college: '计算机学院',
          major: '软件工程',
        },
      }),
    })

    const { buildApp } = await import('../src/index.js')
    app = await buildApp()
    await app.ready()
  })

  afterAll(async () => {
    if (app) {
      await app.close()
    }
    vi.restoreAllMocks()
  })

  it('BFF student login response should NOT expose refreshToken in data object', async () => {
    // This test verifies that the BFF createLoginHandler properly strips
    // refreshToken from the user data before sending to the frontend.
    // The backend returns refreshToken in the student entity, but the BFF
    // should strip it before returning to the frontend.

    const response = await app.inject({
      method: 'POST',
      url: '/api/student/login',
      payload: {
        studentNo: 'S001',
        password: '123456'
      }
    })

    expect(response.statusCode).toBe(200)

    const body = JSON.parse(response.body)
    expect(body.success).toBe(true)
    expect(body.data).toBeDefined()

    // refreshToken should NOT be in the data object sent to frontend
    expect(body.data.refreshToken).toBeUndefined()
    expect(body.data.password).toBeUndefined()

    // But refreshToken should be set as HttpOnly cookie (via Set-Cookie header)
    const setCookieHeader = response.headers['set-cookie']
    expect(setCookieHeader).toBeDefined()
    // The cookie name may be 'bff_refresh_token' or 'refreshToken' depending on config
    const cookieStr = Array.isArray(setCookieHeader)
      ? setCookieHeader.join('; ')
      : setCookieHeader
    // Verify at least one cookie is HttpOnly
    expect(cookieStr).toMatch(/HttpOnly/i)
  })

  it('BFF teacher login response should NOT expose refreshToken in data object', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/api/teacher/login',
      payload: {
        teacherNo: 'T001',
        password: '123456'
      }
    })

    expect(response.statusCode).toBe(200)

    const body = JSON.parse(response.body)
    expect(body.success).toBe(true)
    expect(body.data).toBeDefined()

    // refreshToken should NOT be in the data object sent to frontend
    expect(body.data.refreshToken).toBeUndefined()
    expect(body.data.password).toBeUndefined()

    // But refreshToken should be set as HttpOnly cookie (via Set-Cookie header)
    const setCookieHeader = response.headers['set-cookie']
    expect(setCookieHeader).toBeDefined()
    // The cookie name may be 'bff_refresh_token' or 'refreshToken' depending on config
    const cookieStr = Array.isArray(setCookieHeader)
      ? setCookieHeader.join('; ')
      : setCookieHeader
    // Verify at least one cookie is HttpOnly
    expect(cookieStr).toMatch(/HttpOnly/i)
  })
})