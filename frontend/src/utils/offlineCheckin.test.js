import { describe, it, expect, beforeEach } from 'vitest'
import {
  enqueueCheckIn,
  getQueue,
  removeFromQueue,
  clearQueue,
  getQueueSize
} from './offlineCheckin.js'

const STORAGE_KEY = 'offline_checkin_queue'

beforeEach(() => {
  localStorage.removeItem(STORAGE_KEY)
})

describe('offlineCheckin', () => {
  it('stores only courseId and timestamp', () => {
    enqueueCheckIn(100)
    const queue = getQueue()
    expect(queue).toHaveLength(1)
    expect(queue[0]).toEqual({
      courseId: 100,
      timestamp: expect.any(Number),
    })
    expect(queue[0].studentId).toBeUndefined()
  })

  it('deduplicates by courseId', () => {
    enqueueCheckIn(100)
    enqueueCheckIn(100)
    expect(getQueue()).toHaveLength(1)
  })

  it('migrates old queue entries and drops invalid data', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([
      { studentId: 1, courseId: 100, timestamp: 1 },
      { studentId: 2, courseId: 100, timestamp: 2 },
      { studentId: 3, courseId: null },
      { foo: 'bar' },
    ]))

    expect(getQueue()).toEqual([{ courseId: 100, timestamp: 1 }])
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY))).toEqual([{ courseId: 100, timestamp: 1 }])
  })

  it('damaged or non-array storage returns an empty queue', () => {
    localStorage.setItem(STORAGE_KEY, '{invalid-json')
    expect(getQueue()).toEqual([])

    localStorage.setItem(STORAGE_KEY, '"not-an-array"')
    expect(getQueue()).toEqual([])
  })

  it('removes by courseId', () => {
    enqueueCheckIn(100)
    enqueueCheckIn(200)
    expect(removeFromQueue(100)).toEqual([
      { courseId: 200, timestamp: expect.any(Number) },
    ])
  })

  it('clearQueue and getQueueSize reflect current state', () => {
    enqueueCheckIn(100)
    enqueueCheckIn(200)
    expect(getQueueSize()).toBe(2)
    clearQueue()
    expect(getQueueSize()).toBe(0)
    expect(getQueue()).toEqual([])
  })
})
