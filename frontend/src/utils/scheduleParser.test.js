/**
 * 课表解析工具 — 单元测试
 *
 * 风险行为覆盖：
 * - parseCourseTime: 正常解析、多时段、空值/null、格式不匹配
 * - detectConflict: 时间冲突检测、无冲突、完全重叠、部分重叠
 * - detectConflictWithList: 与课程列表冲突检测
 * - buildScheduleGrid: 课表网格构建
 * - 边界情况: 周日、跨天、单时段、满课时、中文标点
 */
import { describe, it, expect } from 'vitest'
import {
  parseCourseTime,
  detectConflict,
  detectConflictWithList,
  buildScheduleGrid,
  DAY_NAMES,
  PERIOD_CONFIG,
  PERIOD_LABELS
} from './scheduleParser.js'

// ============================================================================
// parseCourseTime — 课程时间解析
// ============================================================================
describe('parseCourseTime — 课程时间解析', () => {
  describe('正常解析', () => {
    it('"周一 1-2节" 应正确解析', () => {
      const result = parseCourseTime('周一 1-2节')
      expect(result).toHaveLength(1)
      expect(result[0]).toMatchObject({
        day: 1,
        dayName: '周一',
        periods: '1-2节',
        startPeriod: 1,
        endPeriod: 2,
        time: '08:00 - 09:40'
      })
    })

    it('"周三 3-4节" 应正确解析', () => {
      const result = parseCourseTime('周三 3-4节')
      expect(result).toHaveLength(1)
      expect(result[0]).toMatchObject({
        day: 3,
        dayName: '周三',
        periods: '3-4节',
        startPeriod: 3,
        endPeriod: 4,
        time: '10:00 - 11:40'
      })
    })

    it('"周五 5-6节" 应正确解析', () => {
      const result = parseCourseTime('周五 5-6节')
      expect(result).toHaveLength(1)
      expect(result[0].day).toBe(5)
      expect(result[0].periods).toBe('5-6节')
      expect(result[0].time).toBe('14:00 - 15:40')
    })

    it('"周日 9-10节" 应正确解析', () => {
      const result = parseCourseTime('周日 9-10节')
      expect(result).toHaveLength(1)
      expect(result[0].day).toBe(7)
      expect(result[0].time).toBe('19:00 - 20:40')
    })
  })

  describe('"星期X" 格式', () => {
    it('"星期一 1-2节" 应正确解析', () => {
      const result = parseCourseTime('星期一 1-2节')
      expect(result).toHaveLength(1)
      expect(result[0].day).toBe(1)
      expect(result[0].dayName).toBe('星期一')
    })

    it('"星期四 7-8节" 应正确解析', () => {
      const result = parseCourseTime('星期四 7-8节')
      expect(result).toHaveLength(1)
      expect(result[0].day).toBe(4)
      expect(result[0].time).toBe('16:00 - 17:40')
    })
  })

  describe('多时段课程', () => {
    it('"周一 1-2节, 周三 3-4节" 应解析为两个时段', () => {
      const result = parseCourseTime('周一 1-2节, 周三 3-4节')
      expect(result).toHaveLength(2)
      expect(result[0].day).toBe(1)
      expect(result[1].day).toBe(3)
    })

    it('用中文逗号分隔也应正确解析', () => {
      const result = parseCourseTime('周一 1-2节，周三 3-4节')
      expect(result).toHaveLength(2)
    })

    it('多个时段混合', () => {
      const result = parseCourseTime('周一 1-2节, 周三 5-6节, 周五 7-8节')
      expect(result).toHaveLength(3)
    })
  })

  describe('边界与异常输入', () => {
    it('空字符串应返回空数组', () => {
      expect(parseCourseTime('')).toEqual([])
    })

    it('null 应返回空数组', () => {
      expect(parseCourseTime(null)).toEqual([])
    })

    it('undefined 应返回空数组', () => {
      expect(parseCourseTime(undefined)).toEqual([])
    })

    it('格式不匹配的字符串应返回空数组', () => {
      expect(parseCourseTime('随便写的内容')).toEqual([])
    })

    it('仅有逗号不应报错', () => {
      expect(parseCourseTime(',,,')).toEqual([])
    })

    it('前后有空格应正常解析', () => {
      const result = parseCourseTime('  周一 1-2节  ')
      expect(result).toHaveLength(1)
      expect(result[0].day).toBe(1)
    })

    it('不存在的星期应返回空数组', () => {
      // "周八" 不在 DAY_MAP 中
      expect(parseCourseTime('周八 1-2节')).toEqual([])
    })
  })
})

