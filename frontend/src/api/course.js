import request from '../utils/request'

export function getCourseList() {
  return request({
    url: '/course/list',
    method: 'get'
  })
}

export function getCourseListSimple() {
  return request({
    url: '/course/list/simple',
    method: 'get'
  })
}

export function addCourse(data) {
  return request({
    url: '/course/add',
    method: 'post',
    data
  })
}

export function updateCourse(data) {
  return request({
    url: '/course/update',
    method: 'put',
    data
  })
}

export function deleteCourse(id) {
  return request({
    url: `/course/delete/${id}`,
    method: 'delete'
  })
}
