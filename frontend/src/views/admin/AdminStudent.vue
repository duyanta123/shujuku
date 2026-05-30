<template>
  <div class="page-container">
    <h2>学生管理</h2>
    <el-button type="primary" @click="handleAdd" style="margin-bottom: 20px">添加学生</el-button>

    <el-table :data="studentList" stripe style="width: 100%">
      <el-table-column prop="studentNo" label="学号" width="150" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="major" label="专业" width="200" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="studentForm" label-width="80px">
        <el-form-item label="学号">
          <el-input v-model="studentForm.studentNo" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="studentForm.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="studentForm.gender">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="studentForm.major" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="studentForm.password" type="password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentList, addStudent, updateStudent, deleteStudent } from '../../api/student'

const studentList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加学生')
const studentForm = ref({
  id: null,
  studentNo: '',
  name: '',
  gender: '',
  major: '',
  password: ''
})

const loadStudents = async () => {
  try {
    const result = await getStudentList()
    if (result.success) {
      studentList.value = result.data
    }
  } catch (error) {
    ElMessage.error('加载学生列表失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '添加学生'
  studentForm.value = {
    id: null,
    studentNo: '',
    name: '',
    gender: '',
    major: '',
    password: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑学生'
  studentForm.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    let result
    if (studentForm.value.id) {
      result = await updateStudent(studentForm.value)
    } else {
      result = await addStudent(studentForm.value)
    }
    if (result.success) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadStudents()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const result = await deleteStudent(id)
    if (result.success) {
      ElMessage.success('删除成功')
      loadStudents()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadStudents()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.page-container h2 {
  margin: 0 0 20px 0;
  color: #333;
}
</style>