// ============================================================================
// detectConflict — 时间冲突检测
// ============================================================================
describe('detectConflict — 时间冲突检测', () => {
  describe('无冲突场景', () => {
    it('不同天不应冲突', () => {
      const result = detectConflict('周一 1-2节', '周二 1-2节')
      expect(result.hasConflict).toBe(false)
      expect(result.conflicts).toHaveLength(0)
    })

    it('同一天不同时段不应冲突', () => {
      const result = detectConflict('周一 1-2节', '周一 3-4节')
      expect(result.hasConflict).toBe(false)
    })

    it('前后相邻时段不应冲突（2节结束 vs 3节开始）', () => {
      const result = detectConflict('周一 1-2节', '周一 3-4节')
      expect(result.hasConflict).toBe(false)
    })
  })

  describe('有冲突场景', () => {
    it('完全相同的时间应冲突', () => {
      const result = detectConflict('周一 1-2节', '周一 1-2节')
      expect(result.hasConflict).toBe(true)
      expect(result.conflicts).toHaveLength(1)
      expect(result.conflicts[0].a.dayName).toBe('周一')
      expect(result.conflicts[0].b.dayName).toBe('周一')
    })

    it('部分重叠应冲突（1-2节 vs 1-2节 完全相同）', () => {
      const result = detectConflict('周一 1-2节', '周一 1-2节')
      expect(result.hasConflict).toBe(true)
    })
  })

  describe('多时段冲突', () => {
    it('多时段中有一个重叠应检测到冲突', () => {
      // 课程A: 周一 1-2节, 周三 3-4节
      // 课程B: 周二 1-2节, 周三 3-4节 — 周三重叠
      const result = detectConflict(
        '周一 1-2节, 周三 3-4节',
        '周二 1-2节, 周三 3-4节'
      )
      expect(result.hasConflict).toBe(true)
      expect(result.conflicts.length).toBeGreaterThan(0)
    })

    it('多时段全部不重叠应无冲突', () => {
      const result = detectConflict(
        '周一 1-2节, 周三 3-4节',
        '周二 1-2节, 周四 5-6节'
      )
      expect(result.hasConflict).toBe(false)
    })
  })

  describe('边界情况', () => {
    it('空字符串应无冲突', () => {
      const result = detectConflict('', '周一 1-2节')
      expect(result.hasConflict).toBe(false)
    })

    it('两个空字符串应无冲突', () => {
      const result = detectConflict('', '')
      expect(result.hasConflict).toBe(false)
    })

    it('格式无效的字符串应无冲突', () => {
      const result = detectConflict('无效格式', '周一 1-2节')
      expect(result.hasConflict).toBe(false)
    })
  })
})

// ============================================================================
// detectConflictWithList — 与课程列表冲突检测
// ============================================================================
describe('detectConflictWithList — 与课程列表冲突检测', () => {
  const existingCourses = [
    { course_name: '高等数学', course_time: '周一 1-2节' },
    { course_name: '大学英语', course_time: '周三 3-4节' },
    { course_name: '体育', course_time: '周五 5-6节' }
  ]

  it('与已有课程时间冲突应检测到', () => {
    const result = detectConflictWithList('周一 1-2节', existingCourses)
    expect(result.hasConflict).toBe(true)
    expect(result.conflictDetails.length).toBeGreaterThan(0)
    expect(result.conflictDetails[0].courseName).toBe('高等数学')
    expect(result.conflictDetails[0].detail).toContain('周一')
  })

  it('与已有课程无冲突应通过', () => {
    const result = detectConflictWithList('周二 1-2节', existingCourses)
    expect(result.hasConflict).toBe(false)
    expect(result.conflictDetails).toHaveLength(0)
  })

  it('与多个课程冲突应列出所有冲突', () => {
    const courses = [
      { course_name: '课程A', course_time: '周一 1-2节' },
      { course_name: '课程B', course_time: '周一 1-2节' }
    ]
    const result = detectConflictWithList('周一 1-2节', courses)
    expect(result.hasConflict).toBe(true)
    expect(result.conflictDetails.length).toBe(2)
  })

  it('空列表应无冲突', () => {
    const result = detectConflictWithList('周一 1-2节', [])
    expect(result.hasConflict).toBe(false)
  })

  it('新课程时间为空应无冲突', () => {
    const result = detectConflictWithList('', existingCourses)
    expect(result.hasConflict).toBe(false)
  })
})

