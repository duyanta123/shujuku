import request from '../utils/request'

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getUserProfile(options = {}) {
  return request({
    url: '/user/profile',
    method: 'get',
    ...options,
  })
}

export function changePassword(data) {
  return request({
    url: '/user/change-password',
    method: 'put',
    data
  })
}
