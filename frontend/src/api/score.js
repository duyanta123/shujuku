import request from '../utils/request'

export function addScore(data) {
  return request({
    url: '/score/add',
    method: 'post',
    data
  })
}

export function getScoresByCourse(courseId) {
  return request({
    url: `/score/course/${courseId}`,
    method: 'get'
  })
}
