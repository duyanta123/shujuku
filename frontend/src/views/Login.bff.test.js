import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import Login from '../views/Login.vue'

const {
  mockStudentLogin,
  mockAdminLogin,
  mockFetchProfile,
  mockRouterPush,
} = vi.hoisted(() => ({
  mockStudentLogin: vi.fn(),
  mockAdminLogin: vi.fn(),
  mockFetchProfile: vi.fn(),
  mockRouterPush: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush }),
}))

vi.mock('../api/student', () => ({ studentLogin: mockStudentLogin }))
vi.mock('../api/teacher', () => ({ teacherLogin: vi.fn() }))
vi.mock('../api/admin', () => ({ adminLogin: mockAdminLogin }))
vi.mock('../stores/userStore', () => ({
  default: { fetchProfile: mockFetchProfile },
}))

const mountLogin = () => mount(Login, {
  global: {
    stubs: {
      'el-form': { template: '<div><slot /></div>' },
      'el-form-item': { template: '<div><slot /></div>' },
      'el-input': true,
      'el-button': { template: '<button @click="$attrs.onClick"><slot /></button>' },
    }
  }
})

describe('Login.vue cookie-only login', () => {
  beforeEach(() => {
    localStorage.clear()
    mockStudentLogin.mockReset()
    mockAdminLogin.mockReset()
    mockFetchProfile.mockReset()
    mockRouterPush.mockReset()
  })

  it('student login fetches profile and stores no token or id', async () => {
    mockStudentLogin.mockResolvedValue({ success: true, data: { id: 1, name: '王小明' } })
    mockFetchProfile.mockResolvedValue({ role: 'student', name: '王小明', account: 'S001' })

    const wrapper = mountLogin()
    wrapper.vm.loginForm.role = 'student'
    wrapper.vm.loginForm.account = 'S001'
    wrapper.vm.loginForm.password = '123456'

    await wrapper.vm.handleLogin()

    expect(mockFetchProfile).toHaveBeenCalled()
    expect(mockRouterPush).toHaveBeenCalledWith('/student')
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('admin login routes according to profile role', async () => {
    mockAdminLogin.mockResolvedValue({ success: true, data: { id: 1, username: 'admin' } })
    mockFetchProfile.mockResolvedValue({ role: 'admin', name: 'admin', account: 'admin' })

    const wrapper = mountLogin()
    wrapper.vm.loginForm.role = 'admin'
    wrapper.vm.loginForm.account = 'admin'
    wrapper.vm.loginForm.password = '123456'

    await wrapper.vm.handleLogin()

    expect(mockRouterPush).toHaveBeenCalledWith('/admin')
    expect(localStorage.getItem('user')).toBeNull()
  })
})
