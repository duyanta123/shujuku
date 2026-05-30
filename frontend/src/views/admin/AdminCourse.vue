<template>
  <div class="page-container">
    <h2>课程管理</h2>
    <el-button type="primary" @click="handleAdd" style="margin-bottom: 20px">添加课程</el-button>

    <el-table :data="courseList" stripe style="width: 100%">
      <el-table-column prop="courseName" label="课程名" width="200" />
      <el-table-column prop="teacherId" label="教师ID" width="100" />
      <el-table-column prop="labId" label="实验室ID" width="120" />
      <el-table-column prop="courseTime" label="上课时间" width="150" />
      <el-table-column prop="maxCount" label="容量" width="100" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="courseForm" label-width="100px">
        <el-form-item label="课程名">
          <el-input v-model="courseForm.courseName" />
        </el-form-item>
        <el-form-item label="教师">
          <el-select v-model="courseForm.teacherId" placeholder="请选择教师">
            <el-option
              v-for="teacher in teacherList"
              :key="teacher.id"
              :label="teacher.name"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实验室">
          <el-select v-model="courseForm.labId" placeholder="请选择实验室">
            <el-option
              v-for="lab in labList"
              :key="lab.id"
              :label="lab.labName"
              :value="lab.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上课时间">
          <el-input v-model="courseForm.courseTime" placeholder="如：周一 1-2节" />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="courseForm.maxCount" :min="1" :max="100" />
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
import { getCourseListSimple, addCourse, updateCourse, deleteCourse } from '../../api/course'
import { getTeacherList } from '../../api/teacher'
import { getLabList } from '../../api/lab'

const courseList = ref([])
const teacherList = ref([])
const labList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加课程')
const courseForm = ref({
  id: null,
  courseName: '',
  teacherId: null,
  labId: null,
  courseTime: '',
  maxCount: 30
})

const loadCourses = async () => {
  try {
    const result = await getCourseListSimple()
    if (result.success) {
      courseList.value = result.data
    }
  } catch (error) {
    ElMessage.error('加载课程列表失败')
  }
}

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

const loadLabs = async () => {
  try {
    const result = await getLabList()
    if (result.success) {
      labList.value = result.data
    }
  } catch (error) {
    ElMessage.error('加载实验室列表失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '添加课程'
  courseForm.value = {
    id: null,
    courseName: '',
    teacherId: null,
    labId: null,
    courseTime: '',
    maxCount: 30
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑课程'
  courseForm.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    let result
    if (courseForm.value.id) {
      result = await updateCourse(courseForm.value)
    } else {
      result = await addCourse(courseForm.value)
    }
    if (result.success) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadCourses()
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
    const result = await deleteCourse(id)
    if (result.success) {
      ElMessage.success('删除成功')
      loadCourses()
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
  loadCourses()
  loadTeachers()
  loadLabs()
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
