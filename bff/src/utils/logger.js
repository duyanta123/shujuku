const MODULE_PREFIX = 'BFF'

function timestamp() {
  return new Date().toISOString()
}

function safeStringify(value) {
  const seen = new WeakSet()
  return JSON.stringify(value, (key, currentValue) => {
    if (typeof currentValue === 'object' && currentValue !== null) {
      if (seen.has(currentValue)) return '[Circular]'
      seen.add(currentValue)
    }
    return currentValue
  })
}

const DEFAULT_SENSITIVE_KEYS = [
  'password',
  'oldPassword',
  'newPassword',
  'token',
  'accessToken',
  'refreshToken',
  'authorization',
  'secret',
  'query',
]
const DEFAULT_SENSITIVE_KEY_SET = new Set(DEFAULT_SENSITIVE_KEYS.map(key => key.toLowerCase()))

function sanitizeUrl(value) {
  if (typeof value !== 'string' || !value.includes('?')) return value

  const [base, query = ''] = value.split('?')
  const redacted = query.split('&').filter(Boolean).map((part) => {
    const [key] = part.split('=')
    if (DEFAULT_SENSITIVE_KEY_SET.has(decodeURIComponent(key || '').toLowerCase())) {
      return `${key}=***`
    }
    return part
  }).join('&')
  return redacted ? `${base}?${redacted}` : base
}

function format(module, level, message, data) {
  const ts = timestamp()
  const prefix = `[${ts}] [${MODULE_PREFIX}:${module}] [${level}]`

  if (data) {
    const safeData = maskSensitive(data)
    const dataStr = Object.entries(safeData)
      .map(([k, v]) => {
        if (typeof v === 'object') {
          return `${k}=${safeStringify(v)}`
        }
        return `${k}=${sanitizeUrl(v)}`
      })
      .join(' | ')
    return `${prefix} ${message} | ${dataStr}`
  }

  return `${prefix} ${message}`
}

export function createLogger(module) {
  return {
    info(message, data) {
      console.log(format(module, 'INFO', message, data))
    },
    warn(message, data) {
      console.warn(format(module, 'WARN', message, data))
    },
    error(message, data) {
      console.error(format(module, 'ERROR', message, data))
    },
    debug(message, data) {
      if (process.env.LOG_LEVEL === 'debug') {
        console.debug(format(module, 'DEBUG', message, data))
      }
    },
  }
}

export function maskSensitive(obj, keys = [
  ...DEFAULT_SENSITIVE_KEYS,
]) {
  if (!obj || typeof obj !== 'object') return obj

  const sensitiveKeys = new Set(keys.map(key => key.toLowerCase()))
  const seen = new WeakMap()

  function mask(value, key) {
    if (key && sensitiveKeys.has(key.toLowerCase())) {
      return value === undefined || value === null || value === '' ? value : '***'
    }

    if (!value || typeof value !== 'object') return value

    if (seen.has(value)) return seen.get(value)

    if (Array.isArray(value)) {
      const result = []
      seen.set(value, result)
      for (const item of value) {
        result.push(mask(item))
      }
      return result
    }

    const result = {}
    seen.set(value, result)
    for (const [childKey, childValue] of Object.entries(value)) {
      result[childKey] = mask(childValue, childKey)
    }
    return result
  }

  return mask(obj)
}

export default createLogger
