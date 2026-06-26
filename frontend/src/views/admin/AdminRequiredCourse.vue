<template>
  <div class="page-container">
    <div class="page-header">
      <h2>必修课配置</h2>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="selectedCollegeId"
        placeholder="选择学院"
        filterable
        clearable
        @change="onCollegeChange"
      >
        <el-option v-for="college in colleges" :key="college.id" :label="college.name" :value="college.id" />
      </el-select>
      <el-select
        v-model="selectedMajorId"
        placeholder="选择专业"
        filterable
        clearable
        :disabled="!selectedCollegeId"
        @change="loadRequiredCourses"
      >
        <el-option v-for="major in majors" :key="major.id" :label="major.name" :value="major.id" />
      </el-select>
      <el-select
        v-model="selectedCourseId"
        placeholder="选择同学院必修课"
        filterable
        clearable
        :disabled="!selectedMajorId"
      >
        <el-option
          v-for="course in availableRequiredCourses"
          :key="course.id"
          :label="course.course_name || course.courseName"
          :value="course.id"
        />
      </el-select>
      <el-button type="primary" :disabled="!selectedMajorId || !selectedCourseId" :loading="submitting" @click="handleBind">
        绑定
      </el-button>
    </div>

    <div v-if="!selectedMajorId" class="empty-state">
      <span class="empty-icon">📌</span>
      <p>请选择学院和专业</p>
    </div>

    <div v-else class="table-responsive">
      <el-table :data="requiredCourses" stripe v-loading="loading">
        <el-table-column prop="course_name" label="课程名" min-width="180">
          <template #default="scope">{{ scope.row.course_name || scope.row.courseName }}</template>
        </el-table-column>
        <el-table-column prop="teacher_name" label="教师" width="140" />
        <el-table-column prop="college" label="学院" min-width="160" />
        <el-table-column prop="course_time" label="上课时间" min-width="160" />
        <el-table-column prop="max_count" label="容量" width="100" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button type="danger" size="small" text @click="handleUnbind(scope.row)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCollegeList } from '../../api/college'
import { getMajorsByCollegeId } from '../../api/major'
import { getCourseListSimple } from '../../api/course'
import { bindRequiredCourse, getRequiredCoursesByMajor, unbindRequiredCourse } from '../../api/majorRequiredCourse'

const colleges = ref([])
const majors = ref([])
const courses = ref([])
const requiredCourses = ref([])
const selectedCollegeId = ref('')
const selectedMajorId = ref('')
const selectedCourseId = ref('')
const loading = ref(false)
const submitting = ref(false)

const availableRequiredCourses = computed(() => {
  const boundIds = new Set(requiredCourses.value.map(item => Number(item.course_id || item.courseId)))
  return courses.value.filter(course => {
    const type = course.course_type || course.courseType
    return type === 'REQUIRED' && !boundIds.has(Number(course.id))
  })
})

async function loadColleges() {
  const result = await getCollegeList({ status: 'ACTIVE', size: 999 })
  if (result.success) colleges.value = result.data?.content || result.data || []
}

async function onCollegeChange() {
  selectedMajorId.value = ''
  selectedCourseId.value = ''
  majors.value = []
  courses.value = []
  requiredCourses.value = []
  if (!selectedCollegeId.value) return
  const [majorResult, courseResult] = await Promise.all([
    getMajorsByCollegeId(selectedCollegeId.value),
    getCourseListSimple(selectedCollegeId.value)
  ])
  if (majorResult.success) majors.value = majorResult.data || []
  if (courseResult.success) courses.value = courseResult.data || []
}

async function loadRequiredCourses() {
  selectedCourseId.value = ''
  requiredCourses.value = []
  if (!selectedMajorId.value) return
  loading.value = true
  try {
    const result = await getRequiredCoursesByMajor(selectedMajorId.value)
    if (result.success) requiredCourses.value = result.data || []
  } finally {
    loading.value = false
  }
}

async function handleBind() {
  submitting.value = true
  try {
    const result = await bindRequiredCourse({
      majorId: selectedMajorId.value,
      courseId: selectedCourseId.value
    })
    if (result.success) {
      ElMessage.success('绑定成功')
      await loadRequiredCourses()
    } else {
      ElMessage.error(result.message || '绑定失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '绑定失败')
  } finally {
    submitting.value = false
  }
}

async function handleUnbind(row) {
  try {
    await ElMessageBox.confirm('确定解除该必修课绑定吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const result = await unbindRequiredCourse(selectedMajorId.value, row.course_id || row.courseId)
    if (result.success) {
      ElMessage.success('解绑成功')
      await loadRequiredCourses()
    } else {
      ElMessage.error(result.message || '解绑失败')
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '解绑失败')
  }
}

onMounted(() => {
  loadColleges().catch(() => ElMessage.error('加载学院失败'))
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
}

.filter-bar .el-select {
  width: 220px;
}
</style>
