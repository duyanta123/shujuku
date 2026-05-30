<template>
  <div class="page-container">
    <h2>实验室管理</h2>
    <el-button type="primary" @click="handleAdd" style="margin-bottom: 20px">添加实验室</el-button>

    <el-table :data="labList" stripe style="width: 100%">
      <el-table-column prop="labName" label="实验室名称" width="250" />
      <el-table-column prop="location" label="地点" width="250" />
      <el-table-column prop="capacity" label="容量" width="150" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="labForm" label-width="100px">
        <el-form-item label="实验室名称">
          <el-input v-model="labForm.labName" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="labForm.location" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="labForm.capacity" :min="1" :max="200" />
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
import { getLabList, addLab, updateLab, deleteLab } from '../../api/lab'

const labList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加实验室')
const labForm = ref({
  id: null,
  labName: '',
  location: '',
  capacity: 30
})

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
  dialogTitle.value = '添加实验室'
  labForm.value = {
    id: null,
    labName: '',
    location: '',
    capacity: 30
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑实验室'
  labForm.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    let result
    if (labForm.value.id) {
      result = await updateLab(labForm.value)
    } else {
      result = await addLab(labForm.value)
    }
    if (result.success) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadLabs()
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
    const result = await deleteLab(id)
    if (result.success) {
      ElMessage.success('删除成功')
      loadLabs()
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
