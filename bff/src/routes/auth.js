import { config } from '../config.js'
import { backendClient } from '../services/backendClient.js'
import { createLogger } from '../utils/logger.js'

const log = createLogger('AUTH')

function getBearerToken(request) {
  const authHeader = request.headers.authorization
  if (authHeader?.startsWith('Bearer ')) {
    return authHeader.slice(7)
  }
  return null
}

export async function setupAuthRoutes(app) {
  const createLoginHandler = (targetPath, usernameField = 'username', roleName = 'unknown') => {
    return async (request, reply) => {
      const body = request.body && typeof request.body === 'object' && !Array.isArray(request.body)
        ? request.body
        : {}
      const username = body[usernameField]
      const { password } = body

      log.info('Login request', {
        requestId: request.id,
        path: targetPath,
        usernameField,
        username: username || '(empty)',
        ip: request.ip,
      })

      if (!username || !password) {
        log.warn('Login rejected: missing credentials', {
          requestId: request.id,
          path: targetPath,
          usernameField,
          hasUsername: !!username,
          hasPassword: !!password,
        })
        reply.code(400)
        return { success: false, message: '请提供用户名和密码' }
      }

      try {
        const response = await backendClient.post(targetPath, body)

        if (response.success || response.data) {
          const userData = response.data || {}
          const {
            accessToken: _ignoredAccessToken,
            refreshToken: _ignoredRefreshToken,
            password: _ignoredPassword,
            ...safeUserData
          } = userData
          const userId = userData.id || userData.studentId || userData.teacherId || username || 'unknown'
          const displayName = userData.name || username || 'unknown'
          const accessToken = response.accessToken || userData.accessToken
          const refreshToken = response.refreshToken || userData.refreshToken

          if (!accessToken || !refreshToken) {
            log.error('Login failed: backend did not return tokens', {
              requestId: request.id,
              path: targetPath,
              hasAccessToken: !!accessToken,
              hasRefreshToken: !!refreshToken,
            })
            reply.code(502)
            return { success: false, message: 'Invalid backend authentication response' }
          }

          const isSecure = config.nodeEnv === 'production'
          reply.setCookie(config.jwt.accessTokenCookieName, accessToken, {
            httpOnly: true,
            secure: isSecure,
            sameSite: 'lax',
            path: '/',
            maxAge: config.jwt.accessTokenMaxAge / 1000,
          })
          reply.setCookie(config.jwt.refreshTokenCookieName, refreshToken, {
            httpOnly: true,
            secure: isSecure,
            sameSite: 'lax',
            path: '/api/auth',
            maxAge: config.jwt.refreshTokenMaxAge / 1000,
          })

          log.info('Login succeeded', {
            requestId: request.id,
            path: targetPath,
            username: username || '(unknown)',
            userId,
            role: roleName,
            displayName,
          })

          return {
            success: true,
            message: response.message || response.msg || 'Login succeeded',
            data: {
              ...safeUserData,
              userId,
              username: username || userData.username || userData.studentNo || userData.teacherNo || 'unknown',
              role: roleName,
              tokenExpireTime: Date.now() + config.jwt.accessTokenMaxAge,
            },
          }
        }

        log.warn('Login failed', {
          requestId: request.id,
          path: targetPath,
          username: username || '(unknown)',
          reason: response.message || 'Authentication failed',
          ip: request.ip,
        })

        reply.code(401)
        return response
      } catch (err) {
        log.error('Login request failed', {
          requestId: request.id,
          path: targetPath,
          error: err.message,
          ip: request.ip,
        })
        reply.code(err.name === 'AbortError' ? 504 : 502)
        return { success: false, message: '后端服务不可达，请稍后重试' }
      }
    }
  }

  app.post('/api/student/login', createLoginHandler('/api/student/login', 'studentNo', 'student'))
  app.post('/api/teacher/login', createLoginHandler('/api/teacher/login', 'teacherNo', 'teacher'))
  app.post('/api/admin/login', createLoginHandler('/api/admin/login', 'username', 'admin'))

  app.post('/api/auth/refresh', async (request, reply) => {
    const jwt = await import('jsonwebtoken')

    const refreshToken = request.cookies?.[config.jwt.refreshTokenCookieName]

    if (!refreshToken) {
      log.warn('Refresh rejected: missing refresh token', { requestId: request.id })
      reply.code(401)
      return { success: false, message: 'RefreshToken is missing, please log in again' }
    }

    try {
      const decoded = jwt.verify(refreshToken, config.jwt.secret, {
        algorithms: ['HS256', 'HS384', 'HS512'],
      })

      log.info('Refresh request locally verified', {
        requestId: request.id,
        userId: decoded.userId || decoded.sub,
        tokenExp: decoded.exp ? new Date(decoded.exp * 1000).toISOString() : 'unknown',
      })
    } catch (err) {
      if (err.name === 'TokenExpiredError') {
        log.warn('Refresh rejected: token expired', {
          requestId: request.id,
          expiredAt: err.expiredAt,
        })
        reply.clearCookie(config.jwt.accessTokenCookieName, { path: '/' })
        reply.clearCookie(config.jwt.refreshTokenCookieName, { path: '/api/auth' })
        reply.clearCookie(config.jwt.cookieName, { path: '/' })
        reply.code(401)
        return { success: false, message: 'Token 已过期，请重新登录' }
      }

      log.warn('Refresh rejected: invalid token', {
        requestId: request.id,
        error: err.name,
        message: err.message,
      })
      reply.clearCookie(config.jwt.accessTokenCookieName, { path: '/' })
      reply.clearCookie(config.jwt.refreshTokenCookieName, { path: '/api/auth' })
      reply.clearCookie(config.jwt.cookieName, { path: '/' })
      reply.code(401)
      return { success: false, message: 'Invalid token, please log in again' }
    }

    try {
      const response = await backendClient.post('/api/auth/refresh', { refreshToken })

      if (response.success && response.accessToken && response.refreshToken) {
        const isSecure = config.nodeEnv === 'production'

        reply.setCookie(config.jwt.accessTokenCookieName, response.accessToken, {
          httpOnly: true,
          secure: isSecure,
          sameSite: 'lax',
          path: '/',
          maxAge: config.jwt.accessTokenMaxAge / 1000,
        })

        reply.setCookie(config.jwt.refreshTokenCookieName, response.refreshToken, {
          httpOnly: true,
          secure: isSecure,
          sameSite: 'lax',
          path: '/api/auth',
          maxAge: config.jwt.refreshTokenMaxAge / 1000,
        })

        log.info('Refresh succeeded', { requestId: request.id })

        return {
          success: true,
          message: 'Token refreshed',
          expiresIn: Math.floor(config.jwt.accessTokenMaxAge / 1000),
        }
      }

      log.warn('Refresh rejected by backend', {
        requestId: request.id,
        reason: response.message || 'unknown',
      })
      reply.code(401)
      return { success: false, message: response.message || 'Token refresh failed, please log in again' }
    } catch (err) {
      log.error('Refresh request failed', {
        requestId: request.id,
        error: err.message,
      })
      reply.code(err.name === 'AbortError' ? 504 : 502)
      return { success: false, message: 'Token refresh service unavailable' }
    }
  })

  app.post('/api/auth/logout', async (request, reply) => {
    log.info('Logout request', { requestId: request.id })

    const accessToken = request.cookies?.[config.jwt.accessTokenCookieName]
      || request.cookies?.[config.jwt.cookieName]
      || getBearerToken(request)

    reply.clearCookie(config.jwt.accessTokenCookieName, { path: '/' })
    reply.clearCookie(config.jwt.refreshTokenCookieName, { path: '/api/auth' })
    reply.clearCookie(config.jwt.cookieName, { path: '/' })

    try {
      const headers = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
      await backendClient.post('/api/auth/logout', {}, { headers })
    } catch (e) {
      log.warn('Backend logout notification failed', { requestId: request.id, error: e.message })
    }

    return { success: true, message: 'Logged out' }
  })
}