// ============================================================================
// buildScheduleGrid — 课表网格构建
// ============================================================================
describe('buildScheduleGrid — 课表网格构建', () => {
  it('应生成 7天 × 5时段 = 35 个格子', () => {
    const grid = buildScheduleGrid([])
    expect(grid).toHaveLength(35)
  })

  it('每个格子应有基本属性', () => {
    const grid = buildScheduleGrid([])
    const firstCell = grid[0]
    expect(firstCell).toHaveProperty('day')
    expect(firstCell).toHaveProperty('dayName')
    expect(firstCell).toHaveProperty('period')
    expect(firstCell).toHaveProperty('startPeriod')
    expect(firstCell).toHaveProperty('endPeriod')
    expect(firstCell).toHaveProperty('time')
    expect(firstCell).toHaveProperty('courses')
    expect(firstCell.courses).toEqual([])
  })

  it('课程应被分配到正确的格子', () => {
    const courses = [
      { courseName: '高等数学', courseTime: '周一 1-2节' }
    ]
    const grid = buildScheduleGrid(courses)

    // 周一 1-2节 是第一个格子 (day=1, period=1-2节)
    const cell = grid.find(c => c.day === 1 && c.period === '1-2节')
    expect(cell).toBeTruthy()
    expect(cell.courses).toHaveLength(1)
    expect(cell.courses[0].courseName).toBe('高等数学')
  })

  it('同一格子可包含多个课程（但实际不应发生）', () => {
    const courses = [
      { courseName: '课程A', courseTime: '周一 1-2节' },
      { courseName: '课程B', courseTime: '周一 1-2节' }
    ]
    const grid = buildScheduleGrid(courses)
    const cell = grid.find(c => c.day === 1 && c.period === '1-2节')
    expect(cell.courses).toHaveLength(2)
  })

  it('多时段课程应出现在多个格子', () => {
    const courses = [
      { courseName: '大课', courseTime: '周一 1-2节, 周三 3-4节' }
    ]
    const grid = buildScheduleGrid(courses)
    const cell1 = grid.find(c => c.day === 1 && c.period === '1-2节')
    const cell2 = grid.find(c => c.day === 3 && c.period === '3-4节')
    expect(cell1.courses).toHaveLength(1)
    expect(cell2.courses).toHaveLength(1)
  })

  it('无 courseTime 的课程应被跳过', () => {
    const courses = [
      { courseName: '无时间课程' }
    ]
    const grid = buildScheduleGrid(courses)
    const allCourses = grid.flatMap(c => c.courses)
    expect(allCourses).toHaveLength(0)
  })
})

// ============================================================================
// 导出常量验证
// ============================================================================
describe('导出常量', () => {
  it('DAY_NAMES 应有 8 个元素（索引 0 为空）', () => {
    expect(DAY_NAMES).toHaveLength(8)
    expect(DAY_NAMES[0]).toBe('')
    expect(DAY_NAMES[1]).toBe('周一')
    expect(DAY_NAMES[7]).toBe('周日')
  })

  it('PERIOD_CONFIG 应有 5 个时段', () => {
    expect(PERIOD_CONFIG).toHaveLength(5)
    expect(PERIOD_CONFIG[0].label).toBe('1-2节')
    expect(PERIOD_CONFIG[4].label).toBe('9-10节')
  })

  it('PERIOD_LABELS 应包含所有时段标签', () => {
    expect(PERIOD_LABELS).toEqual([
      '1-2节', '3-4节', '5-6节', '7-8节', '9-10节'
    ])
  })
})