<template>
  <div class="layout-container">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>学生选课系统</h3>
        <p>{{ user.name }}</p>
      </div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/student/course">
          <span>课程列表</span>
        </el-menu-item>
        <el-menu-item index="/student/my-course">
          <span>我的课程</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </div>
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const user = computed(() => {
  return JSON.parse(localStorage.getItem('user') || '{}')
})

const handleLogout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  display: flex;
  width: 100%;
  height: 100vh;
}

.sidebar {
  width: 250px;
  background: #304156;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  color: white;
  border-bottom: 1px solid #4a5568;
}

.sidebar-header h3 {
  margin: 0 0 10px 0;
}

.sidebar-header p {
  margin: 0;
  font-size: 14px;
  color: #a0aec0;
}

.sidebar-footer {
  margin-top: auto;
  padding: 20px;
}

.main-content {
  flex: 1;
  padding: 20px;
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
