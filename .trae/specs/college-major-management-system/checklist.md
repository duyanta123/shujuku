# Checklist

## 数据库层
- [x] `college` 表创建成功，包含 id, name(UNIQUE), status(ENUM 'ACTIVE','INACTIVE'), created_at DEFAULT CURRENT_TIMESTAMP, updated_at DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- [x] `major` 表创建成功，包含 id, name, college_id(FK ON DELETE RESTRICT ON UPDATE CASCADE), status, UNIQUE(college_id, name), created_at, updated_at
- [x] `major_required_course` 表创建成功，包含 id(PK), major_id(FK), course_id(FK), UNIQUE(major_id, course_id), created_at
- [x] `course_teacher` 表创建成功，包含 id(PK), course_id(FK), teacher_id(FK UNIQUE), created_at
- [x] `student` 表新增 college_id(FK ON DELETE RESTRICT ON UPDATE CASCADE), major_id(FK) 列，保留原 college/major 字符串列
- [x] `teacher` 表新增 college_id(FK ON DELETE RESTRICT ON UPDATE CASCADE) 列，保留原 college 字符串列
- [x] `course` 表新增 college_id(FK ON DELETE RESTRICT ON UPDATE CASCADE), course_type(ENUM 'REQUIRED','ELECTIVE') 列，保留原 college 字符串列
- [x] 所有外键列创建普通索引：major(college_id), student(college_id), student(major_id), course(college_id), teacher(college_id)
- [x] 初始数据正确插入预设学院和专业
- [x] 存量数据迁移 SQL 正确执行：student/teacher/course 的 college 字符串按名称匹配映射为 college_id
- [x] 存量数据迁移 SQL 匹配失败时保留 college_id 为 NULL，有日志记录

## 后端 Entity 层
- [x] College.java 实体类字段完整，含 @PrePersist/@PreUpdate 时间戳，@NotBlank @Size(max=100) 校验
- [x] Major.java 实体类字段完整，collegeId 字段正确，@NotBlank @Size(max=100) @NotNull 校验
- [x] MajorRequiredCourse.java 实体类字段完整，含 id 主键、created_at 时间戳，联合唯一约束
- [x] CourseTeacher.java 实体类字段完整，含 id 主键、created_at 时间戳，teacherId 唯一约束
- [x] Student.java 新增 collegeId、majorId 字段，保留原 college/major 字符串字段
- [x] Teacher.java 新增 collegeId 字段，保留原 college 字符串字段
- [x] Course.java 新增 collegeId、courseType 字段，保留原 college 字符串字段

## 后端 Repository 层
- [x] CollegeRepository 提供 findByStatus 和 findByName 方法
- [x] MajorRepository 提供 findByCollegeId、findByCollegeIdAndName、countByCollegeId 方法
- [x] MajorRequiredCourseRepository 提供 findByMajorId、findByMajorIdAndCourseId、deleteByMajorIdAndCourseId 方法
- [x] CourseTeacherRepository 提供 findByTeacherId、findByCourseId、deleteByTeacherId 方法

## 后端 Service 层
- [x] CollegeService 实现 list(分页/排序/状态筛选)/save/update/delete，delete 含关联数据校验
- [x] MajorService 实现 list(分页/排序/状态筛选/学院筛选)/listByCollegeId/save/update/delete，delete 含关联学生校验
- [x] MajorRequiredCourseService 实现 bind(含课程类型/学院校验)/unbind/listByMajor
- [x] StudentServiceImpl 新增/更新学生时自动分配必修课，幂等不重复
- [x] StudentServiceImpl 必修课自动分配异常处理：专业无必修课正常提示、必修课停用跳过、日志记录
- [x] StudentServiceImpl 学生转专业时重新分配必修课，保留历史成绩考勤
- [x] SelectionServiceImpl 选课校验：必修课拦截、跨学院选修课拦截、学院一致性
- [x] TeacherServiceImpl 教师调学院时校验课程绑定，已绑定课程禁止调学院
- [x] CourseServiceImpl 已选课课程禁止修改课程类型，含前置校验逻辑
- [x] 关键操作日志记录（停用学院、修改专业、绑定/解绑必修课、学生转专业）

## 后端 Controller 层
- [x] CollegeController 提供 GET /list, POST /add, PUT /update, DELETE /delete/{id}，全部 @PreAuthorize
- [x] MajorController 提供 GET /list?collegeId=, POST /add, PUT /update, DELETE /delete/{id}，全部 @PreAuthorize
- [x] 所有接口返回统一格式 { success, message, data }

