export function readJsonStorage(key, fallback = {}) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return fallback
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed
    }
  } catch {
    localStorage.removeItem(key)
  }
  return fallback
}

export function writeJsonStorage(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}
