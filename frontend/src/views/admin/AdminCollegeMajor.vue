<template>
  <div class="page-container">
    <div class="page-header">
      <h2>学院专业管理</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="学院管理" name="college">
        <div class="search-bar">
          <el-input v-model="collegeSearch.name" placeholder="搜索学院名称" clearable style="width:200px" @clear="loadColleges" @keyup.enter="loadColleges" />
          <el-select v-model="collegeSearch.status" placeholder="状态筛选" clearable style="width:140px" @change="loadColleges" @clear="loadColleges">
            <el-option label="全部" value="" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
          <el-button type="primary" @click="loadColleges">搜索</el-button>
          <el-button type="primary" @click="handleAddCollege">添加学院</el-button>
        </div>

        <div class="table-responsive">
          <el-table :data="collegeList" stripe>
            <el-table-column prop="name" label="学院名称" min-width="200" />
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ scope.row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="180" align="center">
              <template #default="scope">
                <el-button type="primary" size="small" text @click="handleEditCollege(scope.row)">编辑</el-button>
                <el-button type="danger" size="small" text @click="handleDeleteCollege(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-pagination
          v-if="collegeTotal > 10"
          v-model:current-page="collegePagination.page"
          :page-size="10"
          :total="collegeTotal"
          layout="total, prev, pager, next"
          @current-change="loadColleges"
          style="margin-top:16px;justify-content:flex-end"
        />
      </el-tab-pane>

      <el-tab-pane label="专业管理" name="major">
        <div class="search-bar">
          <el-input v-model="majorSearch.name" placeholder="搜索专业名称" clearable style="width:200px" @clear="loadMajors" @keyup.enter="loadMajors" />
          <el-select v-model="majorSearch.collegeId" placeholder="所属学院" clearable filterable style="width:200px" @change="loadMajors" @clear="loadMajors">
            <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="majorSearch.status" placeholder="状态筛选" clearable style="width:140px" @change="loadMajors" @clear="loadMajors">
            <el-option label="全部" value="" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
          <el-button type="primary" @click="loadMajors">搜索</el-button>
          <el-button type="primary" @click="handleAddMajor">添加专业</el-button>
        </div>

        <div class="table-responsive">
          <el-table :data="majorList" stripe>
            <el-table-column prop="name" label="专业名称" min-width="200" />
            <el-table-column label="所属学院" min-width="180">
              <template #default="scope">
                {{ getCollegeName(scope.row.collegeId) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ scope.row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center">
              <template #default="scope">
                <el-button type="primary" size="small" text @click="handleEditMajor(scope.row)">编辑</el-button>
                <el-button type="danger" size="small" text @click="handleDeleteMajor(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-pagination
          v-if="majorTotal > 10"
          v-model:current-page="majorPagination.page"
          :page-size="10"
          :total="majorTotal"
          layout="total, prev, pager, next"
          @current-change="loadMajors"
          style="margin-top:16px;justify-content:flex-end"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 学院弹窗 -->
    <el-dialog v-model="collegeDialogVisible" :title="collegeDialogTitle" width="460px" :close-on-click-modal="false">
      <el-form ref="collegeFormRef" :model="collegeForm" :rules="collegeRules" label-width="80px" status-icon>
        <el-form-item label="学院名称" prop="name">
          <el-input v-model="collegeForm.name" placeholder="请输入学院名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="collegeForm.status" style="width:100%" placeholder="请选择状态">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collegeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="collegeSubmitting" @click="handleSaveCollege">保存</el-button>
      </template>
    </el-dialog>

    <!-- 专业弹窗 -->
    <el-dialog v-model="majorDialogVisible" :title="majorDialogTitle" width="460px" :close-on-click-modal="false">
      <el-form ref="majorFormRef" :model="majorForm" :rules="majorRules" label-width="80px" status-icon>
        <el-form-item label="专业名称" prop="name">
          <el-input v-model="majorForm.name" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select v-model="majorForm.collegeId" style="width:100%" placeholder="请选择学院" filterable>
            <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="majorForm.status" style="width:100%" placeholder="请选择状态">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="majorDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="majorSubmitting" @click="handleSaveMajor">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCollegeList, addCollege, updateCollege, deleteCollege } from '../../api/college'
import { getMajorList, addMajor, updateMajor, deleteMajor } from '../../api/major'

const activeTab = ref('college')

// ========== 学院管理 ==========
const collegeList = ref([])
const collegeTotal = ref(0)
const collegePagination = ref({ page: 1 })
const collegeSearch = ref({ name: '', status: '' })
const collegeDialogVisible = ref(false)
const collegeDialogTitle = ref('添加学院')
const collegeFormRef = ref(null)
const collegeForm = ref({ id: null, name: '', status: 'ACTIVE' })
const collegeSubmitting = ref(false)

const collegeRules = {
  name: [
    { required: true, message: '请输入学院名称', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// ========== 专业管理 ==========
const majorList = ref([])
const majorTotal = ref(0)
const majorPagination = ref({ page: 1 })
const majorSearch = ref({ name: '', collegeId: '', status: '' })
const majorDialogVisible = ref(false)
const majorDialogTitle = ref('添加专业')
const majorFormRef = ref(null)
const majorForm = ref({ id: null, name: '', collegeId: null, status: 'ACTIVE' })
const majorSubmitting = ref(false)
const collegeOptions = ref([])

const majorRules = {
  name: [
    { required: true, message: '请输入专业名称', trigger: 'blur' }
  ],
  collegeId: [
    { required: true, message: '请选择学院', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// ========== 学院 CRUD ==========
const loadColleges = async () => {
  try {
    const params = { page: collegePagination.value.page - 1, size: 10 }
    if (collegeSearch.value.name) params.name = collegeSearch.value.name
    if (collegeSearch.value.status) params.status = collegeSearch.value.status
    const result = await getCollegeList(params)
    if (result.success) {
      collegeList.value = result.data?.content || result.data?.records || result.data || []
      collegeTotal.value = result.data?.totalElements || result.data?.total || 0
    }
  } catch (error) { ElMessage.error('加载学院列表失败') }
}

const handleAddCollege = () => {
  collegeDialogTitle.value = '添加学院'
  collegeForm.value = { id: null, name: '', status: 'ACTIVE' }
  collegeFormRef.value?.resetFields()
  collegeDialogVisible.value = true
}

const handleEditCollege = (row) => {
  collegeDialogTitle.value = '编辑学院'
  collegeForm.value = { ...row }
  collegeFormRef.value?.resetFields()
  collegeDialogVisible.value = true
}

const handleSaveCollege = async () => {
  try {
    await collegeFormRef.value.validate()
  } catch {
    return
  }
  collegeSubmitting.value = true
  try {
    const isUpdate = !!collegeForm.value.id
    const result = isUpdate
      ? await updateCollege(collegeForm.value)
      : await addCollege(collegeForm.value)
    if (result.success) {
      ElMessage.success('保存成功')
      collegeDialogVisible.value = false
      loadColleges()
      loadCollegeOptions()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) { ElMessage.error('保存失败') }
  finally { collegeSubmitting.value = false }
}

const handleDeleteCollege = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该学院吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const result = await deleteCollege(id)
    if (result.success) { ElMessage.success('删除成功'); loadColleges(); loadCollegeOptions() }
    else { ElMessage.error(result.message) }
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

// ========== 专业 CRUD ==========
const loadMajors = async () => {
  try {
    const params = { page: majorPagination.value.page - 1, size: 10 }
    if (majorSearch.value.name) params.name = majorSearch.value.name
    if (majorSearch.value.collegeId) params.collegeId = majorSearch.value.collegeId
    if (majorSearch.value.status) params.status = majorSearch.value.status
    const result = await getMajorList(params)
    if (result.success) {
      majorList.value = result.data?.content || result.data?.records || result.data || []
      majorTotal.value = result.data?.totalElements || result.data?.total || 0
    }
  } catch (error) { ElMessage.error('加载专业列表失败') }
}

const handleAddMajor = () => {
  majorDialogTitle.value = '添加专业'
  majorForm.value = { id: null, name: '', collegeId: null, status: 'ACTIVE' }
  majorFormRef.value?.resetFields()
  majorDialogVisible.value = true
}

const handleEditMajor = (row) => {
  majorDialogTitle.value = '编辑专业'
  majorForm.value = { ...row }
  majorFormRef.value?.resetFields()
  majorDialogVisible.value = true
}

const handleSaveMajor = async () => {
  try {
    await majorFormRef.value.validate()
  } catch {
    return
  }
  majorSubmitting.value = true
  try {
    const isUpdate = !!majorForm.value.id
    const result = isUpdate
      ? await updateMajor(majorForm.value)
      : await addMajor(majorForm.value)
    if (result.success) {
      ElMessage.success('保存成功')
      majorDialogVisible.value = false
      loadMajors()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) { ElMessage.error('保存失败') }
  finally { majorSubmitting.value = false }
}

const handleDeleteMajor = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该专业吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const result = await deleteMajor(id)
    if (result.success) { ElMessage.success('删除成功'); loadMajors() }
    else { ElMessage.error(result.message) }
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

const loadCollegeOptions = async () => {
  try {
    const result = await getCollegeList({ status: 'ACTIVE', size: 999 })
    if (result.success) {
      collegeOptions.value = result.data?.content || result.data?.records || result.data || []
    }
  } catch (error) { /* 静默 */ }
}

const getCollegeName = (collegeId) => {
  if (!collegeId) return ''
  const college = collegeOptions.value.find(c => c.id === collegeId)
  return college ? college.name : ''
}

const handleTabChange = (tab) => {
  if (tab === 'college') loadColleges()
  else if (tab === 'major') loadMajors()
}

onMounted(() => {
  loadColleges()
  loadCollegeOptions()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
}

.form-hint {
  display: block;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.el-form :deep(.el-form-item__error) {
  font-size: 0.78rem;
  color: #f56c6c;
  line-height: 1.4;
  padding-top: 2px;
}

.el-form :deep(.el-form-item.is-error .el-input__wrapper),
.el-form :deep(.el-form-item.is-error .el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset;
}

.el-form :deep(.el-form-item.is-success .el-input__wrapper),
.el-form :deep(.el-form-item.is-success .el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px #67c23a inset;
}
</style>