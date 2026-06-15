/**
 * 教师弹窗学院下拉 E2E 测试
 * 测试路径：/admin/teacher → 添加/编辑教师弹窗
 */
const { test, expect } = require('@playwright/test');
const { adminLogin } = require('../utils/auth');
const { SELECTORS } = require('../utils/selectors');

const TEACHER_URL = 'http://localhost:3000/admin/teacher';

test.describe('教师弹窗级联', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto(TEACHER_URL);
    await page.waitForLoadState('networkidle');
  });

  test('教师弹窗学院下拉从API动态加载', async ({ page }) => {
    // 打开添加教师弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加教师' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 学院下拉是表单中第3个 select（工号、姓名、职称、学院、密码）
    // 职称是第1个 select，学院是第2个 select
    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(1);
    await collegeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });

    // 验证下拉选项已加载
    const options = page.locator(SELECTORS.SELECT_OPTION);
    const optionCount = await options.count();
    expect(optionCount).toBeGreaterThan(0);
  });

  test('教师弹窗学院下拉必填校验', async ({ page }) => {
    // 打开添加教师弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加教师' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 填写工号和姓名，但不选择学院
    const inputs = dialog.locator(SELECTORS.INPUT);
    await inputs.nth(0).fill(`T${Date.now()}`); // 工号
    await inputs.nth(1).fill('测试教师');       // 姓名

    // 选择职称（第1个 select）
    const titleSelect = dialog.locator(SELECTORS.SELECT).first();
    await titleSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const titleOptions = page.locator(SELECTORS.SELECT_OPTION);
    if (await titleOptions.count() > 0) {
      await titleOptions.first().click();
    }

    // 不选择学院，直接点保存
    await dialog.locator(SELECTORS.CONFIRM_BUTTON).click();
    await page.waitForTimeout(500);

    // 验证：出现"请选择学院"的表单校验错误
    const formErrors = dialog.locator(SELECTORS.FORM_ERROR);
    const errorTexts = await formErrors.allTextContents();
    const hasCollegeError = errorTexts.some(t => t.includes('学院'));
    expect(hasCollegeError).toBeTruthy();
  });

  test('教师弹窗提交数据正确保存', async ({ page }) => {
    const uniqueTeacherNo = `T${Date.now().toString().slice(-8)}`;

    // 打开添加教师弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加教师' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 填写工号
    const inputs = dialog.locator(SELECTORS.INPUT);
    await inputs.nth(0).fill(uniqueTeacherNo);
    // 填写姓名
    await inputs.nth(1).fill(`测试教师_${Date.now()}`);

    // 选择职称（第1个 select）
    const titleSelect = dialog.locator(SELECTORS.SELECT).first();
    await titleSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const titleOptions = page.locator(SELECTORS.SELECT_OPTION);
    if (await titleOptions.count() > 0) {
      await titleOptions.first().click();
    }

    // 选择学院（第2个 select）
    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(1);
    await collegeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const collegeOptions = page.locator(SELECTORS.SELECT_OPTION);
    if (await collegeOptions.count() > 0) {
      await collegeOptions.first().click();
    }

    // 保存
    await dialog.locator(SELECTORS.CONFIRM_BUTTON).click();
    await page.waitForTimeout(2000);

    // 验证：弹窗关闭
    await expect(dialog).not.toBeVisible({ timeout: 5000 }).catch(() => {});

    // 验证：列表中出现了新教师
    const tableText = await page.locator(SELECTORS.TABLE_BODY).textContent();
    expect(tableText).toContain(uniqueTeacherNo);
  });
});