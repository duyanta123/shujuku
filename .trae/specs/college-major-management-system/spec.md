# 学院专业管理系统 Spec

## Why
当前系统已有 `college` 字段但仅作为自由文本存储在 student/teacher/course/lab 表中，缺少标准化的学院和专业数据模型。需要新增 `college`(学院) 和 `major`(专业) 两张独立数据表，建立层级关联关系，并基于此实现课程分类(必修/选修)、跨学院选课拦截、必修课自动分配等核心业务规则。

## What Changes
- **数据库**：新增 `college`、`major` 表；新增 `major_required_course`（专业-必修课关联表）、`course_teacher`（课程-教师关联表）；改造 `student` 表新增 `college_id`/`major_id` 外键；改造 `teacher` 表新增 `college_id` 外键；改造 `course` 表新增 `college_id`/`course_type` 字段
- **后端**：新增 College、Major、MajorRequiredCourse、CourseTeacher 四个 Entity 及全套 CRUD 接口；改造 Student/Teacher/Course Entity 的 college 字段从 String 改为外键关联；新增选课校验逻辑（课程类型判断、学院一致性、必修课拦截）；新增必修课自动分配逻辑
- **前端**：侧边栏新增「学院专业管理」菜单（仅管理员可见）；新增学院专业管理页面（el-tabs 分学院/专业两个 Tab）；改造学生/教师/课程弹窗的学院字段从静态下拉改为动态 API 加载 + 级联；新增路由权限守卫
- **BFF**：proxyMapping 新增学院/专业相关接口路径映射

## Impact
- Affected specs: `add-college-field`（MODIFIED — college 从字符串改为外键关联）, `college-input-to-dropdown`（MODIFIED — 下拉数据源从静态数组改为 API 动态加载）
- Affected code:
  - `database/init_database.sql` — 新增表、ALTER 改造现有表
  - `backend/.../entity/College.java`（新增）、`Major.java`（新增）、`MajorRequiredCourse.java`（新增）、`CourseTeacher.java`（新增）
  - `backend/.../entity/Student.java`、`Teacher.java`、`Course.java` — 改造 college 字段
  - `backend/.../repository/` — 新增 4 个 Repository
  - `backend/.../controller/CollegeController.java`（新增）、`MajorController.java`（新增）
  - `backend/.../service/impl/CollegeServiceImpl.java`（新增）、`MajorServiceImpl.java`（新增）
  - `backend/.../service/impl/SelectionServiceImpl.java` — 新增选课校验逻辑
  - `backend/.../config/SecurityConfig.java` — 新增学院/专业接口权限配置
  - `frontend/src/views/admin/AdminLayout.vue` — 侧边栏新增菜单项
  - `frontend/src/views/admin/AdminCollegeMajor.vue`（新增）
  - `frontend/src/views/admin/AdminStudent.vue` — 学院/专业改为级联下拉
  - `frontend/src/views/admin/AdminTeacher.vue` — 学院改为动态下拉
  - `frontend/src/views/admin/AdminCourse.vue` — 新增学院下拉、课程类型选择
  - `frontend/src/router/index.js` — 新增路由配置
  - `frontend/src/api/college.js`（新增）、`major.js`（新增）
  - `bff/src/proxy/proxyMapping.js` — 新增路径映射

## ADDED Requirements

### Requirement: 学院数据模型
系统 SHALL 新增 `college` 表，包含 id、name（唯一）、status（启用/停用）、created_at、updated_at 字段。所有外键引用该表的策略为 ON DELETE RESTRICT + ON UPDATE CASCADE。

#### Scenario: 学院名称全局唯一
- **WHEN** 管理员尝试创建同名学院
- **THEN** 系统返回错误提示「学院名称已存在」

#### Scenario: 学院支持启用/停用
- **WHEN** 管理员停用某学院
- **THEN** 该学院在前端下拉列表中不再显示，但已有数据关联不受影响

#### Scenario: 禁止级联删除业务数据
- **WHEN** 尝试删除被其他表外键引用的学院
- **THEN** 数据库外键约束 ON DELETE RESTRICT 阻止删除，后端捕获异常返回友好提示

