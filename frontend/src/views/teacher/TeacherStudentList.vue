<template>
  <div class="page-container">
    <h2>学生名单</h2>
    <el-button @click="goBack" style="margin-bottom: 20px">返回</el-button>
    <el-table :data="studentList" stripe style="width: 100%">
      <el-table-column prop="student_no" label="学号" width="150" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="major" label="专业" width="200" />
      <el-table-column prop="select_time" label="选课时间" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStudentList } from '../../api/selection'

const route = useRoute()
const router = useRouter()
const studentList = ref([])

const loadStudentList = async () => {
  try {
    const result = await getStudentList(route.params.courseId)
    if (result.success) {
      studentList.value = result.data
    }
  } catch (error) {
    ElMessage.error('加载学生名单失败')
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadStudentList()
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
