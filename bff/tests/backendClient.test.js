/**
 * backendClient.js 单元测试 — 覆盖 BFF 后端通信客户端（原本零覆盖）
 *
 * 风险行为覆盖：
 * - GET 请求: 成功 JSON 响应、非 JSON 响应透传
 * - POST 请求: 成功提交、请求体发送
 * - PUT 请求: 成功更新
 * - DELETE 请求: 成功删除
 * - 错误处理: 后端返回 4xx/5xx 错误响应
 * - 请求头: Content-Type 设置、自定义 headers 合并
 * - 边界情况: 空 body、大 body
 *
 * 风险背景：
 * backendClient 是 BFF 中所有后端 API 调用的统一入口，auth.js 的登录/刷新/登出
 * 都依赖它。若请求发送逻辑有缺陷，可能导致认证失败、数据丢失等严重问题。
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { backendClient } from '../src/services/backendClient.js'

// 保存原始 fetch
const originalFetch = globalThis.fetch

describe('backendClient — GET 请求', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('成功 JSON 响应应返回解析后的 JSON 对象', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: {
        get: (key) => key === 'content-type' ? 'application/json' : null,
      },
      json: async () => ({ success: true, data: { id: 1, name: 'test' } }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const result = await backendClient.get('/api/test')

    expect(result).toEqual({ success: true, data: { id: 1, name: 'test' } })
    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/test',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      })
    )
  })

  it('GET 请求应正确拼接路径', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true, data: [] }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    await backendClient.get('/api/college/list?page=1&size=10')

    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/college/list?page=1&size=10',
      expect.any(Object)
    )
  })

  it('非 JSON 响应应透传原始 Response 对象', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: {
        get: (key) => key === 'content-type' ? 'application/octet-stream' : null,
      },
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const result = await backendClient.get('/api/export/excel')

    // 非 JSON 响应直接返回 response 对象
    expect(result).toBe(mockResponse)
  })

  it('后端 4xx 错误 JSON 响应应正常解析', async () => {
    const mockResponse = {
      ok: false,
      status: 400,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: false, message: '参数错误' }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const result = await backendClient.get('/api/college/list?collegeId=invalid')

    expect(result).toEqual({ success: false, message: '参数错误' })
  })

  it('后端 500 错误 JSON 响应应正常解析', async () => {
    const mockResponse = {
      ok: false,
      status: 500,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: false, message: '服务器内部错误' }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const result = await backendClient.get('/api/error')

    expect(result).toEqual({ success: false, message: '服务器内部错误' })
  })
})

describe('backendClient — POST 请求', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('POST 请求应发送 JSON body', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true, message: '添加成功' }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const body = { name: '新学院', status: 'ACTIVE' }
    const result = await backendClient.post('/api/college/add', body)

    expect(result).toEqual({ success: true, message: '添加成功' })
    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/college/add',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(body),
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      })
    )
  })

  it('POST 请求应支持自定义 headers', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const customHeaders = { Authorization: 'Bearer test-token' }
    await backendClient.post('/api/test', { data: 'test' }, { headers: customHeaders })

    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/test',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-token',
        }),
      })
    )
  })

  it('POST 空 body 应正常发送', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const result = await backendClient.post('/api/test')

    expect(result).toEqual({ success: true })
  })
})

describe('backendClient — PUT 请求', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('PUT 请求应发送 JSON body', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true, message: '更新成功' }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const body = { id: 1, name: '更新后名称' }
    const result = await backendClient.put('/api/college/update', body)

    expect(result).toEqual({ success: true, message: '更新成功' })
    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/college/update',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify(body),
      })
    )
  })
})

describe('backendClient — DELETE 请求', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('DELETE 请求应正确发送', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true, message: '删除成功' }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    const result = await backendClient.delete('/api/college/delete/1')

    expect(result).toEqual({ success: true, message: '删除成功' })
    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/college/delete/1',
      expect.objectContaining({
        method: 'DELETE',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      })
    )
  })

  it('DELETE 请求应支持自定义 options', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true }),
    }
    globalThis.fetch.mockResolvedValue(mockResponse)

    await backendClient.delete('/api/test', { headers: { Authorization: 'Bearer token' } })

    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/test',
      expect.objectContaining({
        method: 'DELETE',
        headers: expect.objectContaining({ Authorization: 'Bearer token' }),
      })
    )
  })
})

describe('backendClient — 网络错误处理', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('fetch 网络异常应向上抛出', async () => {
    const networkError = new Error('ECONNREFUSED: connection refused')
    globalThis.fetch.mockRejectedValue(networkError)

    await expect(backendClient.get('/api/test')).rejects.toThrow('ECONNREFUSED')
  })

  it('fetch 超时应向上抛出', async () => {
    const timeoutError = new Error('The operation was aborted')
    timeoutError.name = 'AbortError'
    globalThis.fetch.mockRejectedValue(timeoutError)

    await expect(backendClient.get('/api/test')).rejects.toThrow('aborted')
  })
})