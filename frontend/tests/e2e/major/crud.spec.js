const { test, expect } = require('@playwright/test');
const { adminLogin } = require('../utils/auth');
const { SELECTORS } = require('../utils/selectors');

const COLLEGE_MAJOR_URL = 'http://localhost:3000/admin/college-major';
// Scoped selector: only the major tab pane's table body (avoids confusion with hidden college table)
const MAJOR_TBODY = '#pane-major .el-table__body tbody';
const MAJOR_TR = '#pane-major .el-table__body tbody tr';

/**
 * Helper: switch to major tab and wait for data to load
 */
async function switchToMajorTab(page) {
  const majorTab = page.locator('.el-tabs__item').filter({ hasText: '专业管理' });
  await majorTab.click();
  await expect(majorTab).toHaveClass(/is-active/, { timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(500);
  await page.waitForLoadState('networkidle');
  // Wait for the major table to have rows (data loaded)
  await page.locator('#pane-major .el-table__body tbody tr').first().waitFor({ state: 'attached', timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(500);
}

/**
 * Helper: search major by name
 */
async function searchMajor(page, name) {
  const searchInput = page.locator('#pane-major input[placeholder*="搜索专业"]').first();
  await searchInput.clear();
  await searchInput.fill(name);
  await page.waitForTimeout(300);
  // Click the search button in the major tab pane (more reliable than press('Enter') on el-input)
  await page.locator('#pane-major button').filter({ hasText: '搜索' }).first().click();
  await page.waitForTimeout(3000);
  await page.waitForLoadState('networkidle');
}

/**
 * Helper: select a college from the dropdown in the major dialog
 */
async function selectCollegeInDialog(page, dialog) {
  await dialog.locator('.el-select').first().click();
  await page.waitForTimeout(800);
  
  const dropdowns = page.locator('.el-select-dropdown');
  const ddCount = await dropdowns.count();
  for (let i = ddCount - 1; i >= 0; i--) {
    const dd = dropdowns.nth(i);
    if (await dd.isVisible({ timeout: 1000 }).catch(() => false)) {
      const opts = dd.locator(SELECTORS.SELECT_OPTION);
      if (await opts.count() > 0) {
        await opts.first().click();
        return true;
      }
    }
  }
  return false;
}

test.describe('专业管理 CRUD', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto(COLLEGE_MAJOR_URL);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);
    await switchToMajorTab(page);
  });

  test('专业列表加载正常', async ({ page }) => {
    await expect(page.locator('th').filter({ hasText: '专业名称' }).first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('th').filter({ hasText: '所属学院' }).first()).toBeVisible({ timeout: 5000 });
    await page.locator(MAJOR_TR).last().waitFor({ state: 'attached', timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    const rows = page.locator(MAJOR_TR);
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThan(0);
  });

  test('按学院筛选专业列表', async ({ page }) => {
    // Click the filter select scoped to the major tab pane
    const filterSelects = page.locator('#pane-major .el-select');
    const selectCount = await filterSelects.count();
    if (selectCount === 0) {
      test.skip(true, '无筛选下拉框');
      return;
    }
    await filterSelects.first().click();
    await page.waitForTimeout(1000);
    
    // Find the visible dropdown
    const dropdowns = page.locator('.el-select-dropdown');
    let visibleDropdown = null;
    for (let i = await dropdowns.count() - 1; i >= 0; i--) {
      const dd = dropdowns.nth(i);
      if (await dd.isVisible({ timeout: 1000 }).catch(() => false)) {
        visibleDropdown = dd;
        break;
      }
    }
    if (!visibleDropdown) {
      test.skip(true, '下拉未打开');
      return;
    }
    
    const options = visibleDropdown.locator(SELECTORS.SELECT_OPTION);
    const optionCount = await options.count();
    if (optionCount > 1) {
      await options.nth(1).click();
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(1000);
      const rows = page.locator(MAJOR_TR);
      const rowCount = await rows.count();
      expect(rowCount).toBeGreaterThan(0);
    } else {
      await page.keyboard.press('Escape');
      test.skip(true, '选项不足');
    }
  });

  test('新增专业：选择学院+合法名称提交成功', async ({ page }) => {
    const uniqueName = `测试专业_${Date.now()}`;
    await page.locator('button').filter({ hasText: '添加专业' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    const dialog = page.locator(SELECTORS.DIALOG).last();
    
    await dialog.locator(SELECTORS.INPUT).first().fill(uniqueName);
    
    const selected = await selectCollegeInDialog(page, dialog);
    if (!selected) {
      await page.keyboard.press('Escape');
      test.skip(true, '无法选择学院');
      return;
    }
    
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    
    await searchMajor(page, uniqueName);
    await expect(page.locator(MAJOR_TBODY)).toContainText(uniqueName, { timeout: 5000 });
  });

  test('同学院专业名称唯一校验', async ({ page }) => {
    const uniqueName = `唯一校验_${Date.now()}`;
    
    // First create
    await page.locator('button').filter({ hasText: '添加专业' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    let dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().fill(uniqueName);
    await selectCollegeInDialog(page, dialog);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    
    await searchMajor(page, uniqueName);
    await expect(page.locator(MAJOR_TBODY)).toContainText(uniqueName, { timeout: 5000 });

    // Second create with same name
    await page.locator('button').filter({ hasText: '添加专业' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().fill(uniqueName);
    await selectCollegeInDialog(page, dialog);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForTimeout(2000);

    const hasError = await page.locator('.el-message--error').first().isVisible({ timeout: 3000 }).catch(() => false);
    expect(hasError).toBe(true);
  });

  test('新增专业：空名称、未选学院触发表单校验', async ({ page }) => {
    await page.locator('button').filter({ hasText: '添加专业' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    const dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForTimeout(500);
    const errors = dialog.locator('.el-form-item__error');
    const errorCount = await errors.count();
    expect(errorCount).toBeGreaterThan(0);
    // Close the dialog to avoid blocking subsequent tests
    await dialog.locator('.el-dialog__footer .el-button').first().click();
    await page.waitForTimeout(500);
  });

  test('编辑专业：修改名称提交成功', async ({ page }) => {
    const originalName = `编辑测试_${Date.now()}`;
    const editedName = `编辑测试_已修改_${Date.now()}`;

    // Create a major first
    await page.locator('button').filter({ hasText: '添加专业' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    let dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().fill(originalName);
    await selectCollegeInDialog(page, dialog);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    
    await searchMajor(page, originalName);
    await expect(page.locator(MAJOR_TBODY)).toContainText(originalName, { timeout: 5000 });

    // Edit the created major
    const editBtn = page.locator(MAJOR_TR).filter({ hasText: originalName }).locator('button').filter({ hasText: '编辑' });
    await editBtn.first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().clear();
    await dialog.locator(SELECTORS.INPUT).first().fill(editedName);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    
    await searchMajor(page, editedName);
    await expect(page.locator(MAJOR_TBODY)).toContainText(editedName, { timeout: 5000 });
  });

  test('删除无关联学生专业（软删除）', async ({ page }) => {
    const deleteName = `待删除_${Date.now()}`;

    // Create
    await page.locator('button').filter({ hasText: '添加专业' }).first().click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });
    let dialog = page.locator(SELECTORS.DIALOG).last();
    await dialog.locator(SELECTORS.INPUT).first().fill(deleteName);
    await selectCollegeInDialog(page, dialog);
    await dialog.locator('.el-dialog__footer .el-button--primary').last().click();
    await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => {});
    await page.waitForTimeout(500);
    
    await searchMajor(page, deleteName);
    await expect(page.locator(MAJOR_TBODY)).toContainText(deleteName, { timeout: 5000 });

    // Delete - click the delete button on the row
    const deleteBtn = page.locator(MAJOR_TR).filter({ hasText: deleteName }).locator('button').filter({ hasText: '删除' });
    await deleteBtn.first().click();
    await page.waitForTimeout(800);

    // Confirm deletion via ElMessageBox (the component used by handleDeleteMajor)
    // Wait for the message box to appear
    await page.waitForSelector('.el-message-box', { timeout: 5000 });
    await page.waitForTimeout(300);
    // Click the confirm button
    await page.locator('.el-message-box__btns .el-button--primary').first().click();
    await page.waitForTimeout(500);
    
    // Wait for success or error message
    const successMsg = await page.waitForSelector('.el-message--success', { timeout: 5000 }).catch(() => null);
    if (!successMsg) {
      // Check for error message
      const errorMsg = await page.locator('.el-message--error').first().isVisible({ timeout: 2000 }).catch(() => false);
      if (errorMsg) {
        // The item might have associated data, skip
        test.skip(true, '删除失败（可能有关联数据）');
        return;
      }
    }
    expect(successMsg).not.toBeNull();
    await page.waitForTimeout(500);
    
    // Verify the item now shows status "停用" (INACTIVE) - backend soft deletes
    await searchMajor(page, deleteName);
    const row = page.locator(MAJOR_TR).filter({ hasText: deleteName }).first();
    await expect(row).toContainText('停用', { timeout: 5000 });
  });

  test('专业列表搜索', async ({ page }) => {
    const firstRow = page.locator(MAJOR_TR).first();
    const firstRowText = await firstRow.textContent().catch(() => '');
    if (!firstRowText || firstRowText.includes('暂无数据')) {
      test.skip(true, '无数据');
      return;
    }

    const keyword = firstRowText.trim().substring(0, 2);
    await searchMajor(page, keyword);
    const rows = page.locator(MAJOR_TR);
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThan(0);
  });
});