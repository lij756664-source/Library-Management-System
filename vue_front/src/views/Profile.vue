<template>
  <div class="profile-page">
    <!-- 头像与账号信息 -->
    <el-card class="page-card user-card" shadow="never">
      <div class="avatar-row">
        <el-avatar :size="80" :src="avatarUrl" class="avatar">
          <el-icon :size="36"><UserFilled /></el-icon>
        </el-avatar>
        <div class="user-meta">
          <div class="user-line">
            <span class="username">{{ profile.username }}</span>
            <el-tag :type="profile.role === 'ADMIN' ? 'warning' : 'success'" size="small">
              {{ profile.role === 'ADMIN' ? '图书管理员' : '普通读者' }}
            </el-tag>
          </div>
          <div class="sub-line">注册于 {{ formatDateTime(profile.createTime) }}</div>
          <el-upload
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
            accept=".jpg,.jpeg,.png,.gif,.webp"
          >
            <el-button size="small" :icon="Upload" :loading="uploading">更换头像</el-button>
          </el-upload>
        </div>
      </div>
    </el-card>

    <div class="profile-grid">
      <!-- 基本资料 -->
      <el-card class="page-card" shadow="never">
        <template #header>基本资料</template>
        <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
          <el-form-item label="用户名">
            <el-input :model-value="profile.username" disabled />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱（选填）" maxlength="100" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号（选填）" maxlength="20" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 修改密码 -->
      <el-card class="page-card" shadow="never">
        <template #header>修改密码</template>
        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-20个字符" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingPassword" @click="handleChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Upload } from '@element-plus/icons-vue'
import { getProfile, updateProfile, changePassword, uploadAvatar } from '../api/user'
import { useAuthStore } from '../store/auth'
import { formatDateTime } from '../utils/date'

const authStore = useAuthStore()

const profile = ref({})
const uploading = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)

const profileForm = reactive({ nickname: '', email: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const avatarUrl = computed(() => (profile.value.avatar ? profile.value.avatar : ''))

const profileRules = {
  nickname: [{ max: 50, message: '昵称不能超过50字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ max: 20, message: '手机号不能超过20字符', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6–20 字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const loadProfile = async () => {
  const res = await getProfile()
  profile.value = res.data
  Object.assign(profileForm, {
    nickname: res.data.nickname || '',
    email: res.data.email || '',
    phone: res.data.phone || ''
  })
}

/** 同步本地登录态（顶部导航的昵称/头像随修改实时更新） */
const syncStoreUser = (patch) => {
  authStore.setAuth(authStore.token, { ...(authStore.user || {}), ...patch })
}

const beforeAvatarUpload = (file) => {
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 jpg/png/gif/webp 格式的图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

const handleAvatarUpload = async ({ file }) => {
  uploading.value = true
  try {
    const res = await uploadAvatar(file)
    profile.value.avatar = res.data.url
    syncStoreUser({ avatar: res.data.url })
    ElMessage.success('头像更新成功')
  } finally {
    uploading.value = false
  }
}

const handleSaveProfile = async () => {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    await updateProfile(profileForm)
    ElMessage.success('资料保存成功')
    profile.value = { ...profile.value, ...profileForm }
    syncStoreUser({ nickname: profileForm.nickname })
  } finally {
    savingProfile.value = false
  }
}

const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingPassword.value = true
  try {
    await changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功，下次登录请使用新密码')
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    passwordFormRef.value.resetFields()
  } finally {
    savingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.user-card {
  margin-bottom: 16px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar {
  background: var(--color-primary-light);
  color: var(--color-primary-hover);
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.sub-line {
  font-size: 13px;
  color: var(--color-text-muted);
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 960px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
