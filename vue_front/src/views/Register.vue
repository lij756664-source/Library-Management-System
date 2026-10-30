<template>
  <div class="register-page">
    <!-- 左侧品牌区 60% -->
    <div class="register-brand">
      <div class="brand-content">
        <div class="brand-icon-wrap">
          <svg class="book-icon" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M8 10C8 8.895 8.895 8 10 8H22V40H10C8.895 40 8 39.105 8 38V10Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M22 8H38C39.105 8 40 8.895 40 10V38C40 39.105 39.105 40 38 40H22V8Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M22 8V40" stroke="currentColor" stroke-width="1.8"/>
            <path d="M13 16H19M13 22H19M29 16H35M29 22H35" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.5"/>
          </svg>
        </div>
        <h1 class="brand-title">校园图书借阅<br />自助管理系统</h1>
        <p class="brand-desc">在线查询 · 自助借还 · 逾期提醒 · 数据统计</p>
        <div class="feature-tags">
          <span v-for="tag in tags" :key="tag">{{ tag }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧注册区 40% -->
    <div class="register-panel">
      <div class="register-card">
        <div class="register-header">
          <h2>创建账号</h2>
          <p>注册后即可在线查询与借阅图书</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="register-form"
          @keyup.enter="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名（3-20个字符）"
              size="large"
              :prefix-icon="User"
              class="register-input"
            />
          </el-form-item>
          <el-form-item prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="昵称（选填，默认同用户名）"
              size="large"
              :prefix-icon="Postcard"
              class="register-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码（6-20个字符）"
              size="large"
              show-password
              :prefix-icon="Lock"
              class="register-input"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              show-password
              :prefix-icon="Lock"
              class="register-input"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="register-btn"
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '注 册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-link">
          已有账号？
          <router-link to="/login" class="link">返回登录</router-link>
        </div>
      </div>

      <p class="copyright">Vue 3 + Spring Boot · 前后端分离架构</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { register } from '../api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const tags = ['图书查询', '自助借阅', '续借归还', '逾期提醒']

const form = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3–20 字符', trigger: 'blur' }
  ],
  nickname: [
    { max: 50, message: '昵称不能超过50字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6–20 字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({ username: form.username, nickname: form.nickname, password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { username: form.username } })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  font-family: 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  background: #fafaf8;
}

/* ===== 左侧 60% ===== */
.register-brand {
  flex: 6;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #e3f2fd 0%, #eef6fb 45%, #e8f5e9 100%);
  overflow: hidden;
}

.register-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.04'/%3E%3C/svg%3E");
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  padding: 56px 64px;
  max-width: 520px;
}

.brand-icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 4px 16px rgba(100, 149, 180, 0.12), 0 1px 3px rgba(100, 149, 180, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 36px;
  color: #5b8db8;
}

.book-icon {
  width: 40px;
  height: 40px;
}

.brand-title {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.4;
  color: #3d5a73;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}

.brand-desc {
  font-size: 14px;
  font-weight: 400;
  color: #8fa4b5;
  line-height: 1.8;
  margin-bottom: 28px;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 20px;
}

.feature-tags span {
  font-size: 13px;
  font-weight: 400;
  color: #7a9e8e;
  position: relative;
  padding-left: 12px;
}

.feature-tags span::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #a5c9b8;
}

/* ===== 右侧 40% ===== */
.register-panel {
  flex: 4;
  min-width: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafaf8;
  padding: 40px 32px 24px;
  position: relative;
}

.register-card {
  width: 100%;
  max-width: 360px;
  background: #ffffff;
  border-radius: 16px;
  padding: 40px 36px 32px;
  box-shadow: 0 2px 12px rgba(100, 120, 140, 0.06), 0 8px 32px rgba(100, 120, 140, 0.04);
}

.register-header {
  margin-bottom: 32px;
}

.register-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #3d5a73;
  margin-bottom: 6px;
}

.register-header p {
  font-size: 13px;
  font-weight: 400;
  color: #a8b8c4;
}

/* 表单 & 输入框 */
.register-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.register-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 2px 14px;
  background: #fafafa;
  box-shadow: 0 0 0 1px #dde4ea inset;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.register-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b0c4d4 inset;
  background: #fff;
}

.register-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #90caf9 inset !important;
  background: #fff;
}

.register-input :deep(.el-input__inner) {
  font-weight: 400;
  color: #4a6278;
}

.register-input :deep(.el-input__inner::placeholder) {
  color: #b8c5ce;
}

/* 注册按钮 */
.register-btn {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 3px;
  background: #7eb8da;
  border: none;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.register-btn:hover,
.register-btn:focus {
  background: #6aabce;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(126, 184, 218, 0.3);
}

.register-btn:active {
  transform: scale(0.99);
}

/* 返回登录 */
.login-link {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f2f4;
  text-align: center;
  font-size: 13px;
  font-weight: 400;
  color: #a8b8c4;
}

.link {
  color: #7eb8da;
  text-decoration: none;
  font-weight: 500;
}

.link:hover {
  color: #6aabce;
}

.copyright {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 11px;
  font-weight: 400;
  color: #c5cdd3;
}

/* 响应式 */
@media (max-width: 900px) {
  .register-page {
    flex-direction: column;
  }

  .register-brand {
    flex: none;
    padding: 48px 24px;
  }

  .brand-content {
    padding: 24px;
    text-align: center;
  }

  .brand-icon-wrap {
    margin: 0 auto 24px;
  }

  .brand-title {
    font-size: 28px;
  }

  .feature-tags {
    justify-content: center;
  }

  .register-panel {
    flex: 1;
    min-width: unset;
    width: 100%;
    padding-bottom: 48px;
  }
}
</style>
