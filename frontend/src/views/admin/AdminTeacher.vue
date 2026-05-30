<template>
  <div class="page-container">
    <h2>教师管理</h2>
    <el-button type="primary" @click="handleAdd" style="margin-bottom: 20px">添加教师</el-button>

    <el-table :data="teacherList" stripe style="width: 100%">
      <el-table-column prop="teacherNo" label="工号" width="150" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="title" label="职称" width="150" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="teacherForm" label-width="80px">
        <el-form-item label="工号">
          <el-input v-model="teacherForm.teacherNo" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="teacherForm.name" />
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="teacherForm.title">
            <el-option label="教授" value="教授" />
            <el-option label="副教授" value="副教授" />
            <el-option label="讲师" value="讲师" />
            <el-option label="助教" value="助教" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="teacherForm.password" type="password" />
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
import { getTeacherList, addTeacher, updateTeacher, deleteTeacher } from '../../api/teacher'

const teacherList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加教师')
const teacherForm = ref({
  id: null,
  teacherNo: '',
  name: '',
  title: '',
  password: ''
})

const loadTeachers = async () => {
  try {
    const result = await getTeacherList()
    if (result.success) {
      teacherList.value = result.data
    }
  } catch (error) {
    ElMessage.error('加载教师列表失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '添加教师'
  teacherForm.value = {
    id: null,
    teacherNo: '',
    name: '',
    title: '',
    password: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑教师'
  teacherForm.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    let result
    if (teacherForm.value.id) {
      result = await updateTeacher(teacherForm.value)
    } else {
      result = await addTeacher(teacherForm.value)
    }
    if (result.success) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadTeachers()
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
    const result = await deleteTeacher(id)
    if (result.success) {
      ElMessage.success('删除成功')
      loadTeachers()
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
  loadTeachers()
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
