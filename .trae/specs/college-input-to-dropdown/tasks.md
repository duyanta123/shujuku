# Tasks

- [x] Task 1: AdminStudent.vue — 学院 el-input 改为 el-select，新增校验规则和数据源
  - [x] 1.1 在 `<script setup>` 定义 `collegeOptions` 数组（参照 AdminTeacher.vue 职称选项位置），包含 10 个预设学院
  - [x] 1.2 将 `<el-form-item label="学院" prop="college">` 中的 `<el-input>` 替换为 `<el-select>`
  - [x] 1.3 在 `studentRules` 中新增 `college` 校验规则
  - [x] 1.4 在 `AdminStudent.college.test.js` 中新增下拉选择器测试用例（collegeOptions + 必填校验）

- [x] Task 2: AdminTeacher.vue — 学院 el-input 改为 el-select，新增校验规则
  - [x] 2.1 在 `<script setup>` 定义 `collegeOptions` 数组（与 Task 1 数据源一致）
  - [x] 2.2 将 `<el-form-item label="学院" prop="college">` 中的 `<el-input>` 替换为 `<el-select>`
  - [x] 2.3 在 `teacherRules` 中新增 `college` 校验规则

- [x] Task 3: 更新现有测试文件以适配下拉选择器交互
  - [x] 3.1 更新 `AdminStudent.college.test.js`：新增 collegeOptions 数据源验证 + 必填校验规则验证

- [ ] Task 4: 验证 — 运行前端测试无回归，浏览器手动验证 UI 一致性
  - [x] 4.1 运行 `npm test` 确认所有测试通过（50/50）
  - [ ] 4.2 浏览器验证学生管理弹窗学院下拉选择器交互
  - [ ] 4.3 浏览器验证教师管理弹窗学院下拉选择器交互
  - [ ] 4.4 浏览器验证必填校验（未选择学院时提交被拦截）

# Task Dependencies
- Task 1 和 Task 2 可并行执行（无依赖关系）
- Task 3 依赖 Task 1（测试更新需要新的组件结构）
- Task 4 依赖 Task 1、2、3