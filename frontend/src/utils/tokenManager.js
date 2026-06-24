import { readJsonStorage, writeJsonStorage } from './safeStorage'

class TokenManager {
  getToken() {
    return null
  }

  getTokenExpireTime() {
    return 0
  }

  setToken() {
    // Tokens are held only in HttpOnly cookies by the BFF.
  }

  setUser(userInfo) {
    const safeUser = {
      name: userInfo?.name || '',
      account: userInfo?.account || '',
      avatarUrl: userInfo?.avatarUrl || '',
      college: userInfo?.college || '',
      title: userInfo?.title || '',
    }
    writeJsonStorage('user', safeUser)
  }

  getUser() {
    return readJsonStorage('user', {})
  }

  isTokenExpired() {
    return false
  }

  isTokenAboutToExpire() {
    return false
  }

  async refreshTokenIfNeeded() {
    return null
  }

  clearToken() {
    localStorage.removeItem('user')
  }
}

const tokenManager = new TokenManager()

export default tokenManager
