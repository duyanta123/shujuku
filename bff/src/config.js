import dotenv from 'dotenv'
dotenv.config()

const env = process.env
const nodeEnv = env.NODE_ENV || 'development'

const weakJwtSecrets = new Set([
  'lab-course-system-secret-key-2024',
  'your-secret-key-here',
])

function parseInteger(value, fallback, name) {
  const parsed = parseInt(value || fallback, 10)
  if (!Number.isFinite(parsed)) {
    throw new Error(`${name} must be a valid integer`)
  }
  return parsed
}

function validateJwtSecret(secret) {
  if (!secret) {
    throw new Error('JWT_SECRET must be configured')
  }
  if (secret.length < 32) {
    throw new Error('JWT_SECRET must be at least 32 characters')
  }
  if (weakJwtSecrets.has(secret)) {
    throw new Error('JWT_SECRET must not use a default or example value')
  }
  return secret
}

const jwtSecret = nodeEnv === 'test' && weakJwtSecrets.has(env.JWT_SECRET)
  ? 'test-jwt-secret-for-bff-tests-at-least-32-bytes'
  : env.JWT_SECRET

export const config = {
  port: parseInteger(env.BFF_PORT, '4000', 'BFF_PORT'),
  backendUrl: env.BACKEND_URL || 'http://localhost:8080',
  backendTimeoutMs: parseInteger(env.BACKEND_TIMEOUT_MS, '10000', 'BACKEND_TIMEOUT_MS'),
  nodeEnv,
  rateLimit: {
    max: parseInteger(env.RATE_LIMIT_MAX, nodeEnv === 'production' ? '100' : '1000', 'RATE_LIMIT_MAX'),
    timeWindow: env.RATE_LIMIT_WINDOW || '1 minute',
  },

  jwt: {
    secret: validateJwtSecret(jwtSecret),
    expiration: parseInteger(env.JWT_EXPIRATION, '86400000', 'JWT_EXPIRATION'),
    accessTokenCookieName: 'bff_access_token',
    accessTokenMaxAge: 30 * 60 * 1000,
    refreshTokenCookieName: 'bff_refresh_token',
    refreshTokenMaxAge: 7 * 24 * 60 * 60 * 1000,
    cookieName: 'bff_token',
    cookieMaxAge: 24 * 60 * 60 * 1000,
  },

  log: {
    level: env.LOG_LEVEL || 'info',
    pretty: env.NODE_ENV !== 'production',
  },
}
