const { test, expect } = require('@playwright/test');
const { adminLogin, studentLogin, teacherLogin, BASE_URL } = require('../utils/auth');

test.describe('权限与路由测试', () => {

  test('admin登录后侧边栏包含学院专业管理菜单', async ({ page }) => {
    await adminLogin(page);
    // Verify the sidebar nav item exists
    const navItem = page.locator('.nav-item').filter({ hasText: '学院专业管理' });
    await expect(navItem).toBeVisible();
    // Click it and verify URL
    await navItem.click();
    await page.waitForURL('**/admin/college-major**');
    await expect(page).toHaveURL(/.*college-major.*/);
  });

  test('student登录后侧边栏无学院专业管理菜单', async ({ page }) => {
    await studentLogin(page);
    // Verify the nav item does NOT exist
    const navItem = page.locator('.nav-item').filter({ hasText: '学院专业管理' });
    await expect(navItem).not.toBeVisible();
  });

  test('student手动输入URL访问学院专业管理页面 — BFF模式页面可渲染但API返回403', async ({ page }) => {
    await studentLogin(page);
    // BFF模式下前端路由不拦截角色，信任后端API授权
    // 页面会渲染，但学院列表API应返回403
    const apiFailed = page.waitForResponse(
      resp => resp.url().includes('/api/college/list') && resp.status() === 403,
      { timeout: 10000 }
    ).then(() => true).catch(() => false);
    await page.goto(`${BASE_URL}/admin/college-major`);
    await page.waitForLoadState('networkidle');
    // BFF模式下页面可渲染（URL 保持在 college-major）
    await expect(page).toHaveURL(/.*college-major.*/);
    // 验证后端 API 返回 403（权限不足）
    const has403 = await apiFailed;
    expect(has403).toBe(true);
  });

  test('teacher登录后侧边栏无学院专业管理菜单', async ({ page }) => {
    await teacherLogin(page);
    const navItem = page.locator('.nav-item').filter({ hasText: '学院专业管理' });
    await expect(navItem).not.toBeVisible();
  });

  test('teacher手动输入URL访问学院专业管理页面 — BFF模式页面可渲染但API返回403', async ({ page }) => {
    await teacherLogin(page);
    // BFF模式下前端路由不拦截角色，信任后端API授权
    const apiFailed = page.waitForResponse(
      resp => resp.url().includes('/api/college/list') && resp.status() === 403,
      { timeout: 10000 }
    ).then(() => true).catch(() => false);
    await page.goto(`${BASE_URL}/admin/college-major`);
    await page.waitForLoadState('networkidle');
    // BFF模式下页面可渲染（URL 保持在 college-major）
    await expect(page).toHaveURL(/.*college-major.*/);
    // 验证后端 API 返回 403（权限不足）
    const has403 = await apiFailed;
    expect(has403).toBe(true);
  });

  test('未登录用户访问管理页面自动跳转登录页', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/college-major`);
    await page.waitForLoadState('networkidle');
    // Should redirect to login
    await expect(page).toHaveURL(/.*login.*/);
  });

  test.afterEach(async ({ page }, testInfo) => {
    if (testInfo.status !== 'passed') {
      const { captureFailure } = require('../utils/screenshot');
      await captureFailure(page, testInfo.title);
    }
  });
});