const BASE_URL = 'http://localhost:3000';

async function resetBrowserSession(page) {
  await page.context().clearCookies();
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

async function selectRole(page, roleText) {
  const tab = page.locator('.role-tab').filter({ hasText: roleText });
  if (await tab.count() > 0 && await tab.first().isVisible()) {
    await tab.first().click();
    await page.waitForTimeout(300);
  }
}

async function submitLogin(page, username, password) {
  const inputs = page.locator('.el-input__inner');
  await inputs.first().fill(username);
  await inputs.nth(1).fill(password);
  await page.locator('.login-btn').first().click();
  await page.waitForFunction(() => !window.location.href.includes('/login'), { timeout: 10000 }).catch(() => {});
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);
}

async function adminLogin(page, username = 'admin', password = '123456') {
  await resetBrowserSession(page);
  await page.waitForSelector('input[type="text"], .el-input__inner', { timeout: 10000 });
  await selectRole(page, '管理员');
  await submitLogin(page, username, password);
}

async function studentLogin(page, username = 'S001', password = '123456') {
  await resetBrowserSession(page);
  await page.waitForSelector('.el-input__inner', { timeout: 10000 });
  await selectRole(page, '学生');
  await submitLogin(page, username, password);
}

async function teacherLogin(page, username = 'T001', password = '123456') {
  await resetBrowserSession(page);
  await page.waitForSelector('.el-input__inner', { timeout: 10000 });
  await selectRole(page, '教师');
  await submitLogin(page, username, password);
}

async function logout(page) {
  await page.context().clearCookies();
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
  await page.waitForLoadState('networkidle');
}

async function clearStorage(page) {
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

module.exports = { adminLogin, studentLogin, teacherLogin, logout, clearStorage, BASE_URL };
