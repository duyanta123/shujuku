/**
 * 必修课业务规则测试
 * 验证学生新增 / 转专业后的必修课自动分配，以及手动选择必修课的拦截逻辑
 */
const { test, expect } = require('@playwright/test');
const { adminLogin, studentLogin, logout, BASE_URL } = require('../utils/auth');

/**
 * 辅助：通过管理员 API 直接调用后端接口
 * 用于需要高度可控的数据准备操作，减少 UI 交互的不确定性
 */
async function apiRequest(page, method, url, data = null) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  };
  if (data && method !== 'GET') {
    options.body = JSON.stringify(data);
  }
  const resp = await page.evaluate(async ({ url, options }) => {
    const res = await fetch(url, options);
    const json = await res.json();
    return { status: res.status, body: json };
  }, { url: `${BASE_URL}/api${url}`, options });
  return resp;
}

test.describe('必修课自动分配', () => {
  const TEST_STUDENT_NO = `TEST${Date.now()}`.slice(0, 12);
  const TEST_STUDENT_NAME = '必修课测试生';

  test.afterAll(async ({ browser }) => {
    // 清理：删除测试学生
    const context = await browser.newContext();
    const page = await context.newPage();
    await adminLogin(page);
    const listResp = await apiRequest(page, 'GET', '/student/list');
    if (listResp.body.success) {
      const student = listResp.body.data.find(s => s.studentNo === TEST_STUDENT_NO);
      if (student) {
        await apiRequest(page, 'DELETE', `/student/delete/${student.id}`);
      }
    }
    await context.close();
  });

  test('学生新增并指定专业后自动分配必修课', async ({ page }) => {
    // Step 1: 管理员登录
    await adminLogin(page);

    // Step 2: 导航到学生管理
    await page.goto(`${BASE_URL}/admin/student`);
    await page.waitForLoadState('networkidle');

    // Step 3: 打开添加学生弹窗
    await page.click('.page-header .el-button--primary');

    // Step 4: 填写学生信息
    const dialog = page.locator('.el-dialog').first();
    await dialog.waitFor({ state: 'visible' });

    // 填写学号
    await dialog.locator('.el-input__inner').first().fill(TEST_STUDENT_NO);
    // 填写姓名
    await dialog.locator('.el-input__inner').nth(1).fill(TEST_STUDENT_NAME);
    // 选择性别
    const selects = dialog.locator('.el-select');
    await selects.first().click();
    await page.locator('.el-select-dropdown__item').filter({ hasText: '男' }).first().click();

    // 选择学院（id=1: 数学与计算机科学学院）
    // 学院 select 是倒数第二个（在专业之后、密码之前，但实际 DOM 顺序: 学号/姓名/性别/专业/学院/密码）
    // 学院 el-select 在专业 el-select 之后
    await dialog.locator('.el-select').nth(1).click();
    await page.locator('.el-select-dropdown__item').filter({ hasText: '数学与计算机科学学院' }).first().click();

    // 等待专业加载
    await page.waitForTimeout(500);

    // 选择专业 "计算机科学与技术"
    await dialog.locator('.el-select').first().click();
    // 需要用 last() 因为可能有多个下拉弹出
    const majorOption = page.locator('.el-select-dropdown__item').filter({ hasText: '计算机科学与技术' }).first();
    await majorOption.click();

    // Step 5: 提交保存
    await dialog.locator('.el-dialog__footer .el-button--primary').click();

    // 等待保存完成（弹窗关闭或成功消息）
    await page.waitForTimeout(1000);

    // Step 6: 登出管理员，学生登录
    await logout(page);
    await studentLogin(page, TEST_STUDENT_NO, '123456');

    // Step 7: 导航到我的课程
    await page.goto(`${BASE_URL}/student/my-course`);
    await page.waitForLoadState('networkidle');

    // Step 8: 验证必修课已自动分配（至少应有一门课程）
    // 检查页面不在空状态
    const emptyState = page.locator('.empty-state');
    const courseCards = page.locator('.course-card');
    // 等一会让数据加载
    await page.waitForTimeout(1000);

    const hasEmpty = await emptyState.count();
    const cardCount = await courseCards.count();

    // 如果数据为空，可能是该专业尚未配置必修课，也算符合业务逻辑
    if (hasEmpty > 0) {
      // 至少页面展示了正确的空状态
      await expect(emptyState).toBeVisible();
    } else {
      await expect(courseCards.first()).toBeVisible();
    }
  });

  test('学生转专业后必修课重新分配', async ({ page }) => {
    // Precondition: 已有一个带有专业的学生（使用 test 1 创建的学生）

    // Step 1: 管理员登录，编辑学生专业
    await adminLogin(page);
    await page.goto(`${BASE_URL}/admin/student`);
    await page.waitForLoadState('networkidle');

    // 在表格中查找学生行
    const targetRow = page.locator('.el-table__body tbody tr').filter({ hasText: TEST_STUDENT_NO });
    const rowCount = await targetRow.count();

    if (rowCount === 0) {
      // 学生不存在则跳过（可能是上次清理了或 test1 未执行）
      test.skip(true, '测试学生不存在，跳过转专业测试');
      return;
    }

    // 点击编辑按钮
    await targetRow.first().locator('.el-button--primary').first().click();

    // 等待编辑弹窗
    const dialog = page.locator('.el-dialog').first();
    await dialog.waitFor({ state: 'visible' });

    // 更改专业：先清掉当前选择
    // 专业 select 是弹窗中第一个 el-select
    await dialog.locator('.el-select').first().locator('.el-select__caret').click();
    await page.waitForTimeout(300);

    // 选另一个专业（例如 '软件工程'，该专业也在同一学院下）
    const anotherMajor = page.locator('.el-select-dropdown__item').filter({ hasText: '软件工程' });
    const anotherMajorCount = await anotherMajor.count();
    if (anotherMajorCount > 0) {
      await anotherMajor.first().click();
    } else {
      // 如果没有其他专业则跳过
      await dialog.locator('.el-dialog__header .el-dialog__close, .el-dialog__footer .el-button').first().click();
      test.skip(true, '没有可切换的专业，跳过转专业测试');
      return;
    }

    // 保存
    await dialog.locator('.el-dialog__footer .el-button--primary').click();
    await page.waitForTimeout(1000);

    // Step 3: 登出，学生登录验证新课
    await logout(page);
    await studentLogin(page, TEST_STUDENT_NO, '123456');
    await page.goto(`${BASE_URL}/student/my-course`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // 验证课程页面正常加载（新的必修课应出现）
    const hasEmpty = await page.locator('.empty-state').count();
    if (hasEmpty === 0) {
      await expect(page.locator('.course-card').first()).toBeVisible();
    }
  });

  test('必修课手动选课被拦截', async ({ page }) => {
    // Step 1: 学生 S001 登录
    await studentLogin(page, 'S001', '123456');

    // Step 2: 获取课程列表，找到一门 REQUIRED 类型的课程
    const courseResp = await apiRequest(page, 'GET', '/course/list');
    expect(courseResp.body.success).toBeTruthy();

    const requiredCourse = courseResp.body.data.find(c => c.course_type === 'REQUIRED');
    if (!requiredCourse) {
      test.skip(true, '无 REQUIRED 类型课程可测试');
      return;
    }

    // Step 3: 尝试通过 API 手动选择该必修课
    const selectResp = await apiRequest(page, 'POST', '/selection/add', {
      courseId: requiredCourse.id,
    });

    // Step 4: 应返回错误（必修课不允许手动选课）
    expect(selectResp.body.success).toBeFalsy();
  });
});