/**
 * 课表本地缓存 — 单元测试
 *
 * 风险行为覆盖：
 * - getScheduleCache: 空缓存、正常缓存、过期缓存、localStorage 损坏
 * - setScheduleCache: 首次写入、覆盖写入、异常容错
 * - clearScheduleCache: 正常清除、重复清除不报错
 * - getCacheAge: 无缓存、刚刚、分钟前、小时前
 * - 边界情况: 空数组缓存、单条缓存、大量数据缓存
 */
import { describe, it, expect, beforeEach } from 'vitest'
import {
  getScheduleCache,
  setScheduleCache,
  clearScheduleCache,
  getCacheAge
} from './scheduleCache.js'

const CACHE_KEY = 'schedule_cache'
const TIMESTAMP_KEY = 'schedule_cache_timestamp'

beforeEach(() => {
  localStorage.removeItem(CACHE_KEY)
  localStorage.removeItem(TIMESTAMP_KEY)
})

// ============================================================================
// getScheduleCache — 获取缓存
// ============================================================================
describe('getScheduleCache — 获取缓存', () => {
  it('空缓存应返回 null data 和 isExpired=true', () => {
    const result = getScheduleCache()
    expect(result.data).toBeNull()
    expect(result.timestamp).toBe(0)
    expect(result.isExpired).toBe(true)
  })

  it('有效缓存应返回数据且 isExpired=false', () => {
    const courses = [{ courseName: '高等数学', courseTime: '周一 1-2节' }]
    setScheduleCache(courses)

    const result = getScheduleCache()
    expect(result.data).toEqual(courses)
    expect(result.timestamp).toBeGreaterThan(0)
    expect(result.isExpired).toBe(false)
  })

  it('过期缓存应返回数据但 isExpired=true', () => {
    const courses = [{ courseName: '高等数学' }]
    // 写入缓存，然后手动设置时间戳为 31 分钟前
    setScheduleCache(courses)
    const oldTimestamp = Date.now() - 31 * 60 * 1000
    localStorage.setItem(TIMESTAMP_KEY, String(oldTimestamp))

    const result = getScheduleCache()
    expect(result.data).toEqual(courses)
    expect(result.isExpired).toBe(true)
  })

  it('localStorage 中 data 损坏应返回 null data 和 isExpired=true', () => {
    localStorage.setItem(CACHE_KEY, '{invalid-json')
    localStorage.setItem(TIMESTAMP_KEY, String(Date.now()))

    const result = getScheduleCache()
    expect(result.data).toBeNull()
    expect(result.isExpired).toBe(true)
  })

  it('timestamp 不存在时应视为过期', () => {
    localStorage.setItem(CACHE_KEY, JSON.stringify([{ name: 'test' }]))
    // 不设置 TIMESTAMP_KEY

    const result = getScheduleCache()
    expect(result.data).toEqual([{ name: 'test' }])
    expect(result.timestamp).toBe(0)
    expect(result.isExpired).toBe(true)
  })
})

// ============================================================================
// setScheduleCache — 写入缓存
// ============================================================================
describe('setScheduleCache — 写入缓存', () => {
  it('首次写入后应能正常读取', () => {
    const courses = [{ courseName: '大学英语', courseTime: '周三 3-4节' }]
    setScheduleCache(courses)

    const raw = localStorage.getItem(CACHE_KEY)
    expect(raw).toBe(JSON.stringify(courses))

    const timestamp = localStorage.getItem(TIMESTAMP_KEY)
    expect(timestamp).toBeTruthy()
    expect(Number(timestamp)).toBeGreaterThan(0)
  })

  it('覆盖写入应更新数据和 timestamp', async () => {
    const oldCourses = [{ courseName: '旧课程' }]
    setScheduleCache(oldCourses)
    const oldTimestamp = localStorage.getItem(TIMESTAMP_KEY)

    // 等待 2ms 确保 timestamp 变化
    await new Promise(resolve => setTimeout(resolve, 2))
    const newCourses = [{ courseName: '新课程' }]
    setScheduleCache(newCourses)

    const raw = localStorage.getItem(CACHE_KEY)
    expect(raw).toBe(JSON.stringify(newCourses))
    const newTimestamp = localStorage.getItem(TIMESTAMP_KEY)
    expect(Number(newTimestamp)).toBeGreaterThan(Number(oldTimestamp))
  })

  it('空数组缓存应正常写入和读取', () => {
    setScheduleCache([])

    const result = getScheduleCache()
    expect(result.data).toEqual([])
    expect(result.isExpired).toBe(false)
  })

  it('单条缓存应正常', () => {
    setScheduleCache([{ courseName: '单课程' }])

    const result = getScheduleCache()
    expect(result.data).toHaveLength(1)
    expect(result.isExpired).toBe(false)
  })
})

// ============================================================================
// clearScheduleCache — 清除缓存
// ============================================================================
describe('clearScheduleCache — 清除缓存', () => {
  it('清除后 localStorage 应无相关数据', () => {
    setScheduleCache([{ courseName: '测试' }])
    clearScheduleCache()

    expect(localStorage.getItem(CACHE_KEY)).toBeNull()
    expect(localStorage.getItem(TIMESTAMP_KEY)).toBeNull()
  })

  it('重复清除不应报错', () => {
    clearScheduleCache()
    expect(() => clearScheduleCache()).not.toThrow()
  })

  it('清除后 getScheduleCache 应返回过期状态', () => {
    setScheduleCache([{ courseName: '测试' }])
    clearScheduleCache()

    const result = getScheduleCache()
    expect(result.data).toBeNull()
    expect(result.isExpired).toBe(true)
  })
})

// ============================================================================
// getCacheAge — 缓存年龄
// ============================================================================
describe('getCacheAge — 缓存年龄', () => {
  it('无缓存时应返回"无缓存"', () => {
    expect(getCacheAge()).toBe('无缓存')
  })

  it('timestamp 为 0 时应返回"无缓存"', () => {
    localStorage.setItem(TIMESTAMP_KEY, '0')
    expect(getCacheAge()).toBe('无缓存')
  })

  it('刚刚写入的缓存应返回"刚刚"', () => {
    setScheduleCache([{ courseName: '测试' }])
    expect(getCacheAge()).toBe('刚刚')
  })

  it('30 秒前应返回"刚刚"', () => {
    const timestamp = Date.now() - 30 * 1000
    localStorage.setItem(TIMESTAMP_KEY, String(timestamp))
    expect(getCacheAge()).toBe('刚刚')
  })

  it('5 分钟前应返回"5 分钟前"', () => {
    const timestamp = Date.now() - 5 * 60 * 1000
    localStorage.setItem(TIMESTAMP_KEY, String(timestamp))
    expect(getCacheAge()).toBe('5 分钟前')
  })

  it('2 小时前应返回"2 小时前"', () => {
    const timestamp = Date.now() - 2 * 60 * 60 * 1000
    localStorage.setItem(TIMESTAMP_KEY, String(timestamp))
    expect(getCacheAge()).toBe('2 小时前')
  })
})