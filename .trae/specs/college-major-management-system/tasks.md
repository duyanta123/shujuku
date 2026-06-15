# Tasks

- [x] Task 1: 数据库改造 — 新增学院、专业、关联表及改造现有表
  - [x] SubTask 1.1: 编写 `college` 表建表 SQL（id, name UNIQUE, status ENUM, created_at DEFAULT CURRENT_TIMESTAMP, updated_at DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP）
  - [x] SubTask 1.2: 编写 `major` 表建表 SQL（id, name, college_id FK ON DELETE RESTRICT ON UPDATE CASCADE, status ENUM, UNIQUE(college_id, name), created_at, updated_at）
  - [x] SubTask 1.3: 编写 `major_required_course` 表建表 SQL（id PK, major_id FK, course_id FK, UNIQUE(major_id, course_id), created_at）
  - [x] SubTask 1.4: 编写 `course_teacher` 表建表 SQL（id PK, course_id FK, teacher_id FK UNIQUE, created_at）
  - [x] SubTask 1.5: 编写 ALTER TABLE 语句改造 student 表（新增 college_id FK ON DELETE RESTRICT ON UPDATE CASCADE, major_id FK, 保留原 college/major 字符串列兼容过渡）
  - [x] SubTask 1.6: 编写 ALTER TABLE 语句改造 teacher 表（新增 college_id FK ON DELETE RESTRICT ON UPDATE CASCADE）
  - [x] SubTask 1.7: 编写 ALTER TABLE 语句改造 course 表（新增 college_id FK ON DELETE RESTRICT ON UPDATE CASCADE, course_type ENUM('REQUIRED','ELECTIVE')）
  - [x] SubTask 1.8: 编写初始数据插入 SQL（预设学院和专业数据）
  - [x] SubTask 1.9: 更新 `init_database.sql` 整合所有变更
  - [x] SubTask 1.10: 为所有外键列创建普通索引（major.college_id, student.college_id, student.major_id, course.college_id, teacher.college_id）
  - [x] SubTask 1.11: 编写存量数据迁移 SQL（将 student/teacher/course 的 college 字符串按名称匹配映射为 college_id，含匹配失败异常处理）

- [x] Task 2: 后端 Entity 层 — 新增和改造实体类
  - [x] SubTask 2.1: 创建 `College.java` Entity（id, name @NotBlank @Size(max=100), status @NotNull, createdAt, updatedAt）
  - [x] SubTask 2.2: 创建 `Major.java` Entity（id, name @NotBlank @Size(max=100), collegeId @NotNull, status @NotNull, createdAt, updatedAt）
  - [x] SubTask 2.3: 创建 `MajorRequiredCourse.java` Entity（id, majorId, courseId, createdAt）
  - [x] SubTask 2.4: 创建 `CourseTeacher.java` Entity（id, courseId, teacherId, createdAt）
  - [x] SubTask 2.5: 改造 `Student.java` Entity（新增 collegeId, majorId 字段，保留原 college/major 字符串字段兼容过渡）
  - [x] SubTask 2.6: 改造 `Teacher.java` Entity（新增 collegeId 字段，保留原 college 字符串字段兼容过渡）
  - [x] SubTask 2.7: 改造 `Course.java` Entity（新增 collegeId, courseType 字段，保留原 college 字符串字段兼容过渡）

- [x] Task 3: 后端 Repository 层 — 新增数据访问接口
  - [x] SubTask 3.1: 创建 `CollegeRepository.java`（findByStatus, findByName）
  - [x] SubTask 3.2: 创建 `MajorRepository.java`（findByCollegeId, findByCollegeIdAndName, countByCollegeId）
  - [x] SubTask 3.3: 创建 `MajorRequiredCourseRepository.java`（findByMajorId, findByMajorIdAndCourseId, deleteByMajorIdAndCourseId）
  - [x] SubTask 3.4: 创建 `CourseTeacherRepository.java`（findByTeacherId, findByCourseId, deleteByTeacherId）

- [x] Task 4: 后端 Service 层 — 学院和专业业务逻辑
  - [x] SubTask 4.1: 创建 `CollegeService.java` 接口和 `CollegeServiceImpl.java`（list 含分页/排序/状态筛选, getById, save, update, delete 含关联数据校验）
  - [x] SubTask 4.2: 创建 `MajorService.java` 接口和 `MajorServiceImpl.java`（list 含分页/排序/状态筛选/学院筛选, listByCollegeId, getById, save, update, delete 含关联学生校验）
  - [x] SubTask 4.3: 创建 `MajorRequiredCourseService.java` 接口和实现类（bind 含课程类型/学院校验, unbind, listByMajor）
  - [x] SubTask 4.4: 改造 `StudentServiceImpl.java`（新增/更新时自动分配必修课含异常处理：专业无必修课提示、必修课停用跳过、幂等分配）
  - [x] SubTask 4.5: 改造 `StudentServiceImpl.java`（学生转专业时重新分配必修课，保留历史成绩和考勤记录）
  - [x] SubTask 4.6: 改造 `TeacherServiceImpl.java`（教师调学院时校验课程绑定，已绑定课程禁止调学院）
  - [x] SubTask 4.7: 改造 `CourseServiceImpl.java`（已选课课程禁止修改课程类型，含前置校验逻辑）
  - [x] SubTask 4.8: 关键操作新增日志记录（停用学院、修改专业、绑定/解绑必修课、学生转专业）

