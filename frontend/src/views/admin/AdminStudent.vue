<template>
  <div class="page-container">
    <div class="page-header">
      <h2>学生管理</h2>
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
        <el-button type="primary" @click="handleAdd">添加学生</el-button>
      </div>
    </div>

    <div class="table-responsive">
      <el-table :data="studentList" stripe v-loading="loading">
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="major" label="专业" min-width="180" />
        <el-table-column prop="college" label="学院" min-width="180" />
        <el-table-column label="操作" width="260" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" text @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="warning" size="small" text @click="handleResetPassword(scope.row)">重置密码</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false">
      <el-form ref="studentFormRef" :model="studentForm" :rules="studentRules" label-width="72px" status-icon>
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="studentForm.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="studentForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="studentForm.gender" style="width:100%" placeholder="请选择性别">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业" prop="majorId">
          <el-select v-model="studentForm.majorId" style="width:100%" placeholder="请先选择学院" filterable :loading="majorsLoading" :disabled="!studentForm.collegeId">
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学院" prop="collegeId">
          <el-select v-model="studentForm.collegeId" style="width:100%" placeholder="请选择学院" filterable :loading="collegesLoading" @change="onCollegeChange">
            <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <div class="reset-password-info">
        <span class="reset-label">学号：</span>
        <strong>{{ resetTarget.studentNo }}</strong>
      </div>
      <div class="reset-password-info">
        <span class="reset-label">姓名：</span>
        <strong>{{ resetTarget.name }}</strong>
      </div>
      <p style="color: var(--color-text-muted); font-size: 0.88rem; padding: 12px 0;">
        确定要重置该学生的密码为初始密码（123456）吗？
      </p>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePasswordSave">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentList, addStudent, updateStudent, deleteStudent, resetStudentPassword } from '../../api/student'
import { getCollegeList } from '../../api/college'
import { getMajorsByCollegeId } from '../../api/major'

const collegeOptions = ref([])
const majorOptions = ref([])
const collegesLoading = ref(false)
const majorsLoading = ref(false)
const submitting = ref(false)
const filterCollegeId = ref('')
const loading = ref(false)

const studentList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加学生')
const studentFormRef = ref(null)
const studentForm = ref({ id: null, studentNo: '', name: '', gender: '', majorId: null, collegeId: null })

const studentRules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\S+$/, message: '学号不能包含空格', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  majorId: [
    { required: true, message: '请选择专业', trigger: 'change' }
  ],
  collegeId: [
    { required: true, message: '请选择学院', trigger: 'change' }
  ]
}

const passwordDialogVisible = ref(false)
const resetTarget = ref({ id: null, studentNo: '', name: '' })

const loadStudents = async (collegeId) => {
  loading.value = true
  try {
    const result = await getStudentList(collegeId)
    if (result.success) studentList.value = result.data
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
    loadStudents(val || undefined)
  }, 300)
}

const handleAdd = () => {
  dialogTitle.value = '添加学生'
  studentForm.value = { id: null, studentNo: '', name: '', gender: '', majorId: null, collegeId: null }
  majorOptions.value = []
  studentFormRef.value?.resetFields()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑学生'
  studentFormRef.value?.resetFields()
  studentForm.value = { ...row }
  if (row.collegeId) {
    await loadMajorOptions(row.collegeId)
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    await studentFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const isUpdate = !!studentForm.value.id
    const result = isUpdate
      ? await updateStudent(studentForm.value)
      : await addStudent(studentForm.value)
    if (result.success) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadStudents(filterCollegeId.value || undefined)
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) { ElMessage.error('保存失败') }
  finally { submitting.value = false }
}

const handleResetPassword = (row) => {
  resetTarget.value = { id: row.id, studentNo: row.studentNo, name: row.name }
  passwordDialogVisible.value = true
}

const handlePasswordSave = async () => {
  try {
    const result = await resetStudentPassword(resetTarget.value.id)
    if (result.success) {
      ElMessage.success('密码重置成功')
      passwordDialogVisible.value = false
    } else {
      ElMessage.error(result.message || '密码重置失败')
    }
  } catch (error) {
    ElMessage.error('密码重置失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const result = await deleteStudent(id)
    if (result.success) { ElMessage.success('删除成功'); loadStudents(filterCollegeId.value || undefined) }
    else { ElMessage.error(result.message) }
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

const onCollegeChange = (collegeId) => {
  studentForm.value.majorId = null
  majorOptions.value = []
  if (collegeId) {
    loadMajorOptions(collegeId)
  }
}

const loadMajorOptions = async (collegeId) => {
  majorsLoading.value = true
  try {
    const result = await getMajorsByCollegeId(collegeId)
    if (result.success) {
      majorOptions.value = result.data || []
    }
  } catch (error) { /* 静默 */ }
  finally { majorsLoading.value = false }
}

const loadCollegeOptions = async () => {
  collegesLoading.value = true
  try {
    const result = await getCollegeList({ status: 'ACTIVE', size: 999 })
    if (result.success) {
      collegeOptions.value = result.data?.content || result.data || []
    }
  } catch (error) {
    ElMessage.warning('学院列表加载失败')
  } finally { collegesLoading.value = false }
}

onMounted(() => { loadStudents(); loadCollegeOptions() })
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reset-password-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 16px;
  background: var(--color-border-soft);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
}

.reset-label {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

/* 编辑表单统一校验样式 */
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