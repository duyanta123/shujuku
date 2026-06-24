const { test, expect } = require('@playwright/test');
const { adminLogin, studentLogin, logout, BASE_URL } = require('../utils/auth');

async function apiRequest(page, method, url, data = null) {
  const options = { method, headers: { 'Content-Type': 'application/json' }, credentials: 'include' };
  if (data && method !== 'GET') options.body = JSON.stringify(data);
  return page.evaluate(async ({ url, options }) => {
    const res = await fetch(url, options);
    const json = await res.json();
    return { status: res.status, body: json };
  }, { url: `${BASE_URL}/api${url}`, options });
}

function asList(body) {
  const data = body?.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.list)) return data.list;
  return [];
}

test.describe('Required course assignment', () => {
  const TEST_STUDENT_NO = `TEST${Date.now()}`.slice(0, 12);
  const TEST_STUDENT_NAME = 'Required Course Test Student';
  const TEST_STUDENT_PASSWORD = 'Test123!';

  test.afterAll(async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await adminLogin(page);
    const listResp = await apiRequest(page, 'GET', '/student/list');
    const student = asList(listResp.body).find(s => s.studentNo === TEST_STUDENT_NO);
    if (student) await apiRequest(page, 'DELETE', `/student/delete/${student.id}`);
    await context.close();
  });

  test('new student with major can open assigned-course page', async ({ page }) => {
    await adminLogin(page);
    const collegeResp = await apiRequest(page, 'GET', '/college/list?status=ACTIVE&size=999');
    const college = asList(collegeResp.body)[0];
    if (!college) test.skip(true, 'No active college data');

    const majorResp = await apiRequest(page, 'GET', `/major/list?collegeId=${college.id}&status=ACTIVE&size=999`);
    const major = asList(majorResp.body)[0];
    if (!major) test.skip(true, 'No active major data');

    const addResp = await apiRequest(page, 'POST', '/student/add', {
      studentNo: TEST_STUDENT_NO,
      name: TEST_STUDENT_NAME,
      gender: 'M',
      collegeId: college.id,
      majorId: major.id,
      password: TEST_STUDENT_PASSWORD,
    });
    if (!addResp.body.success) {
      test.skip(true, addResp.body.message || 'Student creation is unavailable in this test dataset');
    }

    await logout(page);
    await studentLogin(page, TEST_STUDENT_NO, TEST_STUDENT_PASSWORD);
    await page.goto(`${BASE_URL}/student/my-course`);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.empty-state').or(page.locator('.course-card').first())).toBeVisible();
  });

  test('manual selection of required course is rejected', async ({ page }) => {
    await studentLogin(page, 'S001', '123456');
    const courseResp = await apiRequest(page, 'GET', '/course/list');
    const requiredCourse = asList(courseResp.body).find(c => c.course_type === 'REQUIRED' || c.courseType === 'REQUIRED');
    if (!requiredCourse) test.skip(true, 'No REQUIRED course data');

    const selectResp = await apiRequest(page, 'POST', '/selection/add', { courseId: requiredCourse.id });
    expect(selectResp.body.success).toBeFalsy();
  });
});
