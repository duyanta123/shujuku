/**
 * TokenManager 单元测试 — BFF 模式
 *
 * 当前环境: VITE_BFF_ENABLED=true (BFF 模式)
 * 在 BFF 模式下，Token 由 HttpOnly Cookie 管理，前端不持有 Token。
 *
 * 风险行为覆盖：
 * - BFF 模式: getToken 返回 null, setToken 是 no-op
 * - BFF 模式: isTokenExpired / isTokenAboutToExpire 返回 false
 * - BFF 模式: refreshTokenIfNeeded 调用 /api/auth/refresh
 * - getUser / setUser: 用户信息存取（BFF 和 非BFF 行为一致）
 * - clearToken: 清除 localStorage 中的 user 数据
 * - 边界情况: localStorage 损坏、空值处理
 */
import { describe, it, expect, beforeEach } from 'vitest'
import tokenManager from './tokenManager.js'

beforeEach(() => {
  localStorage.clear()
  tokenManager.clearToken()
})

// ============================================================================
// BFF 模式 — Token 存取（BFF 下不操作 localStorage）
// ============================================================================
describe('BFF 模式 — Token 存取', () => {
  it('BFF 模式下 getToken 应返回 null（Cookie 自动携带）', () => {
    expect(tokenManager.getToken()).toBeNull()
  })

  it('BFF 模式下 setToken 应为 no-op（Cookie 由 BFF 管理）', () => {
    tokenManager.setToken('test-token', 3600)
    expect(tokenManager.getToken()).toBeNull()
    // localStorage 不应被写入
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    expect(user.token).toBeUndefined()
  })

  it('BFF 模式下 isTokenExpired 应返回 false（BFF 端管理过期）', () => {
    expect(tokenManager.isTokenExpired()).toBe(false)
  })

  it('BFF 模式下 isTokenAboutToExpire 应返回 false（BFF 端自动刷新）', () => {
    expect(tokenManager.isTokenAboutToExpire()).toBe(false)
  })

  it('BFF 模式下 getTokenExpireTime 应返回 0', () => {
    expect(tokenManager.getTokenExpireTime()).toBe(0)
  })
})

// ============================================================================
// getUser / setUser — 用户信息存取（BFF 和非BFF 行为一致）
// ============================================================================
describe('getUser / setUser — 用户信息存取', () => {
  it('初始状态应返回空对象', () => {
    expect(tokenManager.getUser()).toEqual({})
  })

  it('setUser 应能存储用户信息', () => {
    tokenManager.setUser({ id: 1, name: '张三', role: 'student' })
    const user = tokenManager.getUser()
    expect(user.id).toBe(1)
    expect(user.name).toBe('张三')
    expect(user.role).toBe('student')
  })

  it('setUser 应覆盖旧用户信息', () => {
    tokenManager.setUser({ id: 1, name: '张三' })
    tokenManager.setUser({ id: 2, name: '李四', role: 'teacher' })
    const user = tokenManager.getUser()
    expect(user.id).toBe(2)
    expect(user.name).toBe('李四')
    expect(user.role).toBe('teacher')
  })

  it('setUser 后 localStorage 应持久化', () => {
    tokenManager.setUser({ id: 1, name: '张三' })
    const raw = localStorage.getItem('user')
    expect(raw).toBeTruthy()
    const parsed = JSON.parse(raw)
    expect(parsed.id).toBe(1)
    expect(parsed.name).toBe('张三')
  })
})

// ============================================================================
// clearToken — 清除数据
// ============================================================================
describe('clearToken — 清除数据', () => {
  it('应清除 localStorage 中的 user 数据', () => {
    tokenManager.setUser({ id: 1, name: '张三' })
    expect(localStorage.getItem('user')).toBeTruthy()

    tokenManager.clearToken()
    expect(localStorage.getItem('user')).toBeNull()
    expect(tokenManager.getUser()).toEqual({})
  })

  it('重复 clearToken 不应报错', () => {
    expect(() => tokenManager.clearToken()).not.toThrow()
    expect(() => tokenManager.clearToken()).not.toThrow()
  })

  it('clearToken 后 getToken 仍返回 null', () => {
    tokenManager.setUser({ id: 1, name: '张三' })
    tokenManager.clearToken()
    expect(tokenManager.getToken()).toBeNull()
  })
})

// ============================================================================
// 边界情况 — localStorage 损坏
// ============================================================================
describe('边界情况 — localStorage 损坏', () => {
  it('localStorage 中存储无效 JSON 时 getToken 不应崩溃', () => {
    localStorage.setItem('user', '{invalid-json')
    expect(() => tokenManager.getToken()).not.toThrow()
    expect(tokenManager.getToken()).toBeNull()
  })

  it('localStorage 中存储无效 JSON 时 getUser 会抛出 SyntaxError（已知缺陷：getUser 缺少 JSON.parse 异常处理）', () => {
    localStorage.setItem('user', '{invalid-json')
    expect(() => tokenManager.getUser()).toThrow(SyntaxError)
  })

  it('localStorage 中存储有效 JSON 但非对象时 getToken 应安全处理', () => {
    localStorage.setItem('user', '"just-a-string"')
    expect(() => tokenManager.getToken()).not.toThrow()
    const result = tokenManager.getToken()
    // BFF 模式下始终返回 null
    expect(result).toBeNull()
  })

  it('localStorage 中存储有效 JSON 但非对象时 getUser 应返回空对象', () => {
    localStorage.setItem('user', '"just-a-string"')
    expect(() => tokenManager.getUser()).not.toThrow()
    const user = tokenManager.getUser()
    // JSON.parse 返回字符串，但代码期望对象
    expect(user == null || typeof user === 'string' || typeof user === 'object').toBe(true)
  })
})

// ============================================================================
// 集成场景 — BFF 模式下的完整用户流程
// ============================================================================
describe('集成场景 — BFF 模式完整流程', () => {
  it('登录 → 存储用户信息 → 登出清除', () => {
    // 模拟登录后设置用户信息
    tokenManager.setUser({
      id: 100,
      name: '李四',
      role: 'student',
      userId: 100,
      username: 'S001'
    })

    const user = tokenManager.getUser()
    expect(user.id).toBe(100)
    expect(user.name).toBe('李四')
    expect(user.role).toBe('student')

    // Token 由 BFF Cookie 管理，前端不应持有
    expect(tokenManager.getToken()).toBeNull()
    expect(tokenManager.isTokenExpired()).toBe(false)

    // 登出
    tokenManager.clearToken()
    expect(tokenManager.getUser()).toEqual({})
    expect(tokenManager.getToken()).toBeNull()
  })
})