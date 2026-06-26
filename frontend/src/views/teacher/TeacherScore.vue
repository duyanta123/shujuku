<template>
  <div class="page-container">
    <div class="page-header">
      <h2>成绩录入</h2>
    </div>

    <div class="filter-bar">
      <el-select v-model="selectedCourse" placeholder="选择课程" size="large" @change="loadStudents" clearable>
        <el-option v-for="course in courseList" :key="course.id" :label="course.course_name" :value="course.id" />
      </el-select>
    </div>

    <div v-if="!selectedCourse" class="empty-state">
      <span class="empty-icon">📝</span>
      <p>请先选择要录入成绩的课程</p>
    </div>

    <template v-else>
      <div v-if="studentList.length === 0" class="empty-state">
        <span class="empty-icon">🎓</span>
        <p>该课程暂无学生</p>
      </div>

      <el-table v-else :data="studentList" stripe>
        <el-table-column prop="student_no" label="学号" width="150" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="major" label="专业" min-width="180" />
        <el-table-column prop="college" label="学院" min-width="180" />
        <el-table-column label="成绩" width="180">
          <template #default="scope">
            <el-input-number v-model="scope.row.score" :min="0" :max="100" :precision="1" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleSaveScore(scope.row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyTeachingCourses } from '../../api/course'
import { addScore, getScoresByCourse } from '../../api/score'

const courseList = ref([])
const studentList = ref([])
const selectedCourse = ref('')

const loadCourses = async () => {
  try {
    const result = await getMyTeachingCourses()
    if (result.success) {
      courseList.value = result.data || []
    }
  } catch (error) {
    ElMessage.error('加载课程列表失败')
  }
}

const loadStudents = async () => {
  if (!selectedCourse.value) { studentList.value = []; return }
  try {
    const result = await getScoresByCourse(selectedCourse.value)
    if (result.success) {
      studentList.value = (result.data || []).map(s => ({
        ...s,
        student_no: s.student_no || s.studentNo,
        score: s.score === undefined ? null : s.score
      }))
    }
  } catch (error) {
    ElMessage.error('加载学生列表失败')
  }
}

const handleSaveScore = async (student) => {
  if (student.score === null || student.score === undefined) {
    ElMessage.warning('请输入成绩')
    return
  }
  try {
    const result = await addScore({
      studentId: student.id || student.student_id,
      courseId: selectedCourse.value,
      score: student.score
    })
    if (result.success) {
      ElMessage.success('成绩录入成功')
      student.score = Number(student.score)
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('成绩录入失败')
  }
}

onMounted(() => { loadCourses() })
</script>

<style scoped>
.filter-bar {
  margin-bottom: var(--space-lg);
}
.filter-bar .el-select { width: 280px; }
</style>
