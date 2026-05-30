<template>
  <div class="page-container">
    <h2>我的课程</h2>
    <el-table :data="myCourseList" stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="course_name" label="课程名" width="150" />
      <el-table-column prop="teacher_name" label="教师" width="100" />
      <el-table-column prop="lab_name" label="实验室" width="150" />
      <el-table-column prop="location" label="地点" width="120" />
      <el-table-column prop="course_time" label="上课时间" width="120" />
      <el-table-column prop="select_time" label="选课时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button type="danger" size="small" @click="handleDropCourse(scope.row)">
            退课
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyCourses } from '../../api/selection'
import { deleteSelection } from '../../api/selection'

const myCourseList = ref([])

const user = computed(() => {
  return JSON.parse(localStorage.getItem('user') || '{}')
})

const loadMyCourses = async () => {
  try {
    const result = await getMyCourses(user.value.id)
    if (result.success) {
      myCourseList.value = result.data
    }
  } catch (error) {
    ElMessage.error('加载我的课程失败')
  }
}

const handleDropCourse = async (course) => {
  try {
    await ElMessageBox.confirm('确定要退课吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const result = await deleteSelection(course.selection_id)
    if (result.success) {
      ElMessage.success('退课成功')
      loadMyCourses()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退课失败')
    }
  }
}

onMounted(() => {
  loadMyCourses()
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
