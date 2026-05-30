<template>
  <div class="page-container">
    <h2>课程列表</h2>
    <el-table :data="courseList" stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="course_name" label="课程名" width="150" />
      <el-table-column prop="teacher_name" label="教师" width="100" />
      <el-table-column prop="lab_name" label="实验室" width="150" />
      <el-table-column prop="location" label="地点" width="120" />
      <el-table-column prop="course_time" label="上课时间" width="120" />
      <el-table-column prop="selected_count" label="已选人数" width="100" />
      <el-table-column prop="max_count" label="容量" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.selected_count >= scope.row.max_count ? 'danger' : 'success'">
            {{ scope.row.selected_count >= scope.row.max_count ? '已满' : '可选' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            :disabled="scope.row.selected_count >= scope.row.max_count"
            @click="handleSelectCourse(scope.row)"
          >
            选课
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
import { addSelection } from '../../api/selection'

const courseList = ref([])

const user = computed(() => {
  return JSON.parse(localStorage.getItem('user') || '{}')
})

const loadCourses = async () => {
  try {
    const result = await getCourseList()
    if (result.success) {
      courseList.value = result.data
    }
  } catch (error) {
    ElMessage.error('加载课程列表失败')
  }
}

const handleSelectCourse = async (course) => {
  try {
    const result = await addSelection({
      studentId: user.value.id,
      courseId: course.id
    })
    if (result.success) {
      ElMessage.success('选课成功')
      loadCourses()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('选课失败')
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
