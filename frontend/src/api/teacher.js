import request from '../utils/request'

export function teacherLogin(data) {
  return request({
    url: '/teacher/login',
    method: 'post',
    data
  })
}

export function getTeacherList(collegeId) {
  const params = {}
  if (collegeId !== undefined && collegeId !== '') {
    params.collegeId = collegeId
  }
  return request({
    url: '/teacher/list',
    method: 'get',
    params: Object.keys(params).length > 0 ? params : undefined
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

export function resetTeacherPassword(id) {
  return request({
    url: `/teacher/reset-password/${id}`,
    method: 'post'
  })
}
