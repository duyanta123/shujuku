import request from '../utils/request'

export function studentLogin(data) {
  return request({
    url: '/student/login',
    method: 'post',
    data
  })
}

export function getStudentList() {
  return request({
    url: '/student/list',
    method: 'get'
  })
}

export function addStudent(data) {
  return request({
    url: '/student/add',
    method: 'post',
    data
  })
}

export function updateStudent(data) {
  return request({
    url: '/student/update',
    method: 'put',
    data
  })
}

export function deleteStudent(id) {
  return request({
    url: `/student/delete/${id}`,
    method: 'delete'
  })
}