### Requirement: 专业数据模型
系统 SHALL 新增 `major` 表，包含 id、name、college_id（外键）、status、created_at、updated_at 字段，专业名称在同一学院内唯一。

#### Scenario: 专业名称同学院内唯一
- **WHEN** 管理员在同一学院下创建同名专业
- **THEN** 系统返回错误提示「该学院下已存在同名专业」

#### Scenario: 不同学院可有同名专业
- **WHEN** 管理员在不同学院下创建同名专业
- **THEN** 系统正常创建成功

### Requirement: 数据库外键与索引规范
系统 SHALL 为所有新增外键列（major.college_id、student.college_id、student.major_id、course.college_id、teacher.college_id）创建普通索引以优化查询性能。所有外键约束统一采用 ON DELETE RESTRICT + ON UPDATE CASCADE 策略，禁止级联删除业务数据。所有 created_at/updated_at 字段明确默认值规则：created_at DEFAULT CURRENT_TIMESTAMP，updated_at DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP。

#### Scenario: 外键更新级联
- **WHEN** 学院的主键 id 发生变更
- **THEN** 所有引用该学院的外键列自动更新为新的 id 值

#### Scenario: 外键删除阻止
- **WHEN** 尝试删除被引用的学院或专业记录
- **THEN** 数据库抛出外键约束异常，后端捕获并返回业务错误提示

#### Scenario: 时间戳自动维护
- **WHEN** 插入或更新任意记录
- **THEN** created_at 在插入时自动填充，updated_at 在每次更新时自动刷新

### Requirement: 存量数据迁移
系统 SHALL 提供 SQL 迁移脚本，将 student/teacher/course 表中现有的 college 字符串字段值，按名称匹配映射到新 college 表的 college_id 外键。匹配失败的行记录在日志中并保留 college_id 为 NULL。

#### Scenario: 名称匹配成功
- **WHEN** student 表中某行的 college 字符串值与 college 表中某行的 name 完全匹配
- **THEN** 该 student 行的 college_id 更新为匹配的 college 表 id

#### Scenario: 名称匹配失败
- **WHEN** student 表中某行的 college 字符串值在 college 表中找不到匹配
- **THEN** 该行的 college_id 保留为 NULL，日志记录「未匹配到学院: {college值}」

### Requirement: 专业-必修课关联
系统 SHALL 新增 `major_required_course` 表，包含 id（主键）、major_id、course_id、created_at 字段，联合唯一约束(major_id, course_id)，仅管理员可配置。

#### Scenario: 管理员配置专业必修课
- **WHEN** 管理员为某专业绑定必修课
- **THEN** 系统校验课程类型为必修课且课程学院与专业学院一致，成功后保存关联

#### Scenario: 重复绑定拦截
- **WHEN** 管理员重复绑定同一专业和课程
- **THEN** 系统返回错误提示「该专业已绑定此必修课」

### Requirement: 课程-教师关联
系统 SHALL 新增 `course_teacher` 表，包含 id（主键）、course_id、teacher_id、created_at 字段，teacher_id 全局唯一（一名教师仅教一门课）。

#### Scenario: 教师授课唯一性校验
- **WHEN** 管理员尝试将已绑定其他课程的教师再绑定到新课程
- **THEN** 系统返回错误提示「该教师已绑定其他课程」

#### Scenario: 教师调学院需先解除课程绑定
- **WHEN** 管理员修改教师学院但该教师已绑定课程
- **THEN** 系统返回错误提示「该教师已绑定课程，请先解除课程绑定」

### Requirement: 学生/教师/课程表改造
系统 SHALL 改造 student 表新增 college_id、major_id 外键；改造 teacher 表新增 college_id 外键；改造 course 表新增 college_id 外键和 course_type 字段（REQUIRED/ELECTIVE）。

#### Scenario: 学生绑定学院和专业
- **WHEN** 管理员创建学生时选择学院和专业
- **THEN** 系统校验专业归属于所选学院，不一致则拒绝

