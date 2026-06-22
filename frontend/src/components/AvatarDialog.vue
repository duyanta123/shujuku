<template>
  <el-dialog
    :model-value="modelValue"
    title="个人信息"
    :width="400"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="avatar-dialog">
      <!-- Avatar Section -->
      <div class="avatar-section">
        <div class="avatar-wrapper" :class="{ 'has-image': avatarUrl }">
          <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" class="avatar-img" />
          <img v-else :src="placeholderSvg" alt="placeholder" class="avatar-img" />
          <div class="avatar-overlay" @click="triggerFileInput">
            <span class="overlay-text">更换头像</span>
          </div>
        </div>
        <div class="avatar-btn-row">
          <el-button size="small" plain @click="triggerFileInput" :loading="uploading">
            更换头像
          </el-button>
          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            style="display:none"
            @change="handleFileChange"
          />
        </div>
      </div>

      <!-- Loading -->
      <div v-if="uploading" class="upload-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在上传...</span>
      </div>

      <!-- Info Table -->
      <div class="info-list">
        <div class="info-item">
          <span class="info-label">姓名</span>
          <span class="info-value">{{ userName || '—' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">账号</span>
          <span class="info-value">{{ userAccount || '—' }}</span>
        </div>
        <div v-if="role === 'teacher' || role === 'student'" class="info-item">
          <span class="info-label">学院</span>
          <span class="info-value">{{ college || '—' }}</span>
        </div>
        <div v-if="role === 'teacher'" class="info-item">
          <span class="info-label">职称</span>
          <span class="info-value">{{ title || '—' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">角色</span>
          <span class="info-value">
            <el-tag :type="roleTagType" size="small">{{ roleText }}</el-tag>
          </span>
        </div>
      </div>
    </div>

    <!-- 修改密码子对话框 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="380px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="80px"
        status-icon
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handlePasswordChange">
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <template #footer>
      <el-button type="primary" plain @click="showPasswordDialog = true">修改密码</el-button>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { uploadAvatar, changePassword } from '../api/user'
import { teacherPlaceholder, studentPlaceholder, adminPlaceholder } from '../assets/avatarPlaceholder'
import { validatePassword, validateConfirmPassword, getPasswordValidationError } from '../utils/passwordValidator'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  role: { type: String, default: '' },
  userName: { type: String, default: '' },
  userAccount: { type: String, default: '' },
  avatarUrl: { type: String, default: '' },
  college: { type: String, default: '' },
  title: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'avatar-updated'])

const fileInput = ref(null)
const uploading = ref(false)

// Password change
const showPasswordDialog = ref(false)
const passwordFormRef = ref(null)
const passwordSubmitting = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { validator: validatePassword, trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => {
        validateConfirmPassword(_rule, value, callback, passwordForm.newPassword)
      },
      trigger: ['blur', 'change']
    }
  ]
}

const placeholderSvg = computed(() => {
  const map = { teacher: teacherPlaceholder, student: studentPlaceholder, admin: adminPlaceholder }
  return map[props.role] || studentPlaceholder
})

const roleTagType = computed(() => {
  const map = { teacher: 'primary', student: 'success', admin: 'warning' }
  return map[props.role] || 'info'
})

const roleText = computed(() => {
  const map = { teacher: '教师', student: '学生', admin: '管理员' }
  return map[props.role] || props.role
})

function triggerFileInput() {
  fileInput.value?.click()
}

const cropToSquare = (file) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      const size = Math.min(img.width, img.height)
      const sx = (img.width - size) / 2
      const sy = (img.height - size) / 2
      const canvas = document.createElement('canvas')
      canvas.width = 200
      canvas.height = 200
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, sx, sy, size, size, 0, 0, 200, 200)
      canvas.toBlob((blob) => resolve(blob), 'image/png')
    }
    img.src = URL.createObjectURL(file)
  })
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  // Reset input so same file can be re-selected
  if (fileInput.value) fileInput.value.value = ''

  // Validate file format
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、WebP 格式图片')
    return
  }

  // Validate file size (max 2MB)
  const maxSize = 2 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }

  uploading.value = true
  try {
    const blob = await cropToSquare(file)
    const res = await uploadAvatar(blob)
    const newUrl = res.data?.avatarUrl || res.avatarUrl || ''
    emit('avatar-updated', newUrl)
    ElMessage.success('头像更新成功')
  } catch (err) {
    // Friendly error message
    const msg = err?.response?.data?.message || '头像上传失败，请稍后重试'
    ElMessage.error(msg)
    // Keep dialog open, don't close it
  } finally {
    uploading.value = false
  }
}

async function handlePasswordChange() {
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  passwordSubmitting.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.success) {
      ElMessage.success('密码修改成功')
      showPasswordDialog.value = false
      // Reset form
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      ElMessage.error(res.message || '密码修改失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.message || '密码修改失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    passwordSubmitting.value = false
  }
}
</script>

<style scoped>
.avatar-dialog {
  text-align: center;
  padding: var(--space-sm) 0;
}

/* --- Avatar Section --- */
.avatar-section {
  margin-bottom: var(--space-lg);
}

.avatar-btn-row {
  margin-top: 12px;
}

.avatar-wrapper {
  display: inline-block;
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 3px solid #444f66;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow var(--duration-fast) var(--ease-out);
}

.avatar-wrapper:hover {
  box-shadow: 0 0 12px rgba(68, 79, 102, 0.4);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-out);
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.overlay-text {
  color: #fff;
  font-size: 0.78rem;
  font-weight: 500;
}

/* --- Upload Loading --- */
.upload-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--color-text-soft);
  font-size: 0.85rem;
  margin-bottom: var(--space-md);
}

/* --- Info List --- */
.info-list {
  text-align: left;
  border-top: 1px solid var(--color-border-soft);
  padding-top: var(--space-md);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border-soft);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 0.88rem;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.info-value {
  font-size: 0.88rem;
  color: var(--color-text);
  font-weight: 500;
  text-align: right;
}
</style>