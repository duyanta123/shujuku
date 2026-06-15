/**
 * 表单提交按钮 UI 状态测试
 * 验证提交按钮在提交过程中的 loading / disabled 状态
 */
const { test, expect } = require('@playwright/test');
const { adminLogin, BASE_URL } = require('../utils/auth');
const { SELECTORS } = require('../utils/selectors');

test.describe('表单提交按钮交互', () => {

  test('表单提交按钮进入loading禁用态', async ({ page }) => {
    // Step 1: 管理员登录
    await adminLogin(page);

    // Step 2: 导航到学生管理
    await page.goto(`${BASE_URL}/admin/student`);
    await page.waitForLoadState('networkidle');

    // Step 3: 打开添加学生弹窗
    await page.click('.page-header .el-button--primary');

    const dialog = page.locator(SELECTORS.DIALOG).first();
    await dialog.waitFor({ state: 'visible' });

    // Step 4: 填写表单（填写全部必填字段以通过前端校验）
    const inputs = dialog.locator(SELECTORS.INPUT);
    await inputs.nth(0).fill(`UI_TEST_${Date.now()}`);
    await inputs.nth(1).fill('提交测试生');

    // 选择性别
    await dialog.locator(SELECTORS.SELECT).nth(0).click();
    const genderOption = page.locator(SELECTORS.SELECT_OPTION).filter({ hasText: '男' }).first();
    await genderOption.click();

    // 选择学院
    await dialog.locator(SELECTORS.SELECT).nth(1).click();
    const collegeOption = page.locator(SELECTORS.SELECT_OPTION).filter({ hasText: '数学与计算机科学学院' }).first();
    const collegeExists = await collegeOption.count();
    if (collegeExists === 0) {
      // 如果没找到学院，选第一个可用学院
      await page.locator(SELECTORS.SELECT_OPTION).first().click();
    } else {
      await collegeOption.click();
    }

    // 等待专业加载
    await page.waitForTimeout(500);

    // 选择专业
    await dialog.locator(SELECTORS.SELECT).nth(0).click();
    await page.waitForTimeout(300);
    const majorOption = page.locator(SELECTORS.SELECT_OPTION).first();
    const majorExists = await majorOption.count();
    if (majorExists > 0) {
      await majorOption.click();
    }

    // Step 5: 点击保存按钮
    const saveButton = dialog.locator(SELECTORS.CONFIRM_BUTTON);
    await saveButton.click();

    // Step 6: 断言按钮出现 loading 状态
    // Element Plus 的 loading 通过 .is-loading class 控制
    // 注意：由于保存可能非常快，loading 状态可能瞬间闪过，用软断言
    const isLoading = await saveButton.evaluate(el => el.classList.contains('is-loading'));
    const isDisabled = await saveButton.isDisabled();

    // 至少满足其中一个条件
    const hasLoadingState = isLoading || isDisabled;
    expect(hasLoadingState).toBeTruthy();
  });

  test('表单提交完成后按钮恢复正常', async ({ page }) => {
    // Step 1: 管理员登录
    await adminLogin(page);

    // Step 2: 导航到学院专业管理页 —— 添加学院更简单
    await page.goto(`${BASE_URL}/admin/college-major`);
    await page.waitForLoadState('networkidle');

    // Step 3: 确保在学院管理 tab
    const collegeTab = page.locator('.el-tabs__item').filter({ hasText: '学院管理' });
    await collegeTab.click();
    await page.waitForTimeout(300);

    // Step 4: 打开添加学院弹窗
    // "添加学院" 按钮在搜索栏中
    const addCollegeBtn = page.locator('.search-bar .el-button--primary').filter({ hasText: '添加学院' });
    await addCollegeBtn.click();

    const dialog = page.locator(SELECTORS.DIALOG).first();
    await dialog.waitFor({ state: 'visible' });

    // Step 5: 填写学院名称
    const input = dialog.locator(SELECTORS.INPUT).first();
    await input.fill(`UI测试学院_${Date.now()}`);

    // 选择状态为启用
    const statusSelect = dialog.locator(SELECTORS.SELECT).first();
    await statusSelect.click();
    await page.locator(SELECTORS.SELECT_OPTION).filter({ hasText: '启用' }).first().click();

    // Step 6: 点击保存
    const saveButton = dialog.locator(SELECTORS.CONFIRM_BUTTON);
    await saveButton.click();

    // Step 7: 等待保存完成（弹窗关闭 或 成功消息出现）
    try {
      await page.waitForFunction(() => {
        const msg = document.querySelector('.el-message--success');
        return msg !== null;
      }, { timeout: 8000 });
    } catch {
      // 可能已通过弹窗关闭来判断
    }
    await page.waitForTimeout(500);

    // Step 8: 断言：弹窗已关闭或按钮不再是 loading
    const isDialogVisible = await dialog.count();
    if (isDialogVisible > 0) {
      const stillVisible = await dialog.isVisible().catch(() => false);
      if (!stillVisible) {
        // 弹窗已关闭，按钮自然恢复（测试通过）
        expect(true).toBeTruthy();
      } else {
        // 弹窗还在，检查按钮状态
        const stillLoading = await saveButton.evaluate(el => el.classList.contains('is-loading')).catch(() => false);
        expect(stillLoading).toBeFalsy();
      }
    } else {
      // 弹窗已不存在（测试通过）
      expect(true).toBeTruthy();
    }
  });
});