#### Scenario: 课程区分必修/选修
- **WHEN** 管理员创建课程时选择课程类型
- **THEN** 课程保存后在列表中显示对应类型标签

### Requirement: 学院全套 CRUD 接口
系统 SHALL 提供学院列表查询、新增、编辑、删除接口，仅管理员可访问。

#### Scenario: 删除学院前置校验
- **WHEN** 管理员删除学院但该学院下存在关联专业、学生或教师
- **THEN** 系统返回错误提示「该学院下存在 X 个专业、Y 名学生、Z 名教师，无法删除」，并提示关联数据量

#### Scenario: 删除无关联学院
- **WHEN** 管理员删除无关联数据的学院
- **THEN** 系统执行软删除（停用），不物理删除

### Requirement: 专业全套 CRUD 接口
系统 SHALL 提供专业列表查询（支持按学院筛选）、新增、编辑、删除接口，仅管理员可访问。

#### Scenario: 根据学院ID查询专业列表
- **WHEN** 前端请求 `GET /api/major/list?collegeId=X`
- **THEN** 系统返回该学院下所有启用专业的列表

#### Scenario: 删除专业前置校验
- **WHEN** 管理员删除专业但该专业下存在绑定学生
- **THEN** 系统返回错误提示「该专业下存在 X 名学生，无法删除」

### Requirement: 必修课自动分配
系统 SHALL 在学生新增或转专业时，自动根据学生所属专业查询 `major_required_course` 表，将对应必修课通过 `selection` 表分配给学生。

#### Scenario: 学生新增时自动分配必修课
- **WHEN** 管理员新增学生并指定专业
- **THEN** 系统自动将该专业所有必修课分配到该学生的选课记录中

#### Scenario: 学生转专业时重新分配必修课
- **WHEN** 管理员修改学生专业
- **THEN** 系统移除原专业必修课（保留历史成绩和考勤记录），分配新专业必修课

#### Scenario: 必修课分配幂等
- **WHEN** 系统重复执行必修课分配
- **THEN** 已存在的选课记录不重复创建

### Requirement: 选课校验增强
系统 SHALL 在选课接口中增加课程类型判断和学院一致性校验。

#### Scenario: 拦截必修课手动选课
- **WHEN** 学生尝试通过选课接口选择必修课
- **THEN** 系统返回错误提示「必修课由系统自动分配，无法手动选课」

#### Scenario: 拦截跨学院选修课选课
- **WHEN** 学生尝试选择非本学院的选修课
- **THEN** 系统返回错误提示「仅可选择本学院的选修课」

#### Scenario: 允许本学院选修课选课
- **WHEN** 学生选择本学院的选修课
- **THEN** 系统正常处理选课请求

### Requirement: 前端学院专业管理页面
系统 SHALL 在管理员端新增「学院专业管理」页面，使用 el-tabs 分为学院管理和专业管理两个标签页。

#### Scenario: 学院管理 Tab
- **WHEN** 管理员打开学院管理 Tab
- **THEN** 展示学院列表（含名称、状态、创建时间），支持新增、编辑、删除操作

#### Scenario: 专业管理 Tab
- **WHEN** 管理员打开专业管理 Tab
- **THEN** 展示专业列表（含名称、所属学院、状态），支持按学院筛选、新增、编辑、删除操作

#### Scenario: 删除二次确认
- **WHEN** 管理员点击删除学院或专业
- **THEN** 弹窗显示关联数据量，确认后执行删除

### Requirement: 前端下拉级联改造
系统 SHALL 将学生/教师/课程弹窗中的学院字段改为 API 动态加载的下拉选择器，学生弹窗的专业字段改为学院级联选择器。

#### Scenario: 学院下拉动态加载
- **WHEN** 管理员打开学生/教师/课程弹窗
- **THEN** 学院下拉框从 API 加载启用状态的学院列表

#### Scenario: 专业级联过滤
- **WHEN** 管理员在学生弹窗中选择学院
- **THEN** 专业下拉框自动过滤为所选学院下的专业列表，切换学院时清空已选专业

