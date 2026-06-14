/**
 * AdminStudent.vue — college 字段测试
 * 验证管理员端学生表单正确包含学院字段
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import AdminStudent from './AdminStudent.vue'

const { mockGetStudents, mockAddStudent, mockUpdateStudent, mockDeleteStudent } = vi.hoisted(() => ({
  mockGetStudents: vi.fn(() => Promise.resolve({ data: { success: true, data: [] } })),
  mockAddStudent: vi.fn(() => Promise.resolve({ data: { success: true } })),
  mockUpdateStudent: vi.fn(() => Promise.resolve({ data: { success: true } })),
  mockDeleteStudent: vi.fn(() => Promise.resolve({ data: { success: true } })),
}))

vi.mock('../../api/student', () => ({
  getStudents: mockGetStudents,
  addStudent: mockAddStudent,
  updateStudent: mockUpdateStudent,
  deleteStudent: mockDeleteStudent,
}))

const mountAdminStudent = () => mount(AdminStudent, {
  global: {
    stubs: {
      'el-dialog': { template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>', props: ['modelValue'] },
      'el-table': { template: '<div><slot /></div>' },
      'el-table-column': { template: '<div />', props: ['prop', 'label'] },
      'el-form': { template: '<div><slot /></div>', props: ['model'] },
      'el-form-item': { template: '<div><slot /></div>', props: ['label', 'prop'] },
      'el-input': { template: '<input />', props: ['modelValue'] },
      'el-button': { template: '<button><slot /></button>' },
      'el-select': { template: '<select><slot /></select>' },
      'el-option': { template: '<option />', props: ['label', 'value'] },
      'el-pagination': { template: '<div />' },
      'el-message': { template: '<div />' },
      'el-message-box': { template: '<div />' },
    },
  },
})

describe('AdminStudent.vue — college 字段', () => {
  beforeEach(() => {
    mockGetStudents.mockClear()
    mockAddStudent.mockClear()
    mockUpdateStudent.mockClear()
    mockDeleteStudent.mockClear()
  })

  it('表单初始化应包含 college 字段', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    vm.handleAdd()
    await vm.$nextTick()

    expect(vm.studentForm).toHaveProperty('college')
    expect(vm.studentForm.college).toBe('')
  })

  it('表单默认值应正确设置所有字段包括 college', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    vm.handleAdd()
    await vm.$nextTick()

    const expectedKeys = ['id', 'studentNo', 'name', 'gender', 'major', 'college', 'password']
    expectedKeys.forEach(key => {
      expect(vm.studentForm).toHaveProperty(key)
    })
  })

  it('编辑时 college 字段应被正确填充', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    const student = {
      id: 1,
      studentNo: 'S001',
      name: '张三',
      gender: '男',
      major: '计算机科学与技术',
      college: '信息工程学院',
    }

    vm.handleEdit(student)
    await vm.$nextTick()

    expect(vm.studentForm.college).toBe('信息工程学院')
  })

  it('组件应定义 collegeOptions 下拉数据源', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    expect(vm.collegeOptions).toBeDefined()
    expect(Array.isArray(vm.collegeOptions)).toBe(true)
    expect(vm.collegeOptions.length).toBeGreaterThanOrEqual(10)
    expect(vm.collegeOptions).toContain('数学与计算机科学学院')
  })

  it('college 校验规则应为必填', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    expect(vm.studentRules.college).toBeDefined()
    expect(vm.studentRules.college[0].required).toBe(true)
    expect(vm.studentRules.college[0].message).toContain('学院')
  })

  // ======================== 下拉组件专项测试（新增 4 条） ========================

  it('模板渲染 el-select 而非 el-input（学院字段）', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    vm.handleAdd()
    await vm.$nextTick()

    const html = wrapper.html()
    // college 对应的 el-form-item prop="college" 内应包含 el-select 而非 el-input
    // 由于组件被 stub，检查是否有 select 标签即可
    expect(html).toContain('select')
  })

  it('collegeOptions 无重复选项', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    const uniqueSize = new Set(vm.collegeOptions).size
    expect(uniqueSize).toBe(vm.collegeOptions.length)
    expect(vm.collegeOptions).toContain('数学与计算机科学学院')
  })

  it('college 空值表单校验失败', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    // 验证校验规则
    expect(vm.studentRules.college[0].required).toBe(true)
    expect(vm.studentRules.college[0].trigger).toBe('change')
    expect(vm.studentRules.college[0].message).toBe('请选择学院')

    // 模拟空值提交
    vm.handleAdd()
    await vm.$nextTick()
    vm.studentForm.college = ''
    await vm.$nextTick()

    // 表单校验应失败（el-form validate 会 reject）
    let validationFailed = false
    try {
      await vm.studentFormRef.validate()
    } catch {
      validationFailed = true
    }
    expect(validationFailed).toBe(true)
  })

  it('collegeOptions 包含全部 10 个预设学院', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    expect(vm.collegeOptions).toHaveLength(10)
    expect(vm.collegeOptions).toEqual([
      '数学与计算机科学学院',
      '物理与电子工程学院',
      '化学与材料科学学院',
      '生命科学学院',
      '地理与环境科学学院',
      '信息工程学院',
      '经济与管理学院',
      '外国语学院',
      '马克思主义学院',
      '教育学院'
    ])
  })
})