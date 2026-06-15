/**
 * 教师课程绑定业务规则测试
 * 验证教师绑定课程的唯一性，以及教师调学院需先解除绑定的约束
 */
const { test, expect } = require('@playwright/test');
const { adminLogin, BASE_URL } = require('../utils/auth');

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

test.describe('教师课程绑定校验', () => {

  test('教师绑定课程唯一性校验', async ({ page }) => {
    // Step 1: 管理员登录
    await adminLogin(page);

    // Step 2: 获取教师列表，找一位教师
    const teacherResp = await apiRequest(page, 'GET', '/teacher/list');
    expect(teacherResp.body.success).toBeTruthy();
    const teachers = teacherResp.body.data;
    if (teachers.length === 0) {
      test.skip(true, '无教师数据可测试');
      return;
    }

    // 选择第一位教师
    const targetTeacher = teachers[0];

    // Step 3: 获取课程列表，查看该教师是否已有绑定课程
    const courseResp = await apiRequest(page, 'GET', '/course/list/simple');
    expect(courseResp.body.success).toBeTruthy();

    const existingBinding = courseResp.body.data.find(
      c => c.teacher_id === targetTeacher.id || c.teacherId === targetTeacher.id
    );

    if (existingBinding) {
      // 教师已有课程绑定，尝试通过 API 再绑定另一门课
      // 获取该教师学院的另一门可选课程（或新建一门课）
      const teacherCollegeId = targetTeacher.college_id || targetTeacher.collegeId;

      // 尝试通过 API 直接创建课程，将同一教师绑到不同课程
      const addResp = await apiRequest(page, 'POST', '/course/add', {
        courseName: `测试课程_${Date.now()}`,
        teacherId: targetTeacher.id,
        labId: existingBinding.lab_id || existingBinding.labId,
        courseTime: '周五 5-6节',
        collegeId: teacherCollegeId,
        courseType: 'ELECTIVE',
        maxCount: 30,
      });

      // 后端应返回错误或阻止重复绑定
      // 如果成功创建了，记录课程 ID 以便清理
      if (addResp.body.success) {
        // 如果后端允许了，说明该业务规则可能未实施
        console.warn('[测试] 教师重复绑定未被后端拦截，课程ID:', addResp.body.data?.id);
        // 清理测试课程
        await apiRequest(page, 'DELETE', `/course/delete/${addResp.body.data.id}`);
      }
      // 期望失败（不允许重复绑定）
      expect(addResp.body.success).toBeFalsy();
    } else {
      // 教师无课程绑定，先绑定一门课再验证
      // 获取实验室列表
      const labResp = await apiRequest(page, 'GET', '/lab/list');
      const teacherCollegeId = targetTeacher.college_id || targetTeacher.collegeId;

      if (labResp.body.success && labResp.body.data.length > 0) {
        const labId = labResp.body.data[0].id;

        // 创建第一门课程
        const addResp1 = await apiRequest(page, 'POST', '/course/add', {
          courseName: `绑定测试A_${Date.now()}`,
          teacherId: targetTeacher.id,
          labId: labId,
          courseTime: '周一 1-2节',
          collegeId: teacherCollegeId,
          courseType: 'ELECTIVE',
          maxCount: 30,
        });
        expect(addResp1.body.success).toBeTruthy();
        const course1Id = addResp1.body.data?.id;

        // 尝试创建第二门课程使用同一教师
        const addResp2 = await apiRequest(page, 'POST', '/course/add', {
          courseName: `绑定测试B_${Date.now()}`,
          teacherId: targetTeacher.id,
          labId: labId,
          courseTime: '周三 3-4节',
          collegeId: teacherCollegeId,
          courseType: 'ELECTIVE',
          maxCount: 30,
        });

        // 清理：删除第一门课程
        if (course1Id) {
          await apiRequest(page, 'DELETE', `/course/delete/${course1Id}`);
        }
        // 如果第二门也创建了，清理
        if (addResp2.body.success && addResp2.body.data?.id) {
          await apiRequest(page, 'DELETE', `/course/delete/${addResp2.body.data.id}`);
        }

        expect(addResp2.body.success).toBeFalsy();
      } else {
        test.skip(true, '无实验室数据，跳过绑定测试');
      }
    }
  });

  test('教师调学院需先解除课程绑定', async ({ page }) => {
    // Step 1: 管理员登录
    await adminLogin(page);

    // Step 2: 获取教师列表和课程列表，找一个有课程绑定的教师
    const teacherResp = await apiRequest(page, 'GET', '/teacher/list');
    expect(teacherResp.body.success).toBeTruthy();

    const courseResp = await apiRequest(page, 'GET', '/course/list/simple');
    expect(courseResp.body.success).toBeTruthy();

    // 找到有课程绑定的教师
    const boundTeacher = teacherResp.body.data.find(t => {
      return courseResp.body.data.some(
        c => (c.teacher_id === t.id || c.teacherId === t.id)
      );
    });

    if (!boundTeacher) {
      test.skip(true, '没有已绑定课程的教师可测试');
      return;
    }

    // Step 3: 获取不同学院
    const collegeResp = await apiRequest(page, 'GET', '/college/list?status=ACTIVE&size=999');
    const colleges = collegeResp.body.data?.records || collegeResp.body.data || [];

    const teacherCollegeId = boundTeacher.college_id || boundTeacher.collegeId;
    const otherCollege = colleges.find(c => c.id !== teacherCollegeId);

    if (!otherCollege) {
      test.skip(true, '无其他学院可切换');
      return;
    }

    // Step 4: 尝试通过 API 更新教师学院
    const updateResp = await apiRequest(page, 'PUT', '/teacher/update', {
      id: boundTeacher.id,
      teacherNo: boundTeacher.teacherNo,
      name: boundTeacher.name,
      title: boundTeacher.title,
      collegeId: otherCollege.id,
    });

    // Step 5: 期望失败 —— 教师有课程绑定，不能调学院
    expect(updateResp.body.success).toBeFalsy();
  });
});