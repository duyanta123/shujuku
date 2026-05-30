import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import StudentLayout from '../views/student/StudentLayout.vue'
import StudentCourse from '../views/student/StudentCourse.vue'
import StudentMyCourse from '../views/student/StudentMyCourse.vue'
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

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/student',
    component: StudentLayout,
    children: [
      {
        path: '',
        redirect: '/student/course'
      },
      {
        path: 'course',
        name: 'StudentCourse',
        component: StudentCourse
      },
      {
        path: 'my-course',
        name: 'StudentMyCourse',
        component: StudentMyCourse
      }
    ]
  },
  {
    path: '/teacher',
    component: TeacherLayout,
    children: [
      {
        path: '',
        redirect: '/teacher/course'
      },
      {
        path: 'course',
        name: 'TeacherCourse',
        component: TeacherCourse
      },
      {
        path: 'student-list/:courseId',
        name: 'TeacherStudentList',
        component: TeacherStudentList
      },
      {
        path: 'score',
        name: 'TeacherScore',
        component: TeacherScore
      },
      {
        path: 'attendance',
        name: 'TeacherAttendance',
        component: TeacherAttendance
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      {
        path: '',
        redirect: '/admin/student'
      },
      {
        path: 'student',
        name: 'AdminStudent',
        component: AdminStudent
      },
      {
        path: 'teacher',
        name: 'AdminTeacher',
        component: AdminTeacher
      },
      {
        path: 'course',
        name: 'AdminCourse',
        component: AdminCourse
      },
      {
        path: 'lab',
        name: 'AdminLab',
        component: AdminLab
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
