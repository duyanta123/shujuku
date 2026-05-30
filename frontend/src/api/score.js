import request from '../utils/request'

export function addScore(data) {
  return request({
    url: '/score/add',
    method: 'post',
    data
  })
}
