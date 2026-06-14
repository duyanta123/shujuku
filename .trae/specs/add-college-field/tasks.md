# Tasks

- [x] Task 1: 数据库层 — 四张表新增 college 列
  - [x] 1.1 修改 `database/init_database.sql`：在 `student`、`teacher`、`course`、`lab` 建表语句中新增 `college VARCHAR(100) COMMENT '学院'` 列
  - [x] 1.2 对已有数据库执行 ALTER TABLE 迁移语句，确保现有数据不受影响

- [x] Task 2: 后端实体层 — 四个 Entity 新增 college 属性
  - [x] 2.1 修改 `Student.java`：新增 `college` 字段（`@Column(name = "college", length = 100)`）、getter/setter
  - [x] 2.2 修改 `Teacher.java`：新增 `college` 字段、getter/setter
  - [x] 2.3 修改 `Course.java`：新增 `college` 字段、getter/setter
  - [x] 2.4 修改 `Lab.java`：新增 `college` 字段、getter/setter

- [x] Task 3: 后端服务层 — StudentService 处理 college 字段
  - [x] 3.1 修改 `StudentServiceImpl.java`：save/update 方法中处理 college 字段
  - [x] 3.2 修改 `TeacherServiceImpl.java`：updateById 方法中处理 college 字段
  - [x] 3.3 修改 `CourseServiceImpl.java`：getCourseList SQL 查询包含 college 列

- [x] Task 4: 前端管理端 — 四个管理页面新增学院列和表单输入
  - [x] 4.1 修改 `AdminStudent.vue`：表格新增「学院」列，表单新增「学院」输入框，表单模型和校验规则同步更新
  - [x] 4.2 修改 `AdminTeacher.vue`：表格新增「学院」列，表单新增「学院」输入框，表单模型和校验规则同步更新
  - [x] 4.3 修改 `AdminCourse.vue`：表格新增「学院」列，表单新增「学院」输入框，表单模型和校验规则同步更新
  - [x] 4.4 修改 `AdminLab.vue`：表格新增「学院」列，表单新增「学院」输入框，表单模型和校验规则同步更新

- [x] Task 5: 前端学生端/教师端 — 展示学院信息的页面同步更新
  - [x] 5.1 学生端选课页面 (StudentCourse, StudentMyCourse, StudentSchedule) 显示学院信息
  - [x] 5.2 教师端页面 (TeacherCourse, TeacherStudentList) 显示学院信息

- [x] Task 6: 编写测试验证
  - [x] 6.1 编写 `CollegeFieldTest.java` — 6 个单元测试，验证四个 Entity 的 college 字段
  - [x] 6.2 编写 `AdminStudent.college.test.js` — 3 个组件测试，验证表单 college 字段
  - [x] 6.3 运行全部测试：后端 89 通过 (1 预存失败)，前端 48/48 全部通过

# Task Dependencies
- Task 2 依赖 Task 1（数据库列先建好，实体才能映射）
- Task 3 可与 Task 2 并行（实体新增字段后，服务层同步适配）
- Task 4 依赖 Task 2（前端表单依赖后端 Entity 返回 college 字段）
- Task 5 可与 Task 4 并行
- Task 6 依赖 Task 2、3、4（测试验证已完成的改动）