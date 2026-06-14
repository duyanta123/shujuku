# 全模块新增「学院」字段 Spec

## Why
当前系统中教师、课程、实验室三个模块均缺少「学院」字段，学生模块也缺少该字段。管理员无法按学院维度管理资源，学生和教师端也无法查看学院信息。需要在所有模块统一新增 `college` 字段，字段规范与 `major`（专业）保持一致。

## What Changes
- **数据库**：`student`、`teacher`、`course`、`lab` 四张表新增 `college` 列
- **后端实体**：`Student`、`Teacher`、`Course`、`Lab` 四个 Entity 新增 `college` 属性
- **后端服务**：`StudentService` 的 save/update 方法处理 `college` 字段
- **前端管理端**：管理员端四个管理页面（学生/教师/课程/实验室）的列表和表单新增「学院」列/输入项
- **前端学生端/教师端**：展示学院信息的相关页面同步更新

## Impact
- Affected specs: 无（全新功能）
- Affected code:
  - `database/init_database.sql`
  - `backend/src/main/java/com/labcourse/entity/Student.java`
  - `backend/src/main/java/com/labcourse/entity/Teacher.java`
  - `backend/src/main/java/com/labcourse/entity/Course.java`
  - `backend/src/main/java/com/labcourse/entity/Lab.java`
  - `backend/src/main/java/com/labcourse/service/impl/StudentServiceImpl.java`
  - `frontend/src/views/admin/AdminStudent.vue`
  - `frontend/src/views/admin/AdminTeacher.vue`
  - `frontend/src/views/admin/AdminCourse.vue`
  - `frontend/src/views/admin/AdminLab.vue`

## ADDED Requirements

### Requirement: 数据库表新增 college 列
系统 SHALL 在 `student`、`teacher`、`course`、`lab` 四张表中新增 `college` 列，类型为 `VARCHAR(100)`，注释为「学院」。

#### Scenario: 数据库初始化脚本包含 college 列
- **WHEN** 执行 `init_database.sql` 建表
- **THEN** 四张表均包含 `college VARCHAR(100) COMMENT '学院'` 列

#### Scenario: 已有数据库迁移
- **WHEN** 对已有数据库执行 ALTER TABLE
- **THEN** 四张表均成功新增 `college` 列且不影响现有数据

### Requirement: 后端实体新增 college 属性
系统 SHALL 在 `Student`、`Teacher`、`Course`、`Lab` 四个 JPA Entity 中新增 `college` 字段，映射到数据库 `college` 列。

#### Scenario: 实体包含 college 属性
- **WHEN** 序列化实体为 JSON
- **THEN** 响应中包含 `college` 字段

#### Scenario: 创建/更新时接收 college
- **WHEN** 前端发送包含 `college` 的请求体
- **THEN** 后端正确保存 `college` 到数据库

### Requirement: 管理员端列表和表单新增学院列
系统 SHALL 在管理员端的学生管理、教师管理、课程管理、实验室管理四个页面中，列表表格新增「学院」列，编辑表单新增「学院」输入项。

#### Scenario: 列表显示学院列
- **WHEN** 管理员打开任意管理页面
- **THEN** 表格中显示「学院」列，展示对应数据的学院信息

#### Scenario: 表单包含学院输入
- **WHEN** 管理员点击添加或编辑
- **THEN** 表单中包含「学院」文本输入框，可正常填写和保存

### Requirement: 学生端和教师端展示学院信息
系统 SHALL 在学生端和教师端相关页面中展示学院信息（如选课列表、教师信息等）。

#### Scenario: 学生端查看课程时显示学院
- **WHEN** 学生查看课程列表或课程详情
- **THEN** 显示课程所属学院信息

#### Scenario: 教师端显示学院
- **WHEN** 教师查看个人信息或课程信息
- **THEN** 显示学院信息