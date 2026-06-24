const STORAGE_KEY = 'offline_checkin_queue'

function normalizeItem(item) {
  const courseId = Number(item?.courseId)
  if (!Number.isSafeInteger(courseId) || courseId <= 0) return null
  return {
    courseId,
    timestamp: Number(item?.timestamp) > 0 ? Number(item.timestamp) : Date.now(),
  }
}

function normalizeQueue(value) {
  if (!Array.isArray(value)) return []
  const seen = new Set()
  const queue = []
  for (const item of value) {
    const normalized = normalizeItem(item)
    if (!normalized || seen.has(normalized.courseId)) continue
    seen.add(normalized.courseId)
    queue.push(normalized)
  }
  return queue
}

function loadQueue() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    const queue = normalizeQueue(parsed)
    if (JSON.stringify(parsed) !== JSON.stringify(queue)) {
      saveQueue(queue)
    }
    return queue
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return []
  }
}

function saveQueue(queue) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizeQueue(queue)))
}

export function enqueueCheckIn(courseId) {
  const normalized = normalizeItem({ courseId, timestamp: Date.now() })
  if (!normalized) return
  const queue = loadQueue()
  if (!queue.some(item => item.courseId === normalized.courseId)) {
    queue.push(normalized)
    saveQueue(queue)
  }
}

export function getQueue() {
  return loadQueue()
}

export function removeFromQueue(courseId) {
  const id = Number(courseId)
  const queue = loadQueue().filter(item => item.courseId !== id)
  saveQueue(queue)
  return queue
}

export function clearQueue() {
  saveQueue([])
}

export function getQueueSize() {
  return loadQueue().length
}
