/**
 * scheduleEventBus.js 单元测试 — 课表事件总线
 *
 * 测试覆盖：
 * 1. notifyScheduleUpdate — 写入 localStorage 作为兜底（始终执行）
 * 2. onScheduleUpdate — 返回取消监听的函数
 * 3. 边界情况 — localStorage 值类型验证
 *
 * 风险背景：
 * scheduleEventBus.js 是课表更新的核心通信机制。选课/退课操作后需要通知
 * 课表页面刷新，若通信失败会导致课表数据不同步，用户看到过期数据。
 * localStorage 写入是 BroadcastChannel 的兜底机制，也是跨 Tab 通知的基础。
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { notifyScheduleUpdate, onScheduleUpdate } from './scheduleEventBus.js'

beforeEach(() => {
  localStorage.clear()
})

// ============================================================================
// notifyScheduleUpdate — localStorage 写入
// ============================================================================
describe('notifyScheduleUpdate — 写入 localStorage', () => {
  it('应写入 schedule_bust 键', () => {
    notifyScheduleUpdate()

    const value = localStorage.getItem('schedule_bust')
    expect(value).toBeTruthy()
  })

  it('写入的值应为有效数字时间戳', () => {
    notifyScheduleUpdate()

    const value = localStorage.getItem('schedule_bust')
    const timestamp = Number(value)
    expect(Number.isNaN(timestamp)).toBe(false)
    expect(timestamp).toBeGreaterThan(0)
    // 时间戳应在合理范围内（当前时间前后 1 秒）
    expect(Math.abs(timestamp - Date.now())).toBeLessThan(1000)
  })

  it('多次调用应更新值', () => {
    notifyScheduleUpdate()
    const firstValue = localStorage.getItem('schedule_bust')

    // 等待一小段时间确保时间戳不同
    const start = Date.now()
    while (Date.now() - start < 10) { /* busy wait */ }

    notifyScheduleUpdate()
    const secondValue = localStorage.getItem('schedule_bust')

    expect(firstValue).toBeTruthy()
    expect(secondValue).toBeTruthy()
    expect(firstValue).not.toBe(secondValue)
  })
})

// ============================================================================
// onScheduleUpdate — 监听注册
// ============================================================================
describe('onScheduleUpdate — 监听注册', () => {
  it('应返回一个函数（取消监听）', () => {
    const callback = () => {}
    const unsubscribe = onScheduleUpdate(callback)

    expect(typeof unsubscribe).toBe('function')
  })

  it('返回的取消函数调用不应抛出异常', () => {
    const callback = () => {}
    const unsubscribe = onScheduleUpdate(callback)

    expect(() => unsubscribe()).not.toThrow()
  })

  it('多次监听应返回不同的取消函数', () => {
    const callback = () => {}
    const unsub1 = onScheduleUpdate(callback)
    const unsub2 = onScheduleUpdate(callback)

    expect(unsub1).not.toBe(unsub2)
  })
})

// ============================================================================
// 集成场景
// ============================================================================
describe('集成场景', () => {
  it('notify 后 localStorage 应有值', () => {
    expect(localStorage.getItem('schedule_bust')).toBeNull()

    notifyScheduleUpdate()

    expect(localStorage.getItem('schedule_bust')).toBeTruthy()
  })

  it('clear 后重新 notify 应能写入', () => {
    notifyScheduleUpdate()
    localStorage.clear()

    expect(localStorage.getItem('schedule_bust')).toBeNull()

    notifyScheduleUpdate()
    expect(localStorage.getItem('schedule_bust')).toBeTruthy()
  })
})