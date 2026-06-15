# Tasks

- [x] Task 1: 测试环境搭建 — Playwright 配置与公共工具封装
  - [x] SubTask 1.1: 安装 Playwright 依赖并配置 playwright.config.js（baseURL: http://localhost:3000, screenshot: on-failure, video: retain-on-failure）
  - [x] SubTask 1.2: 创建 `tests/e2e/utils/auth.js` 公共登录方法（adminLogin, studentLogin, teacherLogin, logout）
  - [x] SubTask 1.3: 创建 `tests/e2e/utils/screenshot.js` 截图工具封装（失败自动截图、保存控制台日志、网络请求日志）
  - [x] SubTask 1.4: 创建 `tests/e2e/utils/selectors.js` Element Plus 组件选择器常量（el-dialog, el-form, el-table, el-select, el-input, el-tabs, el-tag, el-button, el-pagination, el-message-box）

- [x] Task 2: 权限与路由测试脚本
  - [x] SubTask 2.1: 创建 `tests/e2e/auth/routing.spec.js`（管理员登录菜单可见 + 页面跳转验证）
  - [x] SubTask 2.2: 学生登录侧边栏无菜单 + URL 访问被拦截跳转
  - [x] SubTask 2.3: 教师登录侧边栏无菜单 + URL 访问被拦截跳转
  - [x] SubTask 2.4: 未登录状态访问管理页面自动跳转登录页

- [x] Task 3: 学院管理 Tab 功能测试脚本
  - [x] SubTask 3.1: 创建 `tests/e2e/college/crud.spec.js`（列表加载、新增、编辑、停用/启用、删除）
  - [x] SubTask 3.2: 表单校验测试（空名称、超长名称、名称重复）
  - [x] SubTask 3.3: 搜索与筛选测试（按名称搜索、按状态筛选）
  - [x] SubTask 3.4: 删除关联校验测试（无关联删除成功、有关联删除失败提示）

- [x] Task 4: 专业管理 Tab 功能测试脚本
  - [x] SubTask 4.1: 创建 `tests/e2e/major/crud.spec.js`（列表加载、新增、编辑、删除）
  - [x] SubTask 4.2: 表单校验测试（空名称、未选学院、超长名称、同院同名、跨院同名）
  - [x] SubTask 4.3: 搜索与筛选测试（按学院筛选、按名称搜索、按状态筛选）
  - [x] SubTask 4.4: 删除关联校验测试（无关联学生删除成功、有关联学生删除失败提示）

- [x] Task 5: 弹窗下拉级联测试脚本
  - [x] SubTask 5.1: 创建 `tests/e2e/dialog/student-cascade.spec.js`（学生弹窗学院下拉API加载、filterable、loading、专业级联、切换学院清空、编辑回显）
  - [x] SubTask 5.2: 创建 `tests/e2e/dialog/teacher-cascade.spec.js`（教师弹窗学院下拉动态加载、必填校验）
  - [x] SubTask 5.3: 创建 `tests/e2e/dialog/course-cascade.spec.js`（课程弹窗学院下拉、课程类型选择、编辑已选课课程类型禁用）

- [x] Task 6: 业务规则校验测试脚本
  - [x] SubTask 6.1: 创建 `tests/e2e/business/required-course.spec.js`（新增学生自动分配必修课、转专业重新分配、必修课手动选课拦截）
  - [x] SubTask 6.2: 创建 `tests/e2e/business/cross-college.spec.js`（跨学院选课拦截、本学院选课正常）
  - [x] SubTask 6.3: 创建 `tests/e2e/business/teacher-binding.spec.js`（教师绑课唯一性、调学院解绑校验、专业绑必修课校验）

- [x] Task 7: 交互体验与边界测试脚本
  - [x] SubTask 7.1: 创建 `tests/e2e/ui/form-submit.spec.js`（表单提交按钮 loading 禁用态、防重复提交）
  - [x] SubTask 7.2: 空数据状态展示测试
  - [x] SubTask 7.3: 页面刷新 Tab 状态测试

- [x] Task 8: 测试报告与执行
  - [x] SubTask 8.1: 配置 Playwright HTML Reporter 输出测试报告
  - [x] SubTask 8.2: 编写 `package.json` 测试执行脚本（test:e2e, test:e2e:headed, test:e2e:college, test:e2e:major 等）
  - [x] SubTask 8.3: 编写测试执行环境说明文档（README 或 comments）

# Task Dependencies
- Task 2-7 均依赖 Task 1（公共工具封装必须先完成）
- Task 2-7 可完全并行执行（各模块测试独立，互不依赖）
- Task 8 依赖 Task 2-7 全部完成（报告汇总所有测试结果）