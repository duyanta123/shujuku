import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import StudentLayout from '../views/student/StudentLayout.vue'
import StudentCourse from '../views/student/StudentCourse.vue'
import StudentMyCourse from '../views/student/StudentMyCourse.vue'
import StudentSchedule from '../views/student/StudentSchedule.vue'
import StudentAttendance from '../views/student/StudentAttendance.vue'
import StudentAttendanceHistory from '../views/student/StudentAttendanceHistory.vue'
import TeacherLayout from '../views/teacher/TeacherLayout.vue'
import TeacherCourse from '../views/teacher/TeacherCourse.vue'
import TeacherStudentList from '../views/teacher/TeacherStudentList.vue'
import TeacherScore from '../views/teacher/TeacherScore.vue'
import TeacherAttendance from '../views/teacher/TeacherAttendance.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminStudent from '../views/admin/AdminStudent.vue'
import AdminTeacher from '../views/admin/AdminTeacher.vue'
import AdminCourse from '../views/admin/AdminCourse.vue'
import AdminLab from '../views/admin/AdminLab.vue'
import AdminCollegeMajor from '../views/admin/AdminCollegeMajor.vue'
import userStore from '../stores/userStore'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: Login },
  {
    path: '/student',
    component: StudentLayout,
    children: [
      { path: '', redirect: '/student/course' },
      { path: 'course', name: 'StudentCourse', component: StudentCourse },
      { path: 'my-course', name: 'StudentMyCourse', component: StudentMyCourse },
      { path: 'schedule', name: 'StudentSchedule', component: StudentSchedule },
      { path: 'attendance', name: 'StudentAttendance', component: StudentAttendance },
      { path: 'attendance-history', name: 'StudentAttendanceHistory', component: StudentAttendanceHistory },
    ],
  },
  {
    path: '/teacher',
    component: TeacherLayout,
    children: [
      { path: '', redirect: '/teacher/course' },
      { path: 'course', name: 'TeacherCourse', component: TeacherCourse },
      { path: 'student-list/:courseId', name: 'TeacherStudentList', component: TeacherStudentList },
      { path: 'score', name: 'TeacherScore', component: TeacherScore },
      { path: 'attendance', name: 'TeacherAttendance', component: TeacherAttendance },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      { path: '', redirect: '/admin/student' },
      { path: 'student', name: 'AdminStudent', component: AdminStudent },
      { path: 'teacher', name: 'AdminTeacher', component: AdminTeacher },
      { path: 'course', name: 'AdminCourse', component: AdminCourse },
      { path: 'lab', name: 'AdminLab', component: AdminLab },
      { path: 'college-major', name: 'AdminCollegeMajor', component: AdminCollegeMajor },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function requiredRole(path) {
  if (path.startsWith('/student')) return 'student'
  if (path.startsWith('/teacher')) return 'teacher'
  if (path.startsWith('/admin')) return 'admin'
  return ''
}

router.beforeEach(async (to) => {
  const role = requiredRole(to.path)

  if (to.path === '/login') {
    try {
      await userStore.ensureProfile({
        skipAuthRefresh: true,
        skipAuthRedirect: true,
        skipErrorMessage: true,
      })
      return `/${userStore.role}`
    } catch {
      userStore.reset()
      localStorage.removeItem('user')
      return true
    }
  }

  if (!role) return true

  try {
    await userStore.ensureProfile()
  } catch {
    userStore.reset()
    localStorage.removeItem('user')
    return '/login'
  }

  if (userStore.role === role) return true
  return userStore.role ? `/${userStore.role}` : '/login'
})

export default router
