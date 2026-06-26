import request from '../utils/request'

export function getRequiredCoursesByMajor(majorId) {
  return request({
    url: `/major-required-course/list/by-major/${majorId}`,
    method: 'get'
  })
}

export function bindRequiredCourse(data) {
  return request({
    url: '/major-required-course/bind',
    method: 'post',
    data
  })
}

export function unbindRequiredCourse(majorId, courseId) {
  return request({
    url: `/major-required-course/unbind/${majorId}/${courseId}`,
    method: 'delete'
  })
}