#### Scenario: 编辑场景专业回显
- **WHEN** 管理员编辑已有学生记录时打开弹窗
- **THEN** 学院下拉框回显学生所属学院，专业下拉框自动加载该学院下的专业列表并回显学生当前专业

### Requirement: 前端下拉交互增强
系统 SHALL 为学院和专业下拉框新增 filterable（可搜索过滤）和 loading（加载状态）属性，提升大量数据时的交互体验。

#### Scenario: 学院下拉可搜索
- **WHEN** 管理员在学院下拉框中输入关键字
- **THEN** 下拉列表实时过滤匹配的学院名称

#### Scenario: 下拉加载状态
- **WHEN** 学院或专业下拉框正在从 API 加载数据
- **THEN** 下拉框显示 loading 加载动画

### Requirement: 前端表单提交防重复
系统 SHALL 在所有新增/编辑表单提交时，将提交按钮置为 loading 禁用态，防止用户重复点击提交。

#### Scenario: 提交按钮加载态
- **WHEN** 管理员点击保存按钮提交表单
- **THEN** 按钮变为 loading 状态并禁用，直到请求完成（成功或失败）后恢复

### Requirement: 前端列表搜索与筛选
系统 SHALL 在学院和专业列表页新增按名称搜索、按状态筛选功能，与现有学生管理列表页交互风格保持一致。

#### Scenario: 学院列表按名称搜索
- **WHEN** 管理员在学院列表搜索框中输入学院名称关键字
- **THEN** 列表实时过滤展示匹配的学院

#### Scenario: 专业列表按状态筛选
- **WHEN** 管理员在专业列表中选择状态筛选条件
- **THEN** 列表按所选状态（全部/启用/停用）过滤展示专业

### Requirement: 课程编辑时类型保护
系统 SHALL 在编辑已选课课程时，前置判断是否已有选课记录，有则禁用课程类型选择器并展示提示文案。

#### Scenario: 编辑已选课课程
- **WHEN** 管理员编辑已有学生选课的课程
- **THEN** 课程类型下拉框禁用，下方显示提示「该课程已有学生选课，无法修改课程类型」

### Requirement: 前端路由权限控制
系统 SHALL 在路由守卫中拦截学生和教师角色访问学院专业管理页面，自动跳转首页。

#### Scenario: 学生访问学院专业管理页面
- **WHEN** 学生手动输入 `/admin/college-major` URL
- **THEN** 路由守卫拦截并跳转到学生首页

#### Scenario: 管理员正常访问
- **WHEN** 管理员点击侧边栏「学院专业管理」菜单
- **THEN** 正常进入页面

### Requirement: 课程类型字段
系统 SHALL 在课程新增/编辑弹窗中新增课程类型单选（必修课/选修课），已选课的课程禁止修改课程类型。

#### Scenario: 创建课程时选择类型
- **WHEN** 管理员创建课程选择必修课或选修课
- **THEN** 课程保存后类型字段正确存储

#### Scenario: 已选课课程禁止修改类型
- **WHEN** 管理员尝试修改已有学生选课的课程类型
- **THEN** 系统返回错误提示「该课程已有学生选课，无法修改课程类型」

### Requirement: BFF 代理路径映射
系统 SHALL 在 BFF 层 proxyMapping 中新增学院、专业、必修课配置、教师绑课相关接口路径。

#### Scenario: 学院/专业接口路径认证
- **WHEN** 请求 `/api/college/**` 或 `/api/major/**`
- **THEN** BFF 正确识别为需要认证的路径并进行 JWT 验证和代理转发

#### Scenario: 必修课配置/教师绑课接口路径认证
- **WHEN** 请求 `/api/major-required-course/**` 或 `/api/course-teacher/**`
- **THEN** BFF 正确识别为需要认证的路径并进行 JWT 验证和代理转发

### Requirement: 列表接口分页与筛选
系统 SHALL 为学院和专业列表接口新增分页参数（page、size）、排序参数（sortBy、sortDir）、状态筛选参数（status），默认查询 ACTIVE 状态，管理页可查全部状态。

