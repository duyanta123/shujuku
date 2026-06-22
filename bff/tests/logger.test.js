/**
 * logger.js 单元测试 — 核心日志工具
 *
 * 测试覆盖：
 * 1. createLogger — 各级别日志方法存在性
 * 2. maskSensitive — 敏感字段脱敏（密码、Token、Secret）
 * 3. maskSensitive — 边界情况（null、非对象、嵌套对象、空key列表）
 * 4. maskSensitive — 自定义敏感字段列表
 * 5. format — 时间戳和模块前缀格式
 *
 * 风险背景：
 * logger.js 是 BFF 所有模块的日志基础设施，maskSensitive 是防止敏感信息
 * 泄露到日志的关键安全函数。若脱敏逻辑失效，密码/Token 可能被记录到日志中。
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createLogger, maskSensitive } from '../src/utils/logger.js'

// ============================================================================
// createLogger — 日志器创建
// ============================================================================
describe('createLogger — 日志器创建', () => {
  it('应返回包含 info/warn/error/debug 方法的对象', () => {
    const log = createLogger('TEST')
    expect(log).toBeDefined()
    expect(typeof log.info).toBe('function')
    expect(typeof log.warn).toBe('function')
    expect(typeof log.error).toBe('function')
    expect(typeof log.debug).toBe('function')
  })

  it('各方法调用时不应抛出异常', () => {
    const log = createLogger('TEST')
    expect(() => log.info('测试消息')).not.toThrow()
    expect(() => log.warn('警告消息')).not.toThrow()
    expect(() => log.error('错误消息')).not.toThrow()
    expect(() => log.debug('调试消息')).not.toThrow()
  })

  it('应支持带附加数据的日志', () => {
    const log = createLogger('TEST')
    expect(() => log.info('测试', { key: 'value' })).not.toThrow()
    expect(() => log.warn('警告', { reason: 'test' })).not.toThrow()
    expect(() => log.error('错误', { detail: 'info' })).not.toThrow()
  })

  it('debug 日志在非 debug 级别时不应输出', () => {
    const consoleSpy = vi.spyOn(console, 'debug').mockImplementation(() => {})
    const originalLevel = process.env.LOG_LEVEL
    delete process.env.LOG_LEVEL // 确保不是 debug 级别

    const log = createLogger('TEST')
    log.debug('不应出现')

    expect(consoleSpy).not.toHaveBeenCalled()
    consoleSpy.mockRestore()
    if (originalLevel) process.env.LOG_LEVEL = originalLevel
  })

  it('debug 日志在 LOG_LEVEL=debug 时应输出', () => {
    const consoleSpy = vi.spyOn(console, 'debug').mockImplementation(() => {})
    const originalLevel = process.env.LOG_LEVEL
    process.env.LOG_LEVEL = 'debug'

    const log = createLogger('TEST')
    log.debug('调试信息')

    expect(consoleSpy).toHaveBeenCalled()
    consoleSpy.mockRestore()
    if (originalLevel) process.env.LOG_LEVEL = originalLevel
  })

  it('日志格式应包含模块标识', () => {
    const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {})

    const log = createLogger('AUTH')
    log.info('登录测试')

    const callArg = consoleSpy.mock.calls[0]?.[0]
    expect(callArg).toContain('[BFF:AUTH]')
    expect(callArg).toContain('[INFO]')
    expect(callArg).toContain('登录测试')

    consoleSpy.mockRestore()
  })
})

// ============================================================================
// maskSensitive — 敏感字段脱敏
// ============================================================================
describe('maskSensitive — 敏感字段脱敏', () => {
  it('应遮蔽 password 字段', () => {
    const result = maskSensitive({ username: 'test', password: 'secret123' })
    expect(result.username).toBe('test')
    expect(result.password).toBe('***')
  })

  it('应遮蔽 token 字段', () => {
    const result = maskSensitive({ token: 'eyJhbGciOiJIUzI1NiIs...' })
    expect(result.token).toBe('***')
  })

  it('应遮蔽 secret 字段', () => {
    const result = maskSensitive({ secret: 'my-secret-key' })
    expect(result.secret).toBe('***')
  })

  it('应同时遮蔽多个敏感字段', () => {
    const result = maskSensitive({
      username: 'test',
      password: 'pwd',
      token: 'tok',
      secret: 'sec',
      role: 'admin',
    })
    expect(result.password).toBe('***')
    expect(result.token).toBe('***')
    expect(result.secret).toBe('***')
    expect(result.username).toBe('test')
    expect(result.role).toBe('admin')
  })

  it('不修改原始对象', () => {
    const original = { username: 'test', password: 'secret' }
    const result = maskSensitive(original)
    expect(result.password).toBe('***')
    expect(original.password).toBe('secret') // 原始对象不变
  })

  it('应支持自定义敏感字段列表', () => {
    const result = maskSensitive(
      { apiKey: 'abc123', sessionId: 'sess-456', name: 'test' },
      ['apiKey', 'sessionId']
    )
    expect(result.apiKey).toBe('***')
    expect(result.sessionId).toBe('***')
    expect(result.name).toBe('test')
  })
})

// ============================================================================
// maskSensitive — 边界情况
// ============================================================================
describe('maskSensitive — 边界情况', () => {
  it('null 输入应返回 null', () => {
    expect(maskSensitive(null)).toBeNull()
  })

  it('undefined 输入应返回 undefined', () => {
    expect(maskSensitive(undefined)).toBeUndefined()
  })

  it('非对象输入应原样返回', () => {
    expect(maskSensitive('string')).toBe('string')
    expect(maskSensitive(123)).toBe(123)
    expect(maskSensitive(true)).toBe(true)
  })

  it('空对象应返回空对象', () => {
    const result = maskSensitive({})
    expect(result).toEqual({})
  })

  it('嵌套对象中内层敏感字段不会被遮蔽（浅拷贝限制）', () => {
    const result = maskSensitive({
      user: { name: 'test', password: 'nested-pwd' },
      data: { token: 'nested-token' },
    })
    // maskSensitive 仅做浅拷贝，不会递归进入嵌套对象
    // 一级 key 名为 'password'/'token' 才会被遮蔽，嵌套内部不会
    expect(result.user.password).toBe('nested-pwd')
    expect(result.data.token).toBe('nested-token')
  })

  it('敏感字段值为空字符串时不应遮蔽', () => {
    const result = maskSensitive({ password: '' })
    expect(result.password).toBe('') // 不遮蔽空值
  })

  it('敏感字段值为 null 时不应遮蔽', () => {
    const result = maskSensitive({ password: null })
    expect(result.password).toBeNull()
  })

  it('敏感字段值为 undefined 时不应遮蔽', () => {
    const result = maskSensitive({ password: undefined })
    expect(result.password).toBeUndefined()
  })

  it('自定义空 keys 列表不会遮蔽任何字段', () => {
    // 空数组作为 keys 参数时，不迭代任何 key，所有字段保留原值
    const result = maskSensitive(
      { password: 'secret', token: 'tok' },
      []
    )
    expect(result.password).toBe('secret')
    expect(result.token).toBe('tok')
  })
})