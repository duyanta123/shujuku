<template>
  <div class="page-container">
    <h2>考勤录入</h2>
    <el-form :inline="true" style="margin-bottom: 20px">
      <el-form-item label="选择课程">
        <el-select v-model="selectedCourse" placeholder="请选择课程" @change="loadStudents">
          <el-option
            v-for="course in courseList"
            :key="course.id"
            :label="course.course_name"
            :value="course.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table :data="studentList" stripe style="width: 100%">
      <el-table-column prop="student_no" label="学号" width="150" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="major" label="专业" width="200" />
      <el-table-column label="考勤状态" width="150">
        <template #default="scope">
          <el-select v-model="scope.row.status" placeholder="请选择" size="small">
            <el-option label="出勤" value="出勤" />
            <el-option label="请假" value="请假" />
            <el-option label="缺勤" value="缺勤" />
            <el-option label="迟到" value="迟到" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleSaveAttendance(scope.row)">
            保存
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getCourseList } from '../../api/course'
import { getStudentList } from '../../api/selection'
import { addAttendance } from '../../api/attendance'

const courseList = ref([])
const studentList = ref([])
const selectedCourse = ref('')

const user = computed(() => {
  return JSON.parse(localStorage.getItem('user') || '{}')
})

const loadCourses = async () => {
  try {
    const result = await getCourseList()
    if (result.success) {
      courseList.value = result.data.filter(course => course.teacher_name === user.value.name)
    }
  } catch (error) {
    ElMessage.error('加载课程列表失败')
  }
}

const loadStudents = async () => {
  if (!selectedCourse.value) return
  try {
    const result = await getStudentList(selectedCourse.value)
    if (result.success) {
      studentList.value = result.data.map(s => ({ ...s, status: '' }))
    }
  } catch (error) {
    ElMessage.error('加载学生列表失败')
  }
}

const handleSaveAttendance = async (student) => {
  if (!student.status) {
    ElMessage.warning('请选择考勤状态')
    return
  }
  try {
    const result = await addAttendance({
      studentId: student.id || student.student_id,
      courseId: selectedCourse.value,
      status: student.status
    })
    if (result.success) {
      ElMessage.success('考勤录入成功')
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('考勤录入失败')
  }
}

onMounted(() => {
  loadCourses()
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
