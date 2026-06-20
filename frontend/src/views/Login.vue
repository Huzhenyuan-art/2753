<template>
  <div class="login-container">
    <div class="login-left">
      <div class="visual-content">
        <div class="glass-orb"></div>
        <div class="branding">
          <h1 class="animate-up">用户管理系统</h1>
          <p class="animate-up delay-1">基于现代技术的专业管理解决方案</p>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-box animate-up delay-2">
        <h2 class="login-title">欢迎回来</h2>
        <p class="login-subtitle">请登录您的账号以继续</p>
        
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleLogin">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <div class="options">
            <el-checkbox v-model="rememberMe">记住用户名</el-checkbox>
          </div>
          <div v-if="loginError.show" class="login-error">
            <el-icon class="error-icon"><Warning /></el-icon>
            <span class="error-text">{{ loginError.message }}</span>
          </div>
          <el-button type="primary" :loading="loading" class="login-btn" size="large" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loginForm = ref({ username: '', password: '' })
const rememberMe = ref(false)
const loading = ref(false)
const loginFormRef = ref()

const loginError = reactive({
  show: false,
  message: '',
  type: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

watch(
  () => loginForm.value.username,
  () => {
    if (loginError.show) {
      loginError.show = false
    }
  }
)

watch(
  () => loginForm.value.password,
  () => {
    if (loginError.show) {
      loginError.show = false
    }
  }
)

onMounted(() => {
  const savedUsername = localStorage.getItem('saved_username')
  if (savedUsername) {
    loginForm.value.username = savedUsername
    rememberMe.value = true
  }
})

const setLoginError = (type: string, message: string) => {
  loginError.type = type
  loginError.message = message
  loginError.show = true
}

const handleLoginError = (error: any) => {
  if (error.type === 'business') {
    const code = error.code
    switch (code) {
      case 10001:
        setLoginError('user_not_found', '用户名不存在，请检查后重试')
        break
      case 10002:
        setLoginError('password_error', '密码错误，请重新输入')
        break
      case 10003:
        setLoginError('account_disabled', '账号已被禁用，请联系管理员')
        break
      default:
        setLoginError('unknown', error.message || '登录失败，请重试')
    }
  } else if (error.type === 'timeout') {
    setLoginError('timeout', '请求超时，请检查网络连接后重试')
  } else if (error.type === 'network') {
    setLoginError('network', '网络连接异常，请检查网络设置')
  } else if (error.type === 'http') {
    const status = error.status
    if (status === 500) {
      setLoginError('server_error', '服务器内部错误，请稍后重试')
    } else if (status === 502 || status === 503) {
      setLoginError('service_unavailable', '服务暂时不可用，请稍后重试')
    } else if (status === 404) {
      setLoginError('not_found', '请求的服务不存在')
    } else {
      setLoginError('http_error', `请求失败 (${status})，请稍后重试`)
    }
  } else {
    setLoginError('unknown', '登录失败，请稍后重试')
  }
}

const handleLogin = async () => {
  await loginFormRef.value.validate()
  loading.value = true
  loginError.show = false
  try {
    const res: any = await request.post('/user/login', loginForm.value, { skipErrorToast: true } as any)
    userStore.setLoginData(res.data)
    if (res.data && res.data.status === 0) {
      userStore.logout()
      setLoginError('account_disabled', '账号已被禁用，请联系管理员')
      return
    }
    if (rememberMe.value) {
      localStorage.setItem('saved_username', loginForm.value.username)
    } else {
      localStorage.removeItem('saved_username')
    }
    localStorage.removeItem('saved_login')
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error: any) {
    console.error(error)
    handleLoginError(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  overflow: hidden;
  background-color: #fff;
}

.login-left {
  flex: 1.4;
  background: var(--primary-gradient);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.visual-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: white;
}

.glass-orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(40px);
  z-index: -1;
}

.branding h1 {
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 1.5rem;
  letter-spacing: -1px;
}

.branding p {
  font-size: 1.25rem;
  opacity: 0.9;
  max-width: 400px;
  margin: 0 auto;
  line-height: 1.6;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background-color: #fff;
}

.login-box {
  width: 100%;
  max-width: 420px;
}

.login-title {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: #0f172a;
}

.login-subtitle {
  color: #64748b;
  margin-bottom: 2.5rem;
  font-size: 1rem;
}

.options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.login-btn {
  width: 100%;
  height: 52px;
  font-size: 1.1rem;
  font-weight: 600;
  border-radius: 12px;
  background: var(--primary-gradient);
  border: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-error {
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

.login-error .error-icon {
  flex-shrink: 0;
  font-size: 18px;
}

.login-error .error-text {
  flex: 1;
}

/* Animations */
.animate-up {
  animation: slideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.delay-1 { animation-delay: 0.1s; }
.delay-2 { animation-delay: 0.2s; }

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .login-left {
    display: none;
  }
}
</style>