#### Scenario: 分页查询学院列表
- **WHEN** 前端请求 `GET /api/college/list?page=1&size=10&status=all`
- **THEN** 系统返回分页数据，包含 total、pages、currentPage 等分页元信息

#### Scenario: 按状态筛选专业列表
- **WHEN** 前端请求 `GET /api/major/list?status=INACTIVE`
- **THEN** 系统仅返回已停用状态的专业列表

### Requirement: 实体参数校验
系统 SHALL 对学院和专业实体进行服务端参数校验：名称非空（@NotBlank）、长度限制（@Size(max=100)）、特殊字符过滤（禁止 SQL 注入和 XSS 字符）、status 枚举值合法性校验。

#### Scenario: 名称为空提交被拦截
- **WHEN** 管理员提交空名称的学院或专业
- **THEN** 系统返回 400 错误「名称不能为空」

#### Scenario: 超长名称被拦截
- **WHEN** 管理员提交超过 100 字符的学院名称
- **THEN** 系统返回 400 错误「名称长度不能超过 100 个字符」

#### Scenario: 非法状态值被拦截
- **WHEN** 管理员提交非 ACTIVE/INACTIVE 的 status 值
- **THEN** 系统返回 400 错误「状态值非法」

### Requirement: 必修课自动分配异常处理
系统 SHALL 在必修课自动分配过程中处理异常场景：专业无必修课时正常提示；必修课已停用时跳过分配并记录日志；学生转专业时保留历史成绩和考勤记录。

#### Scenario: 专业无必修课
- **WHEN** 学生新增或转专业到无必修课配置的专业
- **THEN** 系统正常完成操作，不抛出异常，日志记录「专业 {majorName} 无必修课配置」

#### Scenario: 必修课已停用
- **WHEN** 必修课关联的课程状态异常（如已停用）
- **THEN** 系统跳过该必修课分配，日志记录「必修课 {courseName} 状态异常，跳过分配」

#### Scenario: 转专业保留历史数据
- **WHEN** 学生转专业后重新分配必修课
- **THEN** 原专业必修课的选课记录被移除，但该课程的成绩(score)和考勤(attendance)记录保留不变

### Requirement: 关键操作日志记录
系统 SHALL 对关键业务操作记录操作日志（使用 SLF4J Logger）：停用学院、修改专业、绑定/解绑必修课、学生转专业。日志包含操作人、操作时间、操作类型、操作详情。

#### Scenario: 停用学院日志
- **WHEN** 管理员停用学院
- **THEN** 日志记录「管理员 {username} 停用学院 {collegeName}(ID:{collegeId})」

#### Scenario: 学生转专业日志
- **WHEN** 管理员修改学生专业
- **THEN** 日志记录「学生 {studentName}(ID:{studentId}) 专业从 {oldMajor} 变更为 {newMajor}」

## MODIFIED Requirements

### Requirement: 管理员端列表和表单新增学院列（来自 add-college-field）
**变更**：学院字段从自由文本 `el-input`/静态 `el-select` 改为 API 动态加载的 `el-select`，专业字段改为学院级联的 `el-select`。

#### Scenario: 列表显示学院列（不变）
- **WHEN** 管理员打开任意管理页面
- **THEN** 表格中显示「学院」列，展示对应数据的学院名称（通过 join 查询）

#### Scenario: 表单包含学院动态下拉（已修改）
- **WHEN** 管理员点击添加或编辑
- **THEN** 表单中学院下拉框从 API 加载数据，专业下拉框随学院选择联动过滤

### Requirement: 学院下拉选择器数据源（来自 college-input-to-dropdown）
**变更**：数据源从组件内静态数组 `collegeOptions` 改为 API 动态加载 `GET /api/college/list?status=active`。

#### Scenario: 下拉列表从 API 加载
- **WHEN** 用户点击学院输入框
- **THEN** 下拉弹窗展示从后端 API 加载的启用学院列表

## REMOVED Requirements
无。