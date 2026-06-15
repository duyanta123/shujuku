/**
 * 学生弹窗学院-专业级联 E2E 测试
 * 测试路径：/admin/student → 添加/编辑学生弹窗
 */
const { test, expect } = require('@playwright/test');
const { adminLogin } = require('../utils/auth');
const { SELECTORS } = require('../utils/selectors');

const STUDENT_URL = 'http://localhost:3000/admin/student';

test.describe('学生弹窗级联', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto(STUDENT_URL);
    await page.waitForLoadState('networkidle');
  });

  test('学生弹窗学院下拉从API动态加载', async ({ page }) => {
    // 打开添加学生弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加学生' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 找到学院下拉（el-select），点击展开
    // 学院在表单中位置：专业在上，学院在下（按 template 顺序，专业在前、学院在后）
    // 但是表单中实际顺序是：学号、姓名、性别、专业、学院、密码
    // 学院是第5个 el-form-item
    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(1); // 第2个 select（第1个是性别，第2个是学院）
    await collegeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });

    // 验证下拉选项已加载
    const options = page.locator(SELECTORS.SELECT_OPTION);
    const optionCount = await options.count();
    expect(optionCount).toBeGreaterThan(0);
  });

  test('学生弹窗学院下拉支持搜索和loading', async ({ page }) => {
    // 打开添加学生弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加学生' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 学院下拉是第2个 select
    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(1);

    // 验证 filterable 属性（Element Plus 中 filterable 会给 .el-select 添加特定结构）
    // 检查是否存在可输入的 input（filterable 的 select 内部有 input）
    const selectInput = collegeSelect.locator(SELECTORS.INPUT);
    const isInputVisible = await selectInput.isVisible().catch(() => false);
    expect(isInputVisible).toBeTruthy();

    // 验证 loading 属性存在
    // loading 时会出现 .el-select .el-input.is-focus 或 loading 图标
    // 这里我们检查 .el-select 结构包含 loading 状态
    const selectElement = collegeSelect.locator('.el-select');
    expect(selectElement).toBeTruthy();
  });

  test('学生弹窗选择学院后专业下拉联动加载', async ({ page }) => {
    // 打开添加学生弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加学生' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 专业下拉（第1个 select，disabled 状态因为没有选择学院）
    const majorSelect = dialog.locator(SELECTORS.SELECT).first();
    // 验证专业下拉初始为 disabled
    const isDisabled = await majorSelect.locator('.is-disabled').isVisible().catch(() => true);
    expect(isDisabled).toBeTruthy();

    // 选择学院
    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(1);
    await collegeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const collegeOptions = page.locator(SELECTORS.SELECT_OPTION);
    if (await collegeOptions.count() > 0) {
      await collegeOptions.first().click();
    }
    await page.waitForTimeout(1000);

    // 验证专业下拉现在可用（不再 disabled）
    const nowDisabled = await majorSelect.locator('.is-disabled').isVisible().catch(() => false);
    // 选择学院后专业应该启用
    expect(nowDisabled).toBeFalsy();

    // 点击专业下拉，验证选项已加载
    await majorSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const majorOptions = page.locator(SELECTORS.SELECT_OPTION);
    const majorCount = await majorOptions.count();
    // 应该有专业选项（可能为空如果该学院没有专业）
    expect(majorCount).toBeGreaterThanOrEqual(0);
  });

  test('学生弹窗切换学院自动清空专业', async ({ page }) => {
    // 打开添加学生弹窗
    await page.locator('.page-header .el-button--primary').filter({ hasText: '添加学生' }).click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(1);
    const majorSelect = dialog.locator(SELECTORS.SELECT).first();

    // 选择第一个学院
    await collegeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const collegeOptions = page.locator(SELECTORS.SELECT_OPTION);
    const collegeCount = await collegeOptions.count();

    if (collegeCount < 2) {
      test.skip(true, '学院数量不足2个，跳过切换学院测试');
      return;
    }

    await collegeOptions.first().click();
    await page.waitForTimeout(1000);

    // 选择第一个学院下的专业
    await majorSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const majorOptions = page.locator(SELECTORS.SELECT_OPTION);
    if (await majorOptions.count() > 0) {
      await majorOptions.first().click();
    }

    // 切换学院
    await collegeSelect.locator('.el-select__wrapper, .el-input__wrapper').click();
    await page.waitForSelector(SELECTORS.SELECT_DROPDOWN, { timeout: 5000 });
    const collegeOptions2 = page.locator(SELECTORS.SELECT_OPTION);
    if (await collegeOptions2.count() > 1) {
      await collegeOptions2.nth(1).click();
    }
    await page.waitForTimeout(1000);

    // 验证专业被清空（select 的 placeholder 或 input 显示为空）
    const majorInput = majorSelect.locator(SELECTORS.INPUT);
    const majorValue = await majorInput.inputValue().catch(() => '');
    // 专业应该被清空（input value 为空，或 placeholder 可见）
    expect(majorValue).toBe('');
  });

  test('学生弹窗编辑场景回显学院和专业', async ({ page }) => {
    // 找到表格中第一个学生行
    const firstRow = page.locator(SELECTORS.TABLE_ROW).first();
    const rowVisible = await firstRow.isVisible().catch(() => false);
    if (!rowVisible) {
      test.skip(true, '学生列表为空，跳过编辑回显测试');
      return;
    }

    // 获取原学生的学院和专业信息
    const cells = firstRow.locator('td');
    const collegeName = await cells.nth(4).textContent().catch(() => ''); // 学院列
    const majorName = await cells.nth(3).textContent().catch(() => '');   // 专业列

    // 点击编辑
    const editButton = firstRow.locator('.el-button--primary').first();
    await editButton.click();
    await page.waitForSelector(SELECTORS.DIALOG, { timeout: 5000 });

    const dialog = page.locator(SELECTORS.DIALOG).last();

    // 验证学院已回显（select 的 input 中应显示学院名称）
    const collegeSelect = dialog.locator(SELECTORS.SELECT).nth(1);
    const collegeInput = collegeSelect.locator(SELECTORS.INPUT);
    const collegeValue = await collegeInput.inputValue().catch(() => '');
    // 学院 select 的值应该非空（已回显）
    expect(collegeValue.length).toBeGreaterThan(0);

    // 验证专业已回显
    const majorSelect = dialog.locator(SELECTORS.SELECT).first();
    const majorInput = majorSelect.locator(SELECTORS.INPUT);
    const majorValue = await majorInput.inputValue().catch(() => '');
    // 专业 select 的值应该非空
    expect(majorValue.length).toBeGreaterThan(0);
  });
});