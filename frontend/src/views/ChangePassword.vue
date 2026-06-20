<template>
  <div class="change-pwd-wrapper">
    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <div class="app-logo" @click="goHome" style="cursor: pointer;">
            <div class="logo-icon"></div>
            <span>用户管理系统</span>
          </div>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-profile">
              <el-avatar :size="32" :src="resolveAvatarUrl(userStore.userInfo?.avatar)" />
              <span class="username">{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item @click="goChangePassword">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content-area">
        <div class="page-header">
          <div class="title-section">
            <h2 class="page-title">修改密码</h2>
            <p class="page-desc">修改您的登录密码，修改成功后需要重新登录</p>
          </div>
          <el-button @click="goProfile" text>
            <el-icon><ArrowLeft /></el-icon>返回个人中心
          </el-button>
        </div>

        <div class="change-pwd-content">
          <div class="form-card glass-panel">
            <el-form
              ref="formRef"
              :model="formData"
              :rules="formRules"
              label-position="top"
              class="change-pwd-form"
              @submit.prevent
            >
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input
                  v-model="formData.oldPassword"
                  type="password"
                  placeholder="请输入旧密码"
                  show-password
                  size="large"
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="formData.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                  size="large"
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
                <div class="password-strength" v-if="formData.newPassword">
                  <div class="strength-bar">
                    <div
                      class="strength-fill"
                      :style="{ width: strengthPercent + '%', background: strengthColor }"
                    ></div>
                  </div>
                  <span class="strength-text" :style="{ color: strengthColor }">{{ strengthLabel }}</span>
                </div>
                <div class="password-rules">
                  <p :class="{ valid: rulesCheck.minLength, invalid: !rulesCheck.minLength }">
                    <el-icon><Check v-if="rulesCheck.minLength" /><Close v-else /></el-icon>
                    至少8个字符
                  </p>
                  <p :class="{ valid: rulesCheck.hasUpper, invalid: !rulesCheck.hasUpper }">
                    <el-icon><Check v-if="rulesCheck.hasUpper" /><Close v-else /></el-icon>
                    包含大写字母
                  </p>
                  <p :class="{ valid: rulesCheck.hasLower, invalid: !rulesCheck.hasLower }">
                    <el-icon><Check v-if="rulesCheck.hasLower" /><Close v-else /></el-icon>
                    包含小写字母
                  </p>
                  <p :class="{ valid: rulesCheck.hasDigit, invalid: !rulesCheck.hasDigit }">
                    <el-icon><Check v-if="rulesCheck.hasDigit" /><Close v-else /></el-icon>
                    包含数字
                  </p>
                  <p :class="{ valid: rulesCheck.hasSpecial, invalid: !rulesCheck.hasSpecial }">
                    <el-icon><Check v-if="rulesCheck.hasSpecial" /><Close v-else /></el-icon>
                    包含特殊字符
                  </p>
                </div>
              </el-form-item>

              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="formData.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                  size="large"
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>

              <div v-if="submitError.show" class="submit-error">
                <el-icon class="error-icon"><Warning /></el-icon>
                <span class="error-text">{{ submitError.message }}</span>
              </div>

              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  class="submit-btn"
                  size="large"
                  @click="handleSubmit"
                >
                  {{ loading ? '提交中...' : '确认修改' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, Check, Close, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const resolveAvatarUrl = (avatar?: string) => {
  if (!avatar) return defaultAvatar
  if (avatar.startsWith('http')) return avatar
  return avatar
}

const displayName = computed(() => {
  if (userStore.userInfo?.nickname) return userStore.userInfo.nickname
  if (userStore.userInfo?.username) return userStore.userInfo.username
  return userStore.jwtUsername || '用户'
})

const formRef = ref()
const loading = ref(false)

const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const submitError = reactive({
  show: false,
  message: ''
})

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{}|;':",./<>?]).{8,20}$/

const validateNewPassword = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 8) {
    callback(new Error('密码长度至少8位'))
  } else if (value.length > 20) {
    callback(new Error('密码长度不能超过20位'))
  } else if (!PASSWORD_REGEX.test(value)) {
    callback(new Error('密码必须包含大小写字母、数字和特殊字符'))
  } else if (value === formData.oldPassword) {
    callback(new Error('新密码不能与旧密码相同'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== formData.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
}

const rulesCheck = computed(() => ({
  minLength: formData.newPassword.length >= 8,
  hasUpper: /[A-Z]/.test(formData.newPassword),
  hasLower: /[a-z]/.test(formData.newPassword),
  hasDigit: /\d/.test(formData.newPassword),
  hasSpecial: /[!@#$%^&*()_+\-=\[\]{}|;':",./<>?]/.test(formData.newPassword)
}))

const strengthPercent = computed(() => {
  const checks = rulesCheck.value
  const passed = [checks.minLength, checks.hasUpper, checks.hasLower, checks.hasDigit, checks.hasSpecial]
    .filter(Boolean).length
  return (passed / 5) * 100
})

const strengthLabel = computed(() => {
  const pct = strengthPercent.value
  if (pct <= 20) return '非常弱'
  if (pct <= 40) return '弱'
  if (pct <= 60) return '一般'
  if (pct <= 80) return '强'
  return '非常强'
})

const strengthColor = computed(() => {
  const pct = strengthPercent.value
  if (pct <= 20) return '#ef4444'
  if (pct <= 40) return '#f97316'
  if (pct <= 60) return '#eab308'
  if (pct <= 80) return '#22c55e'
  return '#16a34a'
})

const handleSubmit = async () => {
  submitError.show = false
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await request.put('/user/change-password', {
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword,
      confirmPassword: formData.confirmPassword
    })
    ElMessageBox.alert(
      '密码修改成功，请使用新密码重新登录。',
      '密码修改成功',
      {
        confirmButtonText: '重新登录',
        type: 'success',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false
      }
    ).then(() => {
      userStore.logout()
      router.push('/login')
    })
  } catch (error: any) {
    if (error.type === 'business') {
      const code = error.code
      switch (code) {
        case 10004:
          submitError.message = '旧密码错误，请重新输入'
          break
        case 10005:
          submitError.message = '新密码不能与旧密码相同'
          break
        case 10006:
          submitError.message = '确认密码与新密码不一致'
          break
        case 404:
          submitError.message = '用户不存在，请重新登录'
          break
        default:
          submitError.message = error.message || '密码修改失败，请重试'
      }
    } else if (error.type === 'timeout') {
      submitError.message = '请求超时，请检查网络后重试'
    } else if (error.type === 'network') {
      submitError.message = '网络连接异常，请检查网络设置'
    } else {
      submitError.message = '密码修改失败，请稍后重试'
    }
    submitError.show = true
  } finally {
    loading.value = false
  }
}

const goHome = () => {
  router.push('/')
}

const goProfile = () => {
  router.push('/profile')
}

const goChangePassword = () => {
  router.push('/change-password')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.change-pwd-wrapper {
  background-color: #f8fafc;
  min-height: 100vh;
}

.header {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px !important;
}

.app-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: var(--primary-gradient);
  border-radius: 8px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-profile:hover {
  background: #f1f5f9;
}

.username {
  font-weight: 500;
  color: #475569;
}

.content-area {
  padding: 40px;
  max-width: 700px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  margin-bottom: 32px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  font-size: 1.875rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.page-desc {
  color: #64748b;
  margin: 0;
}

.glass-panel {
  background: var(--glass-bg);
  backdrop-filter: blur(10px);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
}

.form-card {
  padding: 40px;
}

.change-pwd-form {
  max-width: 480px;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.strength-bar {
  flex: 1;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease, background 0.3s ease;
}

.strength-text {
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}

.password-rules {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
  margin-top: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.password-rules p {
  margin: 0;
  font-size: 0.8rem;
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1.8;
}

.password-rules p.valid {
  color: #16a34a;
}

.password-rules p.invalid {
  color: #94a3b8;
}

.submit-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 20px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
  line-height: 1.5;
}

.submit-error .error-icon {
  flex-shrink: 0;
  font-size: 18px;
}

.submit-error .error-text {
  flex: 1;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 12px;
  background: var(--primary-gradient);
  border: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}
</style>
