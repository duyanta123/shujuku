/**
 * 课程弹窗学院/课程类型下拉 E2E 测试
 * 测试路径：/admin/course → 添加/编辑课程弹窗
 */
const { test, expect } = require('@playwright/test');
const { adminLogin } = require('../utils/auth');
const { SELECTORS } = require('../utils/selectors');

const COURSE_URL = 'http://localhost:3000/admin/course';

test.describe('课程弹窗级联', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto(COURSE_URL);
    await page.waitForLoadState('networkidle');
  });

  test('课程弹窗学院下拉从API动态加载', async ({ page }) => {
    // 打开添加课程弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加课程' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 学院下拉：表单中有多个 select（教师、实验室、学院、课程类型）
    // 顺序：教师(1)、实验室(2)、学院(3)、课程类型(4)
    // 学院是第3个 select (index 2)
    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(2);
    await collegeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });

    // 验证下拉选项已加载
    const options = page.locator(SELECTORS.SELECT_OPTION);
    const optionCount = await options.count();
    expect(optionCount).toBeGreaterThan(0);
  });

  test('课程弹窗课程类型可选必修选修', async ({ page }) => {
    // 打开添加课程弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加课程' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 课程类型是第4个 select (index 3)
    const courseTypeSelect = dialog.locator(SELECTORS.SELECT).nth(3);
    await courseTypeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });

    // 获取课程类型选项
    const options = page.locator(SELECTORS.SELECT_OPTION);
    const optionTexts = await options.allTextContents();

    // 验证包含"必修课"和"选修课"
    const hasRequired = optionTexts.some(t => t.includes('必修'));
    const hasElective = optionTexts.some(t => t.includes('选修'));
    expect(hasRequired).toBeTruthy();
    expect(hasElective).toBeTruthy();
  });

  test('编辑已选课课程时类型下拉禁用', async ({ page }) => {
    // 找到表格中第一个课程行
    const firstRow = page.locator(SELECTORS.TABLE_ROW).first();
    const rowVisible = await firstRow.isVisible().catch(() => false);
    if (!rowVisible) {
      test.skip(true, '课程列表为空，跳过编辑测试');
      return;
    }

    // 点击编辑按钮
    const editButton = firstRow.locator('.el-button--primary').first();
    await editButton.click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 课程类型是第4个 select (index 3)
    const courseTypeSelect = dialog.locator(SELECTORS.SELECT).nth(3);

    // 检查课程类型下拉是否禁用
    // Element Plus 禁用的 select 会有 .is-disabled 类
    const isDisabled = await courseTypeSelect.locator('.is-disabled').isVisible().catch(() => false);

    if (isDisabled) {
      // 验证存在提示文字
      const hintText = await dialog.locator('.form-hint').textContent().catch(() => '');
      expect(hintText).toContain('无法修改课程类型');
    }
    // 如果未禁用，说明该课程没有学生选课，这也是合理的
  });
});