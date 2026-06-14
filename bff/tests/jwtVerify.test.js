import { describe, it, expect, vi, beforeEach } from 'vitest'
import jwt from 'jsonwebtoken'

// 模拟 config 模块，确保 cookie 名称与 config.js 一致
const mockConfig = {
  jwt: {
    secret: 'test-secret-for-unit-tests',
    accessTokenCookieName: 'bff_access_token',
    refreshTokenCookieName: 'bff_refresh_token',
    cookieName: 'bff_token',
  },
}

vi.mock('../src/config.js', () => ({
  config: mockConfig,
}))

// 动态导入（在 mock 之后）
const { jwtVerify } = await import('../src/middleware/jwtVerify.js')

describe('jwtVerify 中间件 — Cookie 名称匹配', () => {
  /** @type {import('fastify').FastifyRequest} */
  let mockRequest
  /** @type {import('fastify').FastifyReply} */
  let mockReply
  const VALID_TOKEN = jwt.sign(
    { userId: 1, username: 'testuser', role: 'student' },
    mockConfig.jwt.secret,
    { algorithm: 'HS256', expiresIn: '1h' }
  )

  beforeEach(() => {
    mockReply = {
      code: vi.fn().mockReturnThis(),
      send: vi.fn().mockReturnThis(),
      clearCookie: vi.fn().mockReturnThis(),
    }
    mockRequest = {
      id: 'test-id',
      method: 'GET',
      url: '/api/selection/my',
      ip: '127.0.0.1',
      cookies: {},
      headers: {},
    }
  })

  it('应该能从 bff_access_token Cookie 中读取 Token（登录时设置的 Cookie 名）', async () => {
    // 模拟登录后 BFF 设置的 Cookie
    mockRequest.cookies = {
      bff_access_token: VALID_TOKEN,
    }

    await jwtVerify(mockRequest, mockReply)

    // 验证请求头中注入了 Authorization
    expect(mockRequest.headers.authorization).toBe(`Bearer ${VALID_TOKEN}`)
    // 验证用户信息已注入
    expect(mockRequest.user).toBeDefined()
    expect(mockRequest.user.userId).toBe(1)
    expect(mockRequest.user.username).toBe('testuser')
    expect(mockRequest.user.role).toBe('student')
    // 不应该返回 401
    expect(mockReply.code).not.toHaveBeenCalledWith(401)
    expect(mockReply.send).not.toHaveBeenCalled()
  })

  it('当 bff_access_token 不存在时，应该回退到 bff_token Cookie（兼容旧版）', async () => {
    mockRequest.cookies = {
      bff_token: VALID_TOKEN,
    }

    await jwtVerify(mockRequest, mockReply)

    expect(mockRequest.headers.authorization).toBe(`Bearer ${VALID_TOKEN}`)
    expect(mockRequest.user).toBeDefined()
    expect(mockRequest.user.userId).toBe(1)
  })

  it('当所有 Cookie 都不存在时，应该返回 401', async () => {
    mockRequest.cookies = {}

    await jwtVerify(mockRequest, mockReply)

    expect(mockReply.code).toHaveBeenCalledWith(401)
    expect(mockReply.send).toHaveBeenCalledWith(
      expect.objectContaining({ success: false, message: '未提供认证信息' })
    )
  })

  it('应该能验证 HS384 Token（与后端 Keys.hmacShaKeyFor 48字节密钥一致）', async () => {
    // 后端使用 Keys.hmacShaKeyFor(secret.getBytes(UTF_8)) 自动选择算法
    // 48字节密钥 → HS384，这是后端实际生成的算法
    const hs384Token = jwt.sign(
      { userId: 1, username: 'testuser', role: 'student' },
      mockConfig.jwt.secret,
      { algorithm: 'HS384', expiresIn: '1h' }
    )
    mockRequest.cookies = {
      bff_access_token: hs384Token,
    }

    await jwtVerify(mockRequest, mockReply)

    // HS384 Token 也应该被接受
    expect(mockRequest.headers.authorization).toBe(`Bearer ${hs384Token}`)
    expect(mockRequest.user).toBeDefined()
    expect(mockRequest.user.userId).toBe(1)
    expect(mockRequest.user.role).toBe('student')
    expect(mockReply.code).not.toHaveBeenCalledWith(401)
  })

  it('bff_access_token 应优先于 bff_token', async () => {
    const newerToken = jwt.sign(
      { userId: 2, username: 'newuser', role: 'admin' },
      mockConfig.jwt.secret,
      { algorithm: 'HS256', expiresIn: '1h' }
    )
    mockRequest.cookies = {
      bff_access_token: newerToken,
      bff_token: VALID_TOKEN,
    }

    await jwtVerify(mockRequest, mockReply)

    expect(mockRequest.user.userId).toBe(2)
    expect(mockRequest.user.username).toBe('newuser')
    expect(mockRequest.user.role).toBe('admin')
  })
})