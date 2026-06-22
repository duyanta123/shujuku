/**
 * proxyMapping.js 路由认证策略 — 单元测试
 *
 * 测试覆盖：
 * 1. 公开路由 — 无需认证（登录、课程列表）
 * 2. 需认证路由 — 管理员、学生、教师、选课等
 * 3. requiresAuth 方法 — 各类路径判断
 * 4. 边界情况 — 精确匹配 vs 前缀匹配
 * 5. 路径前缀匹配 — 子路径自动继承认证要求
 *
 * 风险背景：
 * proxyMapping 定义了 BFF 透明代理的认证策略，是所有 API 请求的安全门禁。
 * 若 requiresAuth 逻辑错误，可能导致：
 * - 公开路由被错误拦截（用户无法登录）
 * - 需认证路由被错误放行（未授权访问敏感数据）
 */
import { describe, it, expect } from 'vitest'
import { proxyMapping } from '../src/proxy/proxyMapping.js'

// ============================================================================
// 公开路由 — 无需认证
// ============================================================================
describe('公开路由 — 无需 JWT 认证', () => {
  const publicCases = [
    ['学生登录', '/api/student/login'],
    ['教师登录', '/api/teacher/login'],
    ['管理员登录', '/api/admin/login'],
    ['课程列表', '/api/course/list'],
    ['课程列表（简单版）', '/api/course/list/simple'],
  ]

  it.each(publicCases)('%s (%s) 不应要求认证', (_, path) => {
    expect(proxyMapping.requiresAuth(path)).toBe(false)
  })
})

// ============================================================================
// 需认证路由 — 精确匹配和前缀匹配
// ============================================================================
describe('需认证路由 — 需要 JWT 认证', () => {
  it('API 根路径应要求认证', () => {
    expect(proxyMapping.requiresAuth('/api')).toBe(true)
  })

  const authenticatedCases = [
    ['管理员', '/api/admin/dashboard'],
    ['管理员子路径', '/api/admin/students'],
    ['考勤签到', '/api/attendance/check-in'],
    ['考勤查询', '/api/attendance/records'],
    ['选课', '/api/selection/my'],
    ['选课退选', '/api/selection/delete/1'],
    ['学生列表', '/api/student/list'],
    ['教师列表', '/api/teacher/list'],
    ['学院管理', '/api/college/list'],
    ['学院添加', '/api/college/add'],
    ['专业管理', '/api/major/list'],
    ['专业按学院查询', '/api/major/list/by-college/1'],
    ['必修课绑定', '/api/major-required-course/bind'],
    ['实验室管理', '/api/lab/list'],
    ['成绩管理', '/api/score/list'],
    ['用户信息', '/api/user/profile'],
    ['课程教师', '/api/course-teacher/list'],
    ['Token 刷新', '/api/auth/refresh'],
    ['Token 验证', '/api/auth/validate'],
  ]

  it.each(authenticatedCases)('%s (%s) 应要求认证', (_, path) => {
    expect(proxyMapping.requiresAuth(path)).toBe(true)
  })
})

// ============================================================================
// 边界情况 — 精确匹配优先于前缀匹配
// ============================================================================
describe('路由优先级 — 精确匹配优先', () => {
  it('/api/course/list 应不要求认证（公开）', () => {
    // 公开路由的精确匹配优先于 /api/course/ 前缀匹配
    expect(proxyMapping.requiresAuth('/api/course/list')).toBe(false)
  })

  it('/api/course/list/simple 应不要求认证（公开）', () => {
    expect(proxyMapping.requiresAuth('/api/course/list/simple')).toBe(false)
  })

  it('/api/course/add 应要求认证（前缀匹配）', () => {
    // 不在 publicPaths 中，但匹配 /api/course/ 前缀
    expect(proxyMapping.requiresAuth('/api/course/add')).toBe(true)
  })

  it('/api/student/login 应不要求认证', () => {
    // 精确匹配公开路由，尽管 /api/student/ 前缀也匹配
    expect(proxyMapping.requiresAuth('/api/student/login')).toBe(false)
  })

  it('/api/teacher/login 应不要求认证', () => {
    expect(proxyMapping.requiresAuth('/api/teacher/login')).toBe(false)
  })

  it('/api/admin/login 应不要求认证', () => {
    expect(proxyMapping.requiresAuth('/api/admin/login')).toBe(false)
  })
})

// ============================================================================
// 未知路径 — 默认要求认证
// ============================================================================
describe('未知路径 — 默认安全策略', () => {
  it('不匹配任何已知模式的路径应默认要求认证', () => {
    expect(proxyMapping.requiresAuth('/api/unknown/path')).toBe(true)
  })

  it('根路径 /api 应默认要求认证', () => {
    expect(proxyMapping.requiresAuth('/api')).toBe(true)
  })

  it('非 /api 前缀路径应默认要求认证', () => {
    expect(proxyMapping.requiresAuth('/other/path')).toBe(true)
  })
})

// ============================================================================
// 公开路由列表完整性验证
// ============================================================================
describe('公开路由列表', () => {
  it('public 列表应包含所有登录端点', () => {
    expect(proxyMapping.public).toContain('/api/student/login')
    expect(proxyMapping.public).toContain('/api/teacher/login')
    expect(proxyMapping.public).toContain('/api/admin/login')
  })

  it('public 列表应包含课程列表端点', () => {
    expect(proxyMapping.public).toContain('/api/course/list')
    expect(proxyMapping.public).toContain('/api/course/list/simple')
  })

  it('authenticated 列表应包含所有受保护端点前缀', () => {
    const expectedPrefixes = [
      '/api/admin/',
      '/api/attendance/',
      '/api/college/',
      '/api/major/',
      '/api/selection/',
      '/api/student/',
      '/api/teacher/',
      '/api/score/',
      '/api/lab/',
    ]
    for (const prefix of expectedPrefixes) {
      expect(proxyMapping.authenticated).toContain(prefix)
    }
  })
})

// ============================================================================
// 特殊场景
// ============================================================================
describe('特殊场景', () => {
  it('带查询参数的路径应正确判断', () => {
    // /api/student/list?page=1 → /api/student/list
    expect(proxyMapping.requiresAuth('/api/student/list?page=1')).toBe(true)
  })
})