- [x] Task 5: 后端 Controller 层 — 学院和专业 REST API
  - [x] SubTask 5.1: 创建 `CollegeController.java`（GET /api/college/list, POST /add, PUT /update, DELETE /delete/{id}，全部 @PreAuthorize("hasRole('admin')")）
  - [x] SubTask 5.2: 创建 `MajorController.java`（GET /api/major/list?collegeId=, POST /add, PUT /update, DELETE /delete/{id}，全部 @PreAuthorize("hasRole('admin')")）
  - [x] SubTask 5.3: 改造 `SelectionServiceImpl.java`（addSelection 方法增加课程类型校验、学院一致性校验、必修课拦截）

- [x] Task 6: 后端 Security 配置 — 权限路由更新
  - [x] SubTask 6.1: 更新 `SecurityConfig.java`（新增 /api/college/** 和 /api/major/** 仅管理员可访问）
  - [x] SubTask 6.2: 改造 `CourseServiceImpl.java` 和 `TeacherServiceImpl.java`（更新操作支持新字段、教师调学院校验课程绑定）

- [x] Task 7: 前端 API 层 — 新增学院和专业请求模块
  - [x] SubTask 7.1: 创建 `frontend/src/api/college.js`（getCollegeList, addCollege, updateCollege, deleteCollege）
  - [x] SubTask 7.2: 创建 `frontend/src/api/major.js`（getMajorList, getMajorsByCollegeId, addMajor, updateMajor, deleteMajor）

- [x] Task 8: 前端侧边栏和路由 — 新增学院专业管理入口
  - [x] SubTask 8.1: 更新 `AdminLayout.vue`（侧边栏「实验室管理」下方新增「学院专业管理」菜单项，带建筑图标 🏛️）
  - [x] SubTask 8.2: 更新 `router/index.js`（新增 /admin/college-major 路由，路由守卫拦截非管理员访问）

- [x] Task 9: 前端学院专业管理页面 — 完整 Vue 组件
  - [x] SubTask 9.1: 创建 `AdminCollegeMajor.vue`（el-tabs 分学院管理/专业管理两个 Tab）
  - [x] SubTask 9.2: 实现学院管理 Tab（列表含分页、按名称搜索、按状态筛选；新增/编辑弹窗含 loading 提交态；删除二次确认含关联数据展示）
  - [x] SubTask 9.3: 实现专业管理 Tab（列表含分页、按名称搜索、按学院筛选、按状态筛选；新增/编辑弹窗含学院选择、loading 提交态；删除二次确认）

- [x] Task 10: 前端弹窗下拉级联改造
  - [x] SubTask 10.1: 改造 `AdminStudent.vue`（学院下拉改为 API 动态加载 + filterable + loading，专业下拉改为学院级联 + filterable + loading，切换学院清空专业，编辑时回显专业）
  - [x] SubTask 10.2: 改造 `AdminTeacher.vue`（学院下拉改为 API 动态加载 + filterable + loading，移除静态 collegeOptions）
  - [x] SubTask 10.3: 改造 `AdminCourse.vue`（学院下拉改为 API 动态加载 + filterable + loading，新增课程类型选择，编辑时已选课课程禁用类型选择器并展示提示）
  - [x] SubTask 10.4: 所有弹窗表单提交按钮新增 loading 禁用态，防止重复提交

- [x] Task 11: BFF 代理映射更新
  - [x] SubTask 11.1: 更新 `proxyMapping.js`（authenticatedPaths 新增 /api/college/、/api/major/、/api/major-required-course/、/api/course-teacher/）

- [ ] Task 12: 测试验证
  - [ ] SubTask 12.1: 更新后端 `CollegeFieldTest.java` 适配外键关联改造，新增边界场景测试用例
  - [ ] SubTask 12.2: 新增测试用例：学院名称重复校验、专业名称同学院重复校验、跨学院绑定必修课拦截、教师重复绑定课程拦截、超长名称校验、空值提交校验
  - [ ] SubTask 12.3: 新增兼容性测试：原字符串 college 字段与新外键 college_id 字段的兼容展示校验
  - [ ] SubTask 12.4: 新增数据迁移校验：存量数据 college 字符串匹配映射 college_id 的正确性验证
  - [ ] SubTask 12.5: 前端功能验证（学院专业管理页面 CRUD、下拉级联含 filterable/loading、权限拦截、表单防重复提交、课程类型保护）

# Task Dependencies
- Task 2 依赖 Task 1（数据库表结构需先就绪）
- Task 3 依赖 Task 2（Entity 定义影响 Repository 查询方法）
- Task 4 依赖 Task 3（Service 依赖 Repository）
- Task 5 依赖 Task 4（Controller 依赖 Service）
- Task 6 可与 Task 5 并行（SecurityConfig 独立于 Controller 实现）
- Task 7 可与 Task 2-6 并行（前端 API 封装不依赖后端完成）
- Task 8 依赖 Task 7（路由配置依赖 API 模块就绪）
- Task 9 依赖 Task 7, Task 8（页面组件依赖 API 和路由）
- Task 10 依赖 Task 7（弹窗改造依赖 API 模块）
- Task 11 可与 Task 7-10 并行
- Task 12 依赖 Task 1-11 全部完成