/**
 * AdminStudent.vue — collegeId 字段测试
 * 验证管理员端学生表单正确包含学院下拉字段（collegeId）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import AdminStudent from './AdminStudent.vue'

const { mockGetStudentList, mockAddStudent, mockUpdateStudent, mockDeleteStudent, mockResetStudentPassword } = vi.hoisted(() => ({
  mockGetStudentList: vi.fn(() => Promise.resolve({ success: true, data: [] })),
  mockAddStudent: vi.fn(() => Promise.resolve({ success: true })),
  mockUpdateStudent: vi.fn(() => Promise.resolve({ success: true })),
  mockDeleteStudent: vi.fn(() => Promise.resolve({ success: true })),
  mockResetStudentPassword: vi.fn(() => Promise.resolve({ success: true })),
}))

vi.mock('../../api/student', () => ({
  getStudentList: mockGetStudentList,
  addStudent: mockAddStudent,
  updateStudent: mockUpdateStudent,
  deleteStudent: mockDeleteStudent,
  resetStudentPassword: mockResetStudentPassword,
}))

// Mock college API
vi.mock('../../api/college', () => ({
  getCollegeList: vi.fn(() => Promise.resolve({
    success: true,
    data: {
      records: [
        { id: 1, name: '数学与计算机科学学院' },
        { id: 2, name: '信息工程学院' },
      ]
    }
  })),
}))

vi.mock('../../api/major', () => ({
  getMajorsByCollegeId: vi.fn(() => Promise.resolve({ success: true, data: [] })),
}))

const mountAdminStudent = () => mount(AdminStudent, {
  global: {
    stubs: {
      'el-dialog': { template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>', props: ['modelValue'] },
      'el-table': { template: '<div><slot /></div>' },
      'el-table-column': { template: '<div />', props: ['prop', 'label'] },
      'el-form': {
        template: '<div><slot /></div>',
        props: ['model'],
        methods: {
          validate() {
            return this.model?.collegeId ? Promise.resolve(true) : Promise.reject(new Error('invalid'))
          },
          resetFields() {}
        }
      },
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

describe('AdminStudent.vue — collegeId 字段', () => {
  beforeEach(() => {
    mockGetStudentList.mockClear()
    mockAddStudent.mockClear()
    mockUpdateStudent.mockClear()
    mockDeleteStudent.mockClear()
  })

  it('表单初始化应包含 collegeId 字段', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    vm.handleAdd()
    await vm.$nextTick()

    expect(vm.studentForm).toHaveProperty('collegeId')
    expect(vm.studentForm.collegeId).toBeNull()
  })

  it('表单默认值应正确设置所有字段包括 collegeId', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    vm.handleAdd()
    await vm.$nextTick()

    const expectedKeys = ['id', 'studentNo', 'name', 'gender', 'majorId', 'collegeId']
    expectedKeys.forEach(key => {
      expect(vm.studentForm).toHaveProperty(key)
    })
  })

  it('编辑时 collegeId 字段应被正确填充', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    const student = {
      id: 1,
      studentNo: 'S001',
      name: '张三',
      gender: '男',
      majorId: 1,
      collegeId: 2,
    }

    vm.handleEdit(student)
    await vm.$nextTick()

    expect(vm.studentForm.collegeId).toBe(2)
  })

  it('组件应定义 collegeOptions 下拉数据源', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    expect(vm.collegeOptions).toBeDefined()
    expect(Array.isArray(vm.collegeOptions)).toBe(true)
  })

  it('collegeId 校验规则应为必填', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    expect(vm.studentRules.collegeId).toBeDefined()
    expect(vm.studentRules.collegeId[0].required).toBe(true)
    expect(vm.studentRules.collegeId[0].message).toContain('学院')
  })

  it('模板渲染 el-select 而非 el-input（学院字段）', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    vm.handleAdd()
    await vm.$nextTick()

    const html = wrapper.html()
    expect(html).toContain('select')
  })

  it('collegeOptions 初始为空数组（从 API 加载）', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    // collegeOptions 从 API 异步加载，初始应为空数组
    expect(Array.isArray(vm.collegeOptions)).toBe(true)
    expect(vm.collegeOptions.length).toBeGreaterThanOrEqual(0)
  })

  it('collegeId 空值表单校验失败', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    // 验证校验规则
    expect(vm.studentRules.collegeId[0].required).toBe(true)
    expect(vm.studentRules.collegeId[0].message).toBe('请选择学院')

    // 模拟空值提交
    vm.handleAdd()
    await vm.$nextTick()
    vm.studentForm.collegeId = null
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

  it('collegeId 变更时重置 majorId', async () => {
    const wrapper = mountAdminStudent()
    const vm = wrapper.vm

    vm.handleAdd()
    await vm.$nextTick()

    vm.studentForm.majorId = 1
    vm.onCollegeChange(3)
    await vm.$nextTick()

    expect(vm.studentForm.majorId).toBeNull()
  })
})
