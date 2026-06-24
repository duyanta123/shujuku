/**
 * @typedef {Object} NavItem    导航项
 * @property {string} to        路由路径（如 '/admin/student'）
 * @property {string} icon      固定图标 key
 * @property {string} label     导航显示文本
 */

/**
 * @typedef {Object} LayoutConfig         布局配置
 * @property {'admin'|'teacher'|'student'} role   角色标识，透传给 AvatarDialog
 * @property {string} brandSub            侧边栏副标题（如 '管理中心'）
 * @property {string} userNameFallback    用户名为空时的兜底文案
 * @property {string} userRolePrefix      角色信息行前缀（如 '管理员'）
 * @property {string} accountFallback     账号为空时的兜底值
 * @property {string} placeholder         头像加载失败时的 data URI（来自 avatarPlaceholder）
 * @property {NavItem[]} navItems         导航项列表
 */

// 角色布局配置 — 统一管理导航项、品牌信息、头像占位符
import { adminPlaceholder, teacherPlaceholder, studentPlaceholder } from '@/assets/avatarPlaceholder'

// 管理员端配置
export const adminLayoutConfig = {
  role: 'admin',
  brandSub: '管理中心',
  userNameFallback: '管理员',
  userRolePrefix: '管理员',
  accountFallback: 'admin',
  placeholder: adminPlaceholder,
  navItems: [
    { to: '/admin/student', icon: 'student', label: '学生管理' },
    { to: '/admin/teacher', icon: 'teacher', label: '教师管理' },
    { to: '/admin/course', icon: 'course', label: '课程管理' },
    { to: '/admin/lab', icon: 'lab', label: '实验室管理' },
    { to: '/admin/college-major', icon: 'college', label: '学院专业管理' },
  ],
}

// 教师端配置
export const teacherLayoutConfig = {
  role: 'teacher',
  brandSub: '教师端',
  userNameFallback: '教师',
  userRolePrefix: '教师端',
  accountFallback: 'T001',
  placeholder: teacherPlaceholder,
  navItems: [
    { to: '/teacher/course', icon: 'course', label: '我的课程' },
    { to: '/teacher/score', icon: 'score', label: '成绩录入' },
    { to: '/teacher/attendance', icon: 'attendance', label: '考勤录入' },
  ],
}

// 学生端配置
export const studentLayoutConfig = {
  role: 'student',
  brandSub: '学生端',
  userNameFallback: '学生',
  userRolePrefix: '学生端',
  accountFallback: '',
  placeholder: studentPlaceholder,
  navItems: [
    { to: '/student/course', icon: 'course', label: '课程列表' },
    { to: '/student/my-course', icon: 'myCourse', label: '我的课程' },
    { to: '/student/schedule', icon: 'schedule', label: '我的课表' },
    { to: '/student/attendance', icon: 'attendance', label: '课堂签到' },
    { to: '/student/attendance-history', icon: 'history', label: '考勤记录' },
  ],
}
