<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">实验选课系统</h2>
      <el-form :model="loginForm" class="login-form">
        <el-form-item label="身份">
          <el-select v-model="loginForm.role" placeholder="请选择身份" style="width: 100%">
            <el-option label="学生" value="student" />
            <el-option label="教师" value="teacher" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="loginForm.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>测试账号：</p>
        <p>学生：S001 / 123456</p>
        <p>教师：T001 / 123456</p>
        <p>管理员：admin / 123456</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentLogin } from '../api/student'
import { teacherLogin } from '../api/teacher'
import { adminLogin } from '../api/admin'

const router = useRouter()
const loginForm = reactive({
  role: 'student',
  account: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.account || !loginForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  try {
    let result
    if (loginForm.role === 'student') {
      result = await studentLogin({
        studentNo: loginForm.account,
        password: loginForm.password
      })
      if (result.success) {
        localStorage.setItem('user', JSON.stringify({ ...result.data, role: 'student' }))
        router.push('/student')
      }
    } else if (loginForm.role === 'teacher') {
      result = await teacherLogin({
        teacherNo: loginForm.account,
        password: loginForm.password
      })
      if (result.success) {
        localStorage.setItem('user', JSON.stringify({ ...result.data, role: 'teacher' }))
        router.push('/teacher')
      }
    } else {
      result = await adminLogin({
        username: loginForm.account,
        password: loginForm.password
      })
      if (result.success) {
        localStorage.setItem('user', JSON.stringify({ ...result.data, role: 'admin' }))
        router.push('/admin')
      }
    }

    if (!result.success) {
      ElMessage.error(result.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error('登录失败，请检查网络连接')
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 28px;
}

.login-form {
  margin-top: 20px;
}

.login-tips {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 5px;
  font-size: 12px;
  color: #666;
}

.login-tips p {
  margin: 5px 0;
}
</style>
