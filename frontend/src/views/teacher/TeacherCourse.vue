<template>
  <div class="page-container">
    <h2>我的课程</h2>
    <el-table :data="courseList" stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="course_name" label="课程名" width="200" />
      <el-table-column prop="lab_name" label="实验室" width="150" />
      <el-table-column prop="location" label="地点" width="150" />
      <el-table-column prop="course_time" label="上课时间" width="150" />
      <el-table-column prop="selected_count" label="选课人数" width="120" />
      <el-table-column prop="max_count" label="容量" width="100" />
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button type="primary" size="small" @click="viewStudentList(scope.row)">
            查看学生
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCourseListSimple } from '../../api/course'
import { getCourseList } from '../../api/course'

const router = useRouter()
const courseList = ref([])

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

const viewStudentList = (course) => {
  router.push(`/teacher/student-list/${course.id}`)
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
