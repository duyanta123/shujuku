const { defineConfig } = require('@playwright/test');

/**
 * Playwright E2E 测试配置
 * 测试本地运行的前端应用（端口 3000），通过 BFF 层（端口 4000）代理后端（端口 8080）
 */
module.exports = defineConfig({
  testDir: './tests/e2e',
  /* 单个测试超时时间 */
  timeout: 60000,
  /* expect 断言超时时间 */
  expect: {
    timeout: 10000,
  },
  /* 失败重试次数 */
  retries: 1,
  /* 并行执行 worker 数 */
  workers: process.env.CI ? 2 : 1,
  /* Reporter 配置 */
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
  ],
  /* 共享配置 */
  use: {
    /* 基础 URL */
    baseURL: 'http://localhost:3000',
    /* 失败时截图 */
    screenshot: 'only-on-failure',
    /* 失败时录制视频 */
    video: 'retain-on-failure',
    /* 操作超时 */
    actionTimeout: 15000,
    /* 导航超时 */
    navigationTimeout: 20000,
    /* 追踪 */
    trace: 'retain-on-failure',
    /* 忽略 HTTPS 错误 */
    ignoreHTTPSErrors: true,
  },

  /* 针对不同浏览器的配置 */
  projects: [
    {
      name: 'chromium',
      use: {
        ...require('@playwright/test').devices['Desktop Chrome'],
        /* 设置视口大小匹配常见桌面分辨率 */
        viewport: { width: 1440, height: 900 },
      },
    },
    // 如需 Firefox 测试，取消下面的注释
    // {
    //   name: 'firefox',
    //   use: { ...require('@playwright/test').devices['Desktop Firefox'] },
    // },
  ],

  /* 本地开发服务器配置（可选：如果不想手动启动服务） */
  // webServer: {
  //   command: 'npm run dev',
  //   url: 'http://localhost:3000',
  //   reuseExistingServer: true,
  //   timeout: 120000,
  // },
});