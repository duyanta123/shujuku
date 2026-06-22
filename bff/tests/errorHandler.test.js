import { describe, it, expect, vi, beforeEach } from 'vitest'

// ================================================================
// errorHandler 中间件测试
//
// 风险行为覆盖：
// - 生产环境：隐藏内部错误详情，仅返回通用消息
// - 开发环境：返回完整的错误消息便于调试
// - 响应已发送时：不重复发送响应（防止 crash）
// - 状态码透传：使用 error.statusCode 或 reply.statusCode
// - 非500的生产环境错误：仍返回详细消息（预期的客户端错误）
// ================================================================

// 使用 import 原始模块然后直接修改 config 引用来模拟生产/开发环境
describe('errorHandler 中间件', () => {
  let mockReply
  let mockRequest
  let errorHandler

  beforeEach(async () => {
    // 每次测试前重新获取模块引用
    vi.resetModules()
  })

  function createMocks() {
    mockReply = {
      sent: false,
      statusCode: 500,
      code: vi.fn(function (statusCode) {
        this.statusCode = statusCode
        return this
      }),
      send: vi.fn(),
    }
    mockRequest = {
      method: 'POST',
      url: '/api/test',
      log: {
        error: vi.fn(),
      },
    }
  }

  // ================================================================
  // 开发环境 — 返回完整错误消息
  // ================================================================

  it('开发环境应返回完整错误消息', async () => {
    createMocks()

    // 在导入前设置环境变量使 NODE_ENV = development
    const originalNodeEnv = process.env.NODE_ENV
    process.env.NODE_ENV = 'development'

    vi.doMock('../src/config.js', () => ({
      config: {
        nodeEnv: 'development',
      },
    }))

    const { errorHandler: handler } = await import('../src/middleware/errorHandler.js')
    errorHandler = handler

    const error = new Error('数据库连接失败')

    errorHandler(error, mockRequest, mockReply)

    expect(mockReply.send).toHaveBeenCalledWith({
      success: false,
      message: '数据库连接失败',
    })

    process.env.NODE_ENV = originalNodeEnv
  })

  // ================================================================
  // 生产环境 — 隐藏内部错误详情
  // ================================================================

  it('生产环境 500 错误应返回通用错误消息', async () => {
    createMocks()

    vi.doMock('../src/config.js', () => ({
      config: {
        nodeEnv: 'production',
      },
    }))

    const { errorHandler: handler } = await import('../src/middleware/errorHandler.js')
    errorHandler = handler

    const error = new Error('数据库连接失败')

    errorHandler(error, mockRequest, mockReply)

    expect(mockReply.send).toHaveBeenCalledWith({
      success: false,
      message: '服务器内部错误',
    })
  })

  // ================================================================
  // 状态码 — 透传错误状态码
  // ================================================================

  it('应使用 error.statusCode 作为响应状态码', async () => {
    createMocks()

    vi.doMock('../src/config.js', () => ({
      config: { nodeEnv: 'development' },
    }))

    const { errorHandler: handler } = await import('../src/middleware/errorHandler.js')
    errorHandler = handler

    const error = new Error('未授权访问')
    error.statusCode = 401

    errorHandler(error, mockRequest, mockReply)

    expect(mockReply.code).toHaveBeenCalledWith(401)
  })

  it('无 error.statusCode 时应使用 reply.statusCode（默认 500）', async () => {
    createMocks()

    mockReply.statusCode = 503

    vi.doMock('../src/config.js', () => ({
      config: { nodeEnv: 'development' },
    }))

    const { errorHandler: handler } = await import('../src/middleware/errorHandler.js')
    errorHandler = handler

    const error = new Error('服务不可用')

    errorHandler(error, mockRequest, mockReply)

    expect(mockReply.code).toHaveBeenCalledWith(503)
  })

  // ================================================================
  // 响应已发送 — 不重复发送
  // ================================================================

  it('响应已发送时不应重复发送', async () => {
    createMocks()

    mockReply.sent = true

    vi.doMock('../src/config.js', () => ({
      config: { nodeEnv: 'development' },
    }))

    const { errorHandler: handler } = await import('../src/middleware/errorHandler.js')
    errorHandler = handler

    const error = new Error('某个错误')

    errorHandler(error, mockRequest, mockReply)

    expect(mockReply.send).not.toHaveBeenCalled()
  })

  // ================================================================
  // 日志 — 记录错误详情
  // ================================================================

  it('应通过 request.log.error 记录错误', async () => {
    createMocks()

    vi.doMock('../src/config.js', () => ({
      config: { nodeEnv: 'development' },
    }))

    const { errorHandler: handler } = await import('../src/middleware/errorHandler.js')
    errorHandler = handler

    const error = new Error('测试错误')

    errorHandler(error, mockRequest, mockReply)

    expect(mockRequest.log.error).toHaveBeenCalledWith(
      expect.objectContaining({
        err: error,
        method: 'POST',
        url: '/api/test',
      }),
      'Error: 测试错误'
    )
  })

  // ================================================================
  // 非 500 的生产环境错误 — 仍返回详细消息
  // ================================================================

  it('生产环境非 500 错误应返回详细消息', async () => {
    createMocks()

    vi.doMock('../src/config.js', () => ({
      config: { nodeEnv: 'production' },
    }))

    const { errorHandler: handler } = await import('../src/middleware/errorHandler.js')
    errorHandler = handler

    const error = new Error('请求参数无效')
    error.statusCode = 400

    errorHandler(error, mockRequest, mockReply)

    expect(mockReply.send).toHaveBeenCalledWith({
      success: false,
      message: '请求参数无效',
    })
  })
})