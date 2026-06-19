<template>
  <div class="profile-wrapper">
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
              <el-avatar :size="32" :src="userStore.userInfo?.avatar || defaultAvatar" />
              <span class="username">{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">
                  <el-icon><User /></el-icon>个人中心
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
            <h2 class="page-title">个人中心</h2>
            <p class="page-desc">查看和管理您的个人资料信息</p>
          </div>
        </div>

        <div v-loading="loading" class="profile-content">
          <div class="profile-card glass-panel">
            <div class="avatar-section">
              <el-avatar :size="120" :src="formData.avatar || defaultAvatar" class="profile-avatar" />
              <h3 class="profile-name">{{ displayName }}</h3>
              <p class="profile-username">@{{ formData.username }}</p>
            </div>

            <el-divider />

            <el-form label-position="top" class="info-form">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="用户名">
                    <el-input :value="formData.username" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="昵称">
                    <el-input :value="formData.nickname" disabled />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="电子邮箱">
                    <el-input :value="formData.email" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="账号状态">
                    <el-tag :type="formData.status === 1 ? 'success' : 'danger'">
                      {{ formData.status === 1 ? '正常' : '禁用' }}
                    </el-tag>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="注册时间">
                    <el-input :value="formatDate(formData.createTime)" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="更新时间">
                    <el-input :value="formatDate(formData.updateTime)" disabled />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const formData = reactive({
  username: '',
  nickname: '',
  email: '',
  avatar: '',
  status: 0,
  createTime: '',
  updateTime: ''
})

const displayName = computed(() => {
  return formData.nickname || formData.username || userStore.jwtUsername || '用户'
})

const syncFormFromStore = () => {
  if (userStore.userInfo) {
    formData.username = userStore.userInfo.username
    formData.nickname = userStore.userInfo.nickname
    formData.email = userStore.userInfo.email
    formData.avatar = userStore.userInfo.avatar
    formData.status = userStore.userInfo.status
    formData.createTime = userStore.userInfo.createTime
    formData.updateTime = userStore.userInfo.updateTime
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const goHome = () => {
  router.push('/')
}

const goProfile = () => {
  router.push('/profile')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const loadUserInfo = async () => {
  loading.value = true
  try {
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
    syncFormFromStore()
  } finally {
    loading.value = false
  }
}

onMounted(loadUserInfo)
</script>

<style scoped>
.profile-wrapper {
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
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  margin-bottom: 32px;
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

.profile-card {
  padding: 40px;
}

.avatar-section {
  text-align: center;
  padding: 20px 0;
}

.profile-avatar {
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.profile-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
  margin: 16px 0 4px 0;
}

.profile-username {
  color: #64748b;
  margin: 0;
  font-size: 0.95rem;
}

.info-form {
  padding-top: 16px;
}
</style>