## 后端 Security 配置
- [x] SecurityConfig 新增 /api/college/** 仅 hasRole('admin') 可访问
- [x] SecurityConfig 新增 /api/major/** 仅 hasRole('admin') 可访问
- [x] 不修改 JwtFilter、JwtUtil、SecurityConfig 公共底层组件

## 前端 API 层
- [x] college.js 提供 getCollegeList/addCollege/updateCollege/deleteCollege 四个方法
- [x] major.js 提供 getMajorList/getMajorsByCollegeId/addMajor/updateMajor/deleteMajor 五个方法

## 前端路由和侧边栏
- [x] AdminLayout.vue 侧边栏「实验室管理」下方新增「学院专业管理」菜单项
- [x] 学院专业管理菜单仅管理员可见（学生/教师不渲染）
- [x] router/index.js 新增 /admin/college-major 路由，指向 AdminCollegeMajor 组件
- [x] 路由守卫拦截学生/教师访问 /admin/college-major，跳转首页

## 前端学院专业管理页面
- [x] AdminCollegeMajor.vue 使用 el-tabs 分为学院管理和专业管理
- [x] 学院管理 Tab：列表含分页、按名称搜索、按状态筛选；支持新增/编辑/删除
- [x] 删除学院弹窗展示关联数据量（专业数、学生数、教师数）
- [x] 专业管理 Tab：列表含分页、按名称搜索、按学院筛选、按状态筛选
- [x] 新增专业弹窗含学院下拉选择（filterable + loading）
- [x] 删除专业弹窗展示关联学生数
- [x] 所有新增/编辑表单提交按钮含 loading 禁用态，防止重复提交
- [x] 页面样式与现有 AdminStudent/AdminTeacher 保持一致

## 前端弹窗级联改造
- [x] AdminStudent.vue 学院下拉改为 API 动态加载（getCollegeList），含 filterable + loading
- [x] AdminStudent.vue 专业下拉改为学院级联（getMajorsByCollegeId），含 filterable + loading，切换学院清空专业
- [x] AdminStudent.vue 编辑场景回显：学院选中后自动加载专业列表并回显当前专业值
- [x] AdminStudent.vue 表单校验规则更新（collegeId/majorId 必填）
- [x] AdminStudent.vue 提交按钮含 loading 禁用态
- [x] AdminTeacher.vue 学院下拉改为 API 动态加载，含 filterable + loading，移除静态 collegeOptions
- [x] AdminTeacher.vue 表单校验规则更新（collegeId 必填）
- [x] AdminTeacher.vue 提交按钮含 loading 禁用态
- [x] AdminCourse.vue 学院下拉改为 API 动态加载，含 filterable + loading
- [x] AdminCourse.vue 新增课程类型下拉选择（必修课/选修课）
- [x] AdminCourse.vue 编辑已选课课程时，课程类型下拉框禁用并展示提示文案
- [x] AdminCourse.vue 提交按钮含 loading 禁用态

## BFF 层
- [x] proxyMapping.js authenticatedPaths 新增 /api/college/
- [x] proxyMapping.js authenticatedPaths 新增 /api/major/
- [x] proxyMapping.js authenticatedPaths 新增 /api/major-required-course/
- [x] proxyMapping.js authenticatedPaths 新增 /api/course-teacher/

## 测试验证
- [x] CollegeFieldTest 适配外键关联改造，全部用例通过
- [x] 新增边界测试：学院名称重复校验
- [x] 新增边界测试：专业名称同学院重复校验
- [x] 新增边界测试：跨学院绑定必修课拦截
- [x] 新增边界测试：教师重复绑定课程拦截
- [x] 新增边界测试：超长名称校验（>100字符）
- [x] 新增边界测试：空值提交校验（名称为空、学院ID为空等）
- [x] 兼容性测试：原字符串 college 字段与新外键 college_id 字段兼容展示
- [x] 数据迁移校验：存量数据 college 字符串匹配映射 college_id 的正确性
- [x] 学院 CRUD 接口功能正常（含分页、排序、状态筛选）
- [x] 专业 CRUD 接口功能正常（含分页、学院筛选、状态筛选）
- [x] 学生弹窗学院-专业级联下拉功能正常（含 filterable、loading、编辑回显）
- [x] 路由权限拦截正常（学生/教师无法访问学院专业管理页面）
- [x] 必修课自动分配功能正常（含异常处理：无必修课、停用、转专业保留历史）
- [x] 跨学院选课拦截功能正常
- [x] 表单防重复提交功能正常（按钮 loading 禁用态）
- [x] 课程编辑类型保护功能正常（已选课课程禁用类型选择器）