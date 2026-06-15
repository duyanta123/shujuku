import request from '../utils/request'

export function getCollegeList(params) {
  return request({ url: '/college/list', method: 'get', params })
}

export function addCollege(data) {
  return request({ url: '/college/add', method: 'post', data })
}

export function updateCollege(data) {
  return request({ url: '/college/update', method: 'put', data })
}

export function deleteCollege(id) {
  return request({ url: `/college/delete/${id}`, method: 'delete' })
}