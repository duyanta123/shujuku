import { describe, it, expect, beforeEach } from 'vitest'
import { readJsonStorage, writeJsonStorage } from './safeStorage.js'

const TEST_KEY = 'safe_storage_test'

beforeEach(() => {
  localStorage.removeItem(TEST_KEY)
})

describe('safeStorage', () => {
  // ================================================================
  // readJsonStorage
  // ================================================================

  it('readJsonStorage: 键不存在时返回默认值', () => {
    expect(readJsonStorage(TEST_KEY)).toEqual({})
    expect(readJsonStorage(TEST_KEY, [])).toEqual([])
    expect(readJsonStorage(TEST_KEY, { default: true })).toEqual({ default: true })
  })

  it('readJsonStorage: 读取有效 JSON 对象', () => {
    const data = { name: 'test', value: 42 }
    writeJsonStorage(TEST_KEY, data)
    expect(readJsonStorage(TEST_KEY)).toEqual(data)
  })

  it('readJsonStorage: 读取嵌套 JSON 对象', () => {
    const data = { user: { id: 1, roles: ['admin'] }, settings: { theme: 'dark' } }
    writeJsonStorage(TEST_KEY, data)
    expect(readJsonStorage(TEST_KEY)).toEqual(data)
  })

  it('readJsonStorage: 无效 JSON 返回 fallback 并清除存储', () => {
    localStorage.setItem(TEST_KEY, '{invalid json')
    const result = readJsonStorage(TEST_KEY, { fallback: true })
    expect(result).toEqual({ fallback: true })
    // 应清除无效数据
    expect(localStorage.getItem(TEST_KEY)).toBeNull()
  })

  it('readJsonStorage: 解析结果为数组时返回 fallback', () => {
    localStorage.setItem(TEST_KEY, JSON.stringify([1, 2, 3]))
    expect(readJsonStorage(TEST_KEY, { safe: true })).toEqual({ safe: true })
  })

  it('readJsonStorage: 解析结果为 null 时返回 fallback', () => {
    localStorage.setItem(TEST_KEY, 'null')
    expect(readJsonStorage(TEST_KEY, { safe: true })).toEqual({ safe: true })
  })

  it('readJsonStorage: 解析结果为字符串时返回 fallback', () => {
    localStorage.setItem(TEST_KEY, JSON.stringify('just a string'))
    expect(readJsonStorage(TEST_KEY, { safe: true })).toEqual({ safe: true })
  })

  it('readJsonStorage: 解析结果为数字时返回 fallback', () => {
    localStorage.setItem(TEST_KEY, '42')
    expect(readJsonStorage(TEST_KEY, { safe: true })).toEqual({ safe: true })
  })

  // ================================================================
  // writeJsonStorage
  // ================================================================

  it('writeJsonStorage: 写入简单对象', () => {
    writeJsonStorage(TEST_KEY, { name: 'test' })
    const raw = localStorage.getItem(TEST_KEY)
    expect(raw).toBe('{"name":"test"}')
  })

  it('writeJsonStorage: 写入嵌套对象', () => {
    const data = { nested: { a: 1, b: [1, 2, 3] } }
    writeJsonStorage(TEST_KEY, data)
    expect(JSON.parse(localStorage.getItem(TEST_KEY))).toEqual(data)
  })

  it('writeJsonStorage: 写入空对象', () => {
    writeJsonStorage(TEST_KEY, {})
    expect(JSON.parse(localStorage.getItem(TEST_KEY))).toEqual({})
  })

  // ================================================================
  // 往返测试
  // ================================================================

  it('readJsonStorage: 写入后读取应返回相同数据', () => {
    const original = { id: 1, name: 'test', tags: ['a', 'b'] }
    writeJsonStorage(TEST_KEY, original)
    expect(readJsonStorage(TEST_KEY)).toEqual(original)
  })

  it('readJsonStorage: 覆盖写入后读取最新数据', () => {
    writeJsonStorage(TEST_KEY, { version: 1 })
    writeJsonStorage(TEST_KEY, { version: 2 })
    expect(readJsonStorage(TEST_KEY)).toEqual({ version: 2 })
  })
})