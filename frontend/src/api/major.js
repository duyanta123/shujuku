import request from '../utils/request'

export function getMajorList(params) {
  return request({ url: '/major/list', method: 'get', params })
}

export function getMajorsByCollegeId(collegeId) {
  return request({ url: `/major/list/by-college/${collegeId}`, method: 'get' })
}

export function addMajor(data) {
  return request({ url: '/major/add', method: 'post', data })
}

export function updateMajor(data) {
  return request({ url: '/major/update', method: 'put', data })
}

export function deleteMajor(id) {
  return request({ url: `/major/delete/${id}`, method: 'delete' })
}