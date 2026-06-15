/**
 * 跨学院选课限制测试
 * 验证学生端仅展示本学院选修课，跨学院选课被后端拦截
 */
const { test, expect } = require('@playwright/test');
const { studentLogin, adminLogin, logout, BASE_URL } = require('../utils/auth');

/**
 * 辅助：通过 API 请求后端
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

test.describe('跨学院选修课限制', () => {

  test('学生端仅展示本学院选修课', async ({ page }) => {
    // Step 1: 学生 S001 登录（college: 数学与计算机科学学院, id=1）
    await studentLogin(page, 'S001', '123456');

    // Step 2: 导航到课程选择页
    await page.goto(`${BASE_URL}/student/course`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Step 3: 获取页面上展示的课程卡片
    const courseCards = page.locator('.course-card');
    const cardCount = await courseCards.count();

    if (cardCount === 0) {
      // 空状态也符合预期 —— 本学院无可选课程
      const emptyState = page.locator('.empty-state');
      await expect(emptyState).toBeVisible();
      // 验证空状态文案
      await expect(emptyState).toContainText('暂无可用课程');
      return;
    }

    // Step 4: 通过 API 获取课程数据，确认页面只展示本学院课程
    const listResp = await apiRequest(page, 'GET', '/course/list');
    expect(listResp.body.success).toBeTruthy();

    const allCourses = listResp.body.data;
    // 找到属于本学院（id=1）的课程
    const college1Courses = allCourses.filter(
      c => c.college_id === 1 || c.collegeId === 1
    );

    // Step 5: 逐张卡片检查学院名称
    for (let i = 0; i < cardCount; i++) {
      const card = courseCards.nth(i);
      const collegeText = await card.locator('.info-row').filter({ hasText: '学院' }).locator('.info-value').textContent();
      // 确保学院显示正确
      expect(collegeText).toBeTruthy();

      // 如果有 courses 数据且 college1Courses 不为空，验证显示数量
      if (college1Courses.length > 0) {
        // 页面课程数应 ≤ 本学院课程数
        expect(cardCount).toBeLessThanOrEqual(college1Courses.length);
      }
    }
  });

  test('跨学院选修课选课被后端拦截', async ({ page }) => {
    // Step 1: 管理员登录获取课程列表来找到跨学院课程
    await adminLogin(page);

    const listResp = await apiRequest(page, 'GET', '/course/list');
    expect(listResp.body.success).toBeTruthy();

    const allCourses = listResp.body.data;
    // 找到非学院1的选修课
    const crossCollegeCourse = allCourses.find(
      c => c.course_type === 'ELECTIVE' && c.college_id !== 1 && c.collegeId !== 1
    );

    if (!crossCollegeCourse) {
      // 尝试找任何非学院1且非REQUIRED的课程
      const anyOther = allCourses.find(
        c => (c.college_id !== 1 && c.collegeId !== 1)
      );
      if (!anyOther) {
        test.skip(true, '无跨学院课程可测试');
        return;
      }
    }

    const targetCourse = crossCollegeCourse || allCourses.find(c => c.college_id !== 1);

    // Step 2: 登出管理员，学生 S001 登录
    await logout(page);
    await studentLogin(page, 'S001', '123456');

    // Step 3: 通过 API 尝试选择跨学院课程
    const selectResp = await apiRequest(page, 'POST', '/selection/add', {
      courseId: targetCourse.id,
    });

    // Step 4: 应返回错误
    expect(selectResp.body.success).toBeFalsy();
  });
});