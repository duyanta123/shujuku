import request from '../utils/request'

export function studentLogin(data) {
  return request({
    url: '/student/login',
    method: 'post',
    data
  })
}

export function getStudentList(collegeId) {
  const params = {}
  if (collegeId !== undefined && collegeId !== '') {
    params.collegeId = collegeId
  }
  return request({
    url: '/student/list',
    method: 'get',
    params: Object.keys(params).length > 0 ? params : undefined
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

export function resetStudentPassword(id) {
  return request({
    url: `/student/reset-password/${id}`,
    method: 'post'
  })
}
