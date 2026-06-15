/**
 * 公共登录方法封装
 * 支持管理员 / 学生 / 教师三种角色的登录与登出
 */
const BASE_URL = 'http://localhost:3000';

/**
 * 选择登录角色
 * @param {import('@playwright/test').Page} page
 * @param {string} roleText - 角色标签文本（如 "学生"、"教师"、"管理员"）
 */
async function selectRole(page, roleText) {
  const tab = page.locator('.role-tab').filter({ hasText: roleText });
  if (await tab.count() > 0 && await tab.first().isVisible()) {
    await tab.first().click();
    await page.waitForTimeout(300);
  }
}

/**
 * 管理员登录
 * @param {import('@playwright/test').Page} page
 * @param {string} [username='admin'] 用户名
 * @param {string} [password='123456'] 密码
 */
async function adminLogin(page, username = 'admin', password = '123456') {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForSelector('input[type="text"], .el-input__inner', { timeout: 10000 });
  // 选择管理员角色
  await selectRole(page, '管理员');
  // Element Plus el-input 渲染后，实际 input 在 .el-input__inner 内
  const inputs = page.locator('.el-input__inner');
  await inputs.first().fill(username);
  await inputs.nth(1).fill(password);
  await page.locator('.login-btn').first().click();
  // 等待离开登录页
  await page.waitForFunction(() => !window.location.href.includes('/login'), { timeout: 10000 }).catch(() => {});
  await page.waitForLoadState('networkidle');
  // 如果还在登录页，再等一会儿
  await page.waitForTimeout(1000);
}

/**
 * 学生登录
 * @param {import('@playwright/test').Page} page
 * @param {string} [username='S001'] 学号
 * @param {string} [password='123456'] 密码
 */
async function studentLogin(page, username = 'S001', password = '123456') {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForSelector('.el-input__inner', { timeout: 10000 });
  // 选择学生角色（虽然默认就是学生，但明确选择确保兼容）
  await selectRole(page, '学生');
  const inputs = page.locator('.el-input__inner');
  await inputs.first().fill(username);
  await inputs.nth(1).fill(password);
  await page.locator('.login-btn').first().click();
  // 等待离开登录页
  await page.waitForFunction(() => !window.location.href.includes('/login'), { timeout: 10000 }).catch(() => {});
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);
}

/**
 * 教师登录
 * @param {import('@playwright/test').Page} page
 * @param {string} [username='T001'] 工号
 * @param {string} [password='123456'] 密码
 */
async function teacherLogin(page, username = 'T001', password = '123456') {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForSelector('.el-input__inner', { timeout: 10000 });
  // 选择教师角色
  await selectRole(page, '教师');
  const inputs = page.locator('.el-input__inner');
  await inputs.first().fill(username);
  await inputs.nth(1).fill(password);
  await page.locator('.login-btn').first().click();
  // 等待离开登录页
  await page.waitForFunction(() => !window.location.href.includes('/login'), { timeout: 10000 }).catch(() => {});
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);
}

/**
 * 登出：清除 localStorage 并跳转到登录页
 * @param {import('@playwright/test').Page} page
 */
async function logout(page) {
  await page.evaluate(() => localStorage.clear());
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');
}

/**
 * 清除 localStorage（不跳转页面）
 * @param {import('@playwright/test').Page} page
 */
async function clearStorage(page) {
  await page.evaluate(() => localStorage.clear());
}

module.exports = { adminLogin, studentLogin, teacherLogin, logout, clearStorage, BASE_URL };