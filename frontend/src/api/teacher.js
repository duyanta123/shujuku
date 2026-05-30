import request from '../utils/request'

export function teacherLogin(data) {
  return request({
    url: '/teacher/login',
    method: 'post',
    data
  })
}

export function getTeacherList() {
  return request({
    url: '/teacher/list',
    method: 'get'
  })
}

export function addTeacher(data) {
  return request({
    url: '/teacher/add',
    method: 'post',
    data
  })
}

export function updateTeacher(data) {
  return request({
    url: '/teacher/update',
    method: 'put',
    data
  })
}

export function deleteTeacher(id) {
  return request({
    url: `/teacher/delete/${id}`,
    method: 'delete'
  })
}
