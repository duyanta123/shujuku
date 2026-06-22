<template>
  <div class="page-container">
    <div class="page-header">
      <h2>课程管理</h2>
      <div class="header-actions">
        <el-select
          v-model="filterCollegeId"
          placeholder="请选择学院"
          clearable
          filterable
          style="width: 200px"
          @change="onFilterChange"
        >
          <el-option label="全部" value="" />
          <el-option
            v-for="c in collegeOptions"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
        <el-button type="primary" @click="handleAdd">添加课程</el-button>
      </div>
    </div>

    <div class="table-responsive">
      <el-table :data="courseList" stripe v-loading="loading">
        <el-table-column prop="courseName" label="课程名" min-width="150" />
        <el-table-column prop="college" label="学院" min-width="150" />
        <el-table-column prop="courseTime" label="上课时间" width="150" />
        <el-table-column prop="maxCount" label="容量" width="100" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" text @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="88px" status-icon>
        <el-form-item label="课程名" prop="courseName">
          <el-input v-model="courseForm.courseName" placeholder="请输入课程名" />
        </el-form-item>
        <el-form-item label="教师" prop="teacherId">
          <el-select v-model="courseForm.teacherId" placeholder="选择教师" style="width:100%">
            <el-option v-for="t in teacherList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验室" prop="labId">
          <el-select v-model="courseForm.labId" placeholder="选择实验室" style="width:100%">
            <el-option v-for="l in labList" :key="l.id" :label="l.labName" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上课时间" prop="courseTime">
          <el-input v-model="courseForm.courseTime" placeholder="如：周一 1-2节" />
        </el-form-item>
        <el-form-item label="学院" prop="collegeId">
          <el-select v-model="courseForm.collegeId" style="width:100%" placeholder="请选择学院" filterable :loading="collegesLoading">
            <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程类型" prop="courseType">
          <el-select v-model="courseForm.courseType" style="width:100%" placeholder="请选择课程类型" :disabled="courseTypeDisabled">
            <el-option label="必修课" value="REQUIRED" />
            <el-option label="选修课" value="ELECTIVE" />
          </el-select>
          <span v-if="courseTypeDisabled" class="form-hint">该课程已有学生选课，无法修改课程类型</span>
        </el-form-item>
        <el-form-item label="最大人数" prop="maxCount">
          <el-input-number v-model="courseForm.maxCount" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourseListSimple, addCourse, updateCourse, deleteCourse } from '../../api/course'
import { getTeacherList } from '../../api/teacher'
import { getLabList } from '../../api/lab'
import { getCollegeList } from '../../api/college'

const courseList = ref([])
const teacherList = ref([])
const labList = ref([])
const collegeOptions = ref([])
const collegesLoading = ref(false)
const submitting = ref(false)
const courseTypeDisabled = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加课程')
const courseFormRef = ref(null)
const filterCollegeId = ref('')
const loading = ref(false)
const courseForm = ref({ id: null, courseName: '', teacherId: null, labId: null, courseTime: '', collegeId: null, courseType: 'ELECTIVE', maxCount: 30 })

const courseRules = {
  courseName: [
    { required: true, message: '请输入课程名', trigger: 'blur' }
  ],
  teacherId: [
    { required: true, message: '请选择教师', trigger: 'change' }
  ],
  labId: [
    { required: true, message: '请选择实验室', trigger: 'change' }
  ],
  courseTime: [
    { required: true, message: '请输入上课时间', trigger: 'blur' }
  ],
  collegeId: [
    { required: true, message: '请选择学院', trigger: 'change' }
  ],
  courseType: [
    { required: true, message: '请选择课程类型', trigger: 'change' }
  ],
  maxCount: [
    { required: true, message: '请输入最大人数', trigger: 'blur' },
    { type: 'number', min: 1, max: 100, message: '人数范围为1-100', trigger: 'blur' }
  ]
}

const logFormOperation = (action, entity, data) => {
  const timestamp = new Date().toISOString()
  console.log(
    `[表单操作] ${timestamp} | 操作: ${action} | 实体: ${entity} | ` +
    `数据: ${JSON.stringify(data)}`
  )
}

const loadCourses = async (collegeId) => {
  loading.value = true
  try {
    const result = await getCourseListSimple(collegeId)
    if (result.success) courseList.value = result.data
  } catch (error) {
    ElMessage.error('筛选失败，请重试')
  } finally {
    loading.value = false
  }
}

let filterTimer = null
const onFilterChange = (val) => {
  clearTimeout(filterTimer)
  filterTimer = setTimeout(() => {
    loadCourses(val || undefined)
  }, 300)
}

const loadTeachers = async () => {
  try {
    const result = await getTeacherList()
    if (result.success) teacherList.value = result.data
  } catch (error) { ElMessage.error('加载教师列表失败') }
}

const loadLabs = async () => {
  try {
    const result = await getLabList()
    if (result.success) labList.value = result.data
  } catch (error) { ElMessage.error('加载实验室列表失败') }
}

const handleAdd = () => {
  dialogTitle.value = '添加课程'
  courseForm.value = { id: null, courseName: '', teacherId: null, labId: null, courseTime: '', collegeId: null, courseType: 'ELECTIVE', maxCount: 30 }
  courseTypeDisabled.value = false
  courseFormRef.value?.resetFields()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑课程'
  courseForm.value = { ...row }
  courseTypeDisabled.value = false
  courseFormRef.value?.resetFields()
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    await courseFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const isUpdate = !!courseForm.value.id
    const result = isUpdate
      ? await updateCourse(courseForm.value)
      : await addCourse(courseForm.value)
    if (result.success) {
      logFormOperation(isUpdate ? '编辑' : '添加', '课程', {
        id: courseForm.value.id,
        courseName: courseForm.value.courseName,
        teacherId: courseForm.value.teacherId,
        labId: courseForm.value.labId,
        courseTime: courseForm.value.courseTime,
        courseType: courseForm.value.courseType,
        maxCount: courseForm.value.maxCount
      })
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadCourses(filterCollegeId.value || undefined)
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) { ElMessage.error('保存失败') }
  finally { submitting.value = false }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该课程吗？删除后将同时清除该课程的所有选课记录、成绩和考勤数据。',
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const result = await deleteCourse(id)
    if (result.success) {
      ElMessage.success('删除成功')
      loadCourses(filterCollegeId.value || undefined)
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    if (error === 'cancel') return
    // 错误提示已在全局请求拦截器中处理，此处不再重复显示
  }
}

onMounted(() => { loadCourses(); loadTeachers(); loadLabs(); loadCollegeOptions() })

const loadCollegeOptions = async () => {
  collegesLoading.value = true
  try {
    const result = await getCollegeList({ status: 'ACTIVE', size: 999 })
    if (result.success) {
      collegeOptions.value = result.data?.content || result.data || []
    }
  } catch (error) {
    ElMessage.warning('学院列表加载失败')
  } finally {
    collegesLoading.value = false
  }
}
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-hint {
  display: block;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.el-form :deep(.el-form-item__error) {
  font-size: 0.78rem;
  color: #f56c6c;
  line-height: 1.4;
  padding-top: 2px;
}

.el-form :deep(.el-form-item.is-error .el-input__wrapper),
.el-form :deep(.el-form-item.is-error .el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset;
}

.el-form :deep(.el-form-item.is-success .el-input__wrapper),
.el-form :deep(.el-form-item.is-success .el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px #67c23a inset;
}
</style>