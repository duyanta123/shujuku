const { test, expect } = require('@playwright/test');
const { adminLogin } = require('../utils/auth');

const COLLEGE_MAJOR_URL = 'http://localhost:3000/admin/college-major';

// 此测试必须最后执行，因为会删除数据影响其他测试
test.describe('学院删除有关联数据校验（最后执行）', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto(COLLEGE_MAJOR_URL);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);
  });

  test.skip('删除有关联学院：删除失败并提示', async ({ page }) => {
    const rows = page.locator('.el-table__body tbody tr');
    const rowCount = await rows.count();
    if (rowCount === 0) {
      test.skip(true, '无数据');
      return;
    }
    let foundError = false;
    for (let i = 0; i < Math.min(rowCount, 5); i++) {
      const row = rows.nth(i);
      const deleteBtn = row.locator('button').filter({ hasText: /删除/ });
      if (await deleteBtn.count() === 0) continue;
      
      await deleteBtn.first().click();
      await page.waitForTimeout(500);

      // Confirm deletion
      const msgBox = page.locator('.el-message-box__btns .el-button--primary').first();
      if (await msgBox.isVisible({ timeout: 2000 }).catch(() => false)) {
        await msgBox.click();
      } else {
        const popconfirm = page.locator('.el-popconfirm');
        const confirmBtn = popconfirm.locator('.el-button--primary').first();
        if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
          await confirmBtn.click();
        } else {
          continue;
        }
      }
      await page.waitForTimeout(2000);
      
      const errorMsg = page.locator('.el-message--error').first();
      if (await errorMsg.isVisible({ timeout: 3000 }).catch(() => false)) {
        foundError = true;
        break;
      }
      await page.waitForTimeout(500);
    }
    expect(foundError).toBe(true);
  });
});