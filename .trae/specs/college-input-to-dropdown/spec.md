# 学院输入框改造为只读下拉选择器 Spec

## Why
当前管理员端学生管理和教师管理的「学院」字段为自由文本输入框（`el-input`），存在以下问题：
1. 用户可以输入任意文本，可能输入不存在的学院名称，导致数据脏污；
2. 交互方式与同页面其他只读选择组件（如职称 `el-select`）不一致，用户体验割裂。

需要将学院字段改造为只读下拉选择器，UI 和交互逻辑 1:1 复刻现有「职称」下拉组件。

## What Changes
- **AdminStudent.vue**：新增/编辑弹窗中的学院 `el-input` → `el-select`，新增 `college` 校验规则，新增学院选项数据源
- **AdminTeacher.vue**：编辑弹窗中的学院 `el-input` → `el-select`，新增 `college` 校验规则
- **AdminStudent.college.test.js**：更新测试以适配下拉选择器交互

## Impact
- Affected specs: `add-college-field`（MODIFIED — 表单交互方式变更）
- Affected code:
  - `frontend/src/views/admin/AdminStudent.vue`
  - `frontend/src/views/admin/AdminTeacher.vue`
  - `frontend/src/views/admin/AdminStudent.college.test.js`

## ADDED Requirements

### Requirement: 学院下拉选择器数据源
系统 SHALL 在前端定义学院下拉选项列表，作为 `el-select` 的数据源，包含系统预设的全部学院名称。

#### Scenario: 下拉列表包含预设学院
- **WHEN** 用户点击学院输入框
- **THEN** 下拉弹窗展示至少包含「数学与计算机科学学院」在内的系统学院列表

#### Scenario: 数据源在组件内定义
- **WHEN** 开发者查看代码
- **THEN** 学院选项数组定义在组件 `<script setup>` 中，与职称选项 `['教授','副教授','讲师','助教']` 定义位置一致

### Requirement: 学院字段改为只读下拉选择器
系统 SHALL 将学生管理和教师管理弹窗中的学院输入框从 `el-input` 替换为 `el-select`，禁止手动输入。

#### Scenario: 学生管理弹窗学院为下拉选择器
- **WHEN** 管理员打开学生新增或编辑弹窗
- **THEN** 学院字段展示为下拉选择框，点击展开全部可选学院，选择后输入框回填选中名称，不可手动输入

#### Scenario: 教师管理弹窗学院为下拉选择器
- **WHEN** 管理员打开教师编辑弹窗
- **THEN** 学院字段展示为下拉选择框，交互与「职称」下拉完全一致

#### Scenario: 禁用手动输入
- **WHEN** 用户尝试在学院输入框中输入文字或粘贴内容
- **THEN** 输入无效，仅能通过下拉选项选择

### Requirement: 学院字段必填校验
系统 SHALL 将学院字段设为必填，提交前校验未选择时拦截并提示。

#### Scenario: 未选择学院提交被拦截
- **WHEN** 用户在新增/编辑表单中未选择学院即点击保存
- **THEN** 表单校验失败，提示「请选择学院」

#### Scenario: 编辑回显正确选中
- **WHEN** 管理员编辑已有学生/教师记录
- **THEN** 学院下拉框正确回显数据库中的学院值

### Requirement: UI 视觉一致性
学院下拉组件 SHALL 使用 Element Plus `el-select` 默认样式，与职称下拉组件的弹窗圆角、阴影、选中高亮色、上下箭头图标完全一致。

#### Scenario: 样式与职称下拉一致
- **WHEN** 对比学院下拉和职称下拉
- **THEN** 两个下拉组件的视觉呈现无差异（均为 Element Plus 默认主题样式）

## REMOVED Requirements
无。原有增删改查逻辑不受影响。

## MODIFIED Requirements

### Requirement: 管理员端列表和表单新增学院列（来自 add-college-field）
**变更**：表单中学学院字段从 `el-input` 改为 `el-select`，新增必填校验规则。
匹配完整版的规则:

#### Scenario: 列表显示学院列（不变）
- **WHEN** 管理员打开任意管理页面
- **THEN** 表格中显示「学院」列，展示对应数据的学院信息

#### Scenario: 表单包含学院下拉选择器（已修改）
- **WHEN** 管理员点击添加或编辑
- **THEN** 表单中包含「学院」下拉选择框，左侧带红色*必填标识，可正常选择和保存