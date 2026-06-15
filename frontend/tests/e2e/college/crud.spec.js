const { test, expect } = require('@playwright/test');
const { adminLogin } = require('../utils/auth');
const { SELECTORS } = require('../utils/selectors');

const COLLEGE_MAJOR_URL = 'http://localhost:3000/admin/college-major';

test.describe('学院管理Tab功能测试', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto(COLLEGE_MAJOR_URL);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);
  });

  test('学院列表加载正常', async ({ page }) => {
    await page.waitForSelector(SELECTORS.TABLE, { timeout: 5000 });
    await expect(page.locator('th').filter({ hasText: '学院名称' }).first()).toBeVisible({ timeout: 5000 });
    await expect(page.locator('th').filter({ hasText: '状态' }).first()).toBeVisible({ timeout: 5000 });
    const rows = page.locator('.el-table__body tbody tr');
    await expect(rows.first()).toBeVisible({ timeout: 5000 });
  });

  test('新增学院：合法名称提交成功', async ({ page }) => {
    const uniqueName = `自动化学院_${Date.now()}`;
    await page.locator('button').filter({ hasText: '添加学院' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().fill(uniqueName);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    // Search for the new item (it's on a later page due to 38+ items)
    const searchInput = page.locator('input[placeholder*="搜索学院"]').first();
    await searchInput.fill(uniqueName);
    // Click search button (Enter key may not trigger @keyup.enter on el-input)
    await page.locator('button').filter({ hasText: '搜索' }).first().click();
    await page.waitForTimeout(2000);
    await page.waitForLoadState('networkidle');
    
    await expect(page.locator('.el-table__body tbody').first()).toContainText(uniqueName, { timeout: 5000 });
  });

  test('新增学院：名称为空触发表单校验', async ({ page }) => {
    await page.locator('button').filter({ hasText: '添加学院' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    const dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().clear();
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForTimeout(1000);
    const error = dialog.locator('.el-form-item__error').first();
    await expect(error).toBeVisible({ timeout: 3000 });
    // Close the dialog to avoid blocking subsequent tests
    await dialog.locator('.el-dialog__footer .el-button').first().click();
    await page.waitForTimeout(500);
  });

  test('新增学院：重复名称触发后端错误提示', async ({ page }) => {
    // Get name of first cell (college name) to use as duplicate
    const firstCell = page.locator('.el-table__body tbody tr').first().locator('td').first();
    const existingName = await firstCell.textContent().catch(() => '');
    if (!existingName || existingName.includes('暂无数据')) {
      test.skip(true, '无数据');
      return;
    }
    
    await page.locator('button').filter({ hasText: '添加学院' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    const dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().fill(existingName);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForTimeout(2000);
    // Check for any error message
    const msg = page.locator('.el-message--error').first();
    const warning = page.locator('.el-message--warning').first();
    const hasError = await msg.isVisible({ timeout: 3000 }).catch(() => false);
    const hasWarning = await warning.isVisible({ timeout: 3000 }).catch(() => false);
    expect(hasError || hasWarning).toBe(true);
  });

  test('编辑学院：修改名称提交成功', async ({ page }) => {
    const newName = `编辑后学院_${Date.now()}`;
    const editBtn = page.locator('.el-table__body tbody tr').first().locator('button').filter({ hasText: /编辑/ });
    await editBtn.first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    
    const dialog = page.locator(SELECTORS.DIALOG).last();
    const oldName = await dialog.locator(SELECTORS.INPUT).first().inputValue().catch(() => '');
    await dialog.locator(SELECTORS.INPUT).first().clear();
    await dialog.locator(SELECTORS.INPUT).first().fill(newName);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    // Search for the edited name
    const searchInput = page.locator('input[placeholder*="搜索学院"]').first();
    await searchInput.fill(newName);
    await page.locator('button').filter({ hasText: '搜索' }).first().click();
    await page.waitForTimeout(2000);
    await page.waitForLoadState('networkidle');
    
    await expect(page.locator('.el-table__body tbody').first()).toContainText(newName, { timeout: 5000 });
  });

  test('学院列表搜索', async ({ page }) => {
    const searchInput = page.locator('input[placeholder*="搜索学院"]').first();
    const firstRow = page.locator('.el-table__body tbody tr').first();
    const firstRowText = await firstRow.textContent().catch(() => '');
    if (!firstRowText || firstRowText.includes('暂无数据')) {
      test.skip(true, '无数据');
      return;
    }
    const keyword = firstRowText.trim().substring(0, 2);
    await searchInput.fill(keyword);
    await page.locator('button').filter({ hasText: '搜索' }).first().click();
    await page.waitForTimeout(2000);
    await page.waitForLoadState('networkidle');
    const rows = page.locator('.el-table__body tbody tr');
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThan(0);
  });

  test('删除无关联学院：软删除成功', async ({ page }) => {
    const uniqueName = `可删除学院_${Date.now()}`;
    // Create
    await page.locator('button').filter({ hasText: '添加学院' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    let dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().fill(uniqueName);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    
    // Search for the created item
    const searchInput = page.locator('input[placeholder*="搜索学院"]').first();
    await searchInput.click();
    await searchInput.fill(uniqueName);
    await page.waitForTimeout(500);
    await page.locator('button').filter({ hasText: '搜索' }).first().click();
    await page.waitForTimeout(2000);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.el-table__body tbody').first()).toContainText(uniqueName, { timeout: 5000 });

    // Delete from search results
    const row = page.locator('.el-table__body tbody tr').filter({ hasText: uniqueName }).first();
    await row.locator('button').filter({ hasText: /删除/ }).first().click();
    await page.waitForTimeout(500);

    // Confirm deletion via ElMessageBox
    await page.waitForSelector('.el-message-box', { timeout: 5000 });
    await page.waitForTimeout(300);
    await page.locator('.el-message-box__btns .el-button--primary').first().click();
    await page.waitForTimeout(500);
    // Backend does soft delete, verify success message appears
    const successMsg = await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => null);
    expect(successMsg).not.toBeNull();
    // Soft delete confirmed: backend sets status to INACTIVE, frontend shows success message
  });

  test.afterEach(async ({ page }, testInfo) => {
    if (testInfo.status !== 'passed') {
      const { captureFailure } = require('../utils/screenshot');
      await captureFailure(page, testInfo.title);
    }
  });
});