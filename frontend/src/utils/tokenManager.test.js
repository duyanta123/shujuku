import { describe, it, expect, beforeEach } from 'vitest'
import tokenManager from './tokenManager.js'

beforeEach(() => {
  localStorage.clear()
})

describe('tokenManager cookie-only compatibility wrapper', () => {
  it('never exposes or stores bearer tokens', async () => {
    tokenManager.setToken('secret-token', 3600)
    expect(tokenManager.getToken()).toBeNull()
    expect(tokenManager.getTokenExpireTime()).toBe(0)
    expect(tokenManager.isTokenExpired()).toBe(false)
    expect(tokenManager.isTokenAboutToExpire()).toBe(false)
    expect(await tokenManager.refreshTokenIfNeeded()).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('stores only non-sensitive UI cache fields', () => {
    tokenManager.setUser({
      id: 1,
      role: 'student',
      token: 'secret-token',
      name: '张三',
      account: 'S001',
      avatarUrl: '/avatar.png',
    })

    expect(tokenManager.getUser()).toEqual({
      name: '张三',
      account: 'S001',
      avatarUrl: '/avatar.png',
      college: '',
      title: '',
    })
    expect(localStorage.getItem('user')).not.toContain('secret-token')
  })

  it('handles damaged localStorage safely', () => {
    localStorage.setItem('user', '{invalid-json')
    expect(tokenManager.getUser()).toEqual({})
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('clears UI cache on clearToken', () => {
    tokenManager.setUser({ name: '张三' })
    expect(localStorage.getItem('user')).toBeTruthy()
    tokenManager.clearToken()
    expect(localStorage.getItem('user')).toBeNull()
  })
})
