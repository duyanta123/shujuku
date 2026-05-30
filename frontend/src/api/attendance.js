import request from '../utils/request'

export function addAttendance(data) {
  return request({
    url: '/attendance/add',
    method: 'post',
    data
  })
}
