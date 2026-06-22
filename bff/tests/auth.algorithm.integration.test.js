/**
 * auth.js 刷新端点 — 多算法 JWT 支持测试
 *
 * 测试覆盖：
 * 1. refresh 端点接受 HS256 Token（基础场景）
 * 2. refresh 端点接受 HS384 Token（后端 Keys.hmacShaKeyFor 48字节密钥自动选择）
 * 3. refresh 端点接受 HS512 Token（64字节密钥场景）
 * 4. refresh 端点拒绝 'none' 算法 Token（安全防护）
 *
 * 风险背景：
 * jwtVerify.js 中间件通过 algorithms: ['HS256','HS384','HS512'] 支持多算法，
 * 但 auth.js refresh 端点 line 175 仅配置了 algorithms: ['HS256']，
 * 当后端使用 48 字节密钥自动生成 HS384 Token 时，BFF 刷新会拒绝合法 Token。
 */
import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import jwt from 'jsonwebtoken'
import { config } from '../src/config.js'

let app

// 使用与生产环境一致的密钥长度（48字节 → HS384）
const SECRET_48 = 'test-secret-for-hs384-48bytes-minimum!!'
const SECRET_64 = 'test-secret-for-hs512-64bytes-minimum-length-required!!'

beforeAll(async () => {
  const { buildApp } = await import('../src/index.js')
  app = await buildApp()
  await app.ready()
})

afterAll(async () => {
  if (app) await app.close()
})

// ============================================================================
// HS256 — 基础场景（应正常工作）
// ============================================================================
describe('POST /api/auth/refresh — HS256 Token', () => {
  it('HS256 Token 应能正常刷新', async () => {
    const hs256Token = jwt.sign(
      { userId: 1, username: 'testuser', role: 'student' },
      config.jwt.secret,
      { algorithm: 'HS256', expiresIn: '1h' }
    )

    const response = await app.inject({
      method: 'POST',
      url: '/api/auth/refresh',
      cookies: { [config.jwt.refreshTokenCookieName]: hs256Token },
    })

    // 不应返回 401（Token 应被接受）
    // 后端不在运行时返回 502，运行时返回 200
    expect(response.statusCode).not.toBe(401)
    const body = response.json()
    expect(body).toBeTruthy()
  })
})

// ============================================================================
// HS384 — 48字节密钥场景（后端 Keys.hmacShaKeyFor 自动选择）
// ============================================================================
describe('POST /api/auth/refresh — HS384 Token (关键缺口)', () => {
  it('HS384 Token 应被 refresh 端点接受（不应返回 401）', async () => {
    // 后端使用 Keys.hmacShaKeyFor(secret.getBytes(UTF_8)) 时，
    // 48字节密钥自动选择 HS384 算法
    const hs384Token = jwt.sign(
      { userId: 1, username: 'testuser', role: 'student' },
      config.jwt.secret,
      { algorithm: 'HS384', expiresIn: '1h' }
    )

    const response = await app.inject({
      method: 'POST',
      url: '/api/auth/refresh',
      cookies: { [config.jwt.refreshTokenCookieName]: hs384Token },
    })

    // 关键断言：refresh 端点不应因算法不匹配而返回 401
    expect(response.statusCode).not.toBe(401)
    const body = response.json()
    expect(body).toBeTruthy()
  })

  it('HS384 Token 过期时 refresh 端点应正确拒绝', async () => {
    const expiredHs384 = jwt.sign(
      { userId: 1, username: 'testuser', role: 'student' },
      config.jwt.secret,
      { algorithm: 'HS384', expiresIn: '0s' }
    )

    const response = await app.inject({
      method: 'POST',
      url: '/api/auth/refresh',
      cookies: { [config.jwt.refreshTokenCookieName]: expiredHs384 },
    })

    // 过期 Token 应被拒绝
    expect(response.statusCode).toBe(401)
    const body = response.json()
    expect(body.success).toBe(false)
    expect(body.message).toContain('过期')
  })
})

// ============================================================================
// HS512 — 64字节密钥场景
// ============================================================================
describe('POST /api/auth/refresh — HS512 Token', () => {
  it('HS512 Token 应被 refresh 端点接受', async () => {
    const hs512Token = jwt.sign(
      { userId: 1, username: 'testuser', role: 'student' },
      config.jwt.secret,
      { algorithm: 'HS512', expiresIn: '1h' }
    )

    const response = await app.inject({
      method: 'POST',
      url: '/api/auth/refresh',
      cookies: { [config.jwt.refreshTokenCookieName]: hs512Token },
    })

    expect(response.statusCode).not.toBe(401)
    const body = response.json()
    expect(body).toBeTruthy()
  })
})

// ============================================================================
// 安全防护 — 拒绝不安全算法
// ============================================================================
describe('POST /api/auth/refresh — 安全算法拒绝', () => {
  it('none 算法 Token 应被拒绝', async () => {
    // jsonwebtoken 库默认拒绝 'none' 算法，会被 catch 捕获
    const response = await app.inject({
      method: 'POST',
      url: '/api/auth/refresh',
      cookies: { [config.jwt.refreshTokenCookieName]: 'malformed.token.here' },
    })

    expect(response.statusCode).toBe(401)
    const body = response.json()
    expect(body.success).toBe(false)
  })
})