/**
 * Login.vue — BFF 模式应存储 userId
 * Bug: BFF模式下 localStorage 缺少 userId，导致考勤API studentId 为 undefined
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import Login from '../views/Login.vue'

// 使用 vi.hoisted 解决 vi.mock 提升问题
const { mockStudentLogin, mockTeacherLogin, mockAdminLogin } = vi.hoisted(() => ({
  mockStudentLogin: vi.fn(),
  mockTeacherLogin: vi.fn(),
  mockAdminLogin: vi.fn(),
}))

vi.mock('vue-router', () => {
  const mockRouter = { push: vi.fn(), replace: vi.fn(), beforeEach: vi.fn(), afterEach: vi.fn() }
  return {
    useRouter: () => mockRouter,
    useRoute: () => ({ path: '/', query: {}, params: {} }),
    createRouter: () => mockRouter,
    createWebHistory: vi.fn(),
    createMemoryHistory: vi.fn(),
  }
})

vi.mock('../api/student', () => ({ studentLogin: mockStudentLogin }))
vi.mock('../api/teacher', () => ({ teacherLogin: mockTeacherLogin }))
vi.mock('../api/admin', () => ({ adminLogin: mockAdminLogin }))

const mountLogin = () => mount(Login, {
  global: {
    stubs: {
      'el-form': { template: '<div><slot /></div>' },
      'el-form-item': { template: '<div><slot /></div>' },
      'el-input': true,
      'el-button': { template: '<button @click="$attrs.onClick"><slot /></button>' },
      'router-link': true,
    }
  }
})

const createBffResponse = (id, extra = {}) => ({
  success: true,
  message: '登录成功',
  data: { id, userId: String(id), tokenExpireTime: Date.now() + 30 * 60 * 1000, ...extra },
})

describe('Login.vue — BFF 模式登录应存储 userId', () => {
  beforeEach(() => {
    localStorage.clear()
    mockStudentLogin.mockReset()
    mockTeacherLogin.mockReset()
    mockAdminLogin.mockReset()
  })

  it('BFF 模式下学生登录成功后 localStorage 应包含 id', async () => {
    mockStudentLogin.mockResolvedValue(createBffResponse(1, {
      studentNo: 'S001', name: '王小明', gender: '男', major: '计算机科学与技术',
      role: 'student', username: 'S001',
    }))

    const wrapper = mountLogin()
    wrapper.vm.loginForm.role = 'student'
    wrapper.vm.loginForm.account = 'S001'
    wrapper.vm.loginForm.password = '123456'

    await wrapper.vm.handleLogin()

    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    expect(stored._bffMode).toBe(true)
    expect(stored.token).toBe('bff-cookie')
    expect(stored.id).toBeDefined()
    expect(stored.id).toBe(1)
  })

  it('BFF 模式下管理员登录成功后 localStorage 应包含 id', async () => {
    mockAdminLogin.mockResolvedValue(createBffResponse(1, {
      username: 'admin', role: 'admin',
    }))

    const wrapper = mountLogin()
    wrapper.vm.loginForm.role = 'admin'
    wrapper.vm.loginForm.account = 'admin'
    wrapper.vm.loginForm.password = '123456'

    await wrapper.vm.handleLogin()

    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    expect(stored._bffMode).toBe(true)
    expect(stored.id).toBeDefined()
    expect(stored.id).toBe(1)
  })
})