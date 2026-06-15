/**
 * 截图工具封装
 * 测试失败时自动截图、保存控制台日志与网络请求日志
 */

const path = require('path');
const fs = require('fs');

const SCREENSHOT_DIR = path.resolve(__dirname, '..', 'screenshots');

/**
 * 确保截图目录存在
 */
function ensureScreenshotDir() {
  if (!fs.existsSync(SCREENSHOT_DIR)) {
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  }
}

/**
 * 保存失败截图
 * @param {import('@playwright/test').Page} page
 * @param {string} testName 测试用例名称
 * @returns {Promise<string>} 截图文件路径
 */
async function captureFailure(page, testName) {
  ensureScreenshotDir();
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const safeName = testName.replace(/[^a-zA-Z0-9\u4e00-\u9fa5_-]/g, '_');
  const filePath = path.join(SCREENSHOT_DIR, `FAIL_${safeName}_${timestamp}.png`);
  await page.screenshot({ path: filePath, fullPage: true });
  return filePath;
}

/**
 * 收集控制台日志
 * @param {import('@playwright/test').Page} page
 * @returns {string[]} 日志数组
 */
function collectConsoleLogs(page) {
  const logs = [];
  page.on('console', (msg) => {
    logs.push(`[${msg.type()}] ${msg.text()}`);
  });
  return logs;
}

/**
 * 保存测试日志到文件
 * @param {string} testName 测试名称
 * @param {string[]} logs 日志内容
 * @param {string} [reason] 失败原因
 */
function saveTestLog(testName, logs, reason = '') {
  ensureScreenshotDir();
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const safeName = testName.replace(/[^a-zA-Z0-9\u4e00-\u9fa5_-]/g, '_');
  const filePath = path.join(SCREENSHOT_DIR, `LOG_${safeName}_${timestamp}.log`);
  let content = `Test: ${testName}\nTime: ${new Date().toISOString()}\n`;
  if (reason) content += `Reason: ${reason}\n`;
  content += `\nConsole Logs:\n${logs.join('\n')}\n`;
  fs.writeFileSync(filePath, content, 'utf-8');
  return filePath;
}

module.exports = { captureFailure, collectConsoleLogs, saveTestLog, SCREENSHOT_DIR };