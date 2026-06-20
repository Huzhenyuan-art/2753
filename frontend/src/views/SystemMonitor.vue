<template>
  <div class="monitor-wrapper">
    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <div class="app-logo">
            <div class="logo-icon"></div>
            <span>用户管理系统</span>
          </div>
        </div>
        <div class="header-right">
          <el-button type="primary" text @click="goHome">
            <el-icon><User /></el-icon>用户管理
          </el-button>
          <el-button type="primary" text @click="goDept">
            <el-icon><OfficeBuilding /></el-icon>部门管理
          </el-button>
          <el-button v-if="canViewAudit" type="primary" text @click="goAuditLog">
            <el-icon><Notebook /></el-icon>操作审计
          </el-button>
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
            <h2 class="page-title">
              <el-icon class="title-icon"><Monitor /></el-icon>
              系统监控
            </h2>
            <p class="page-desc">实时监控服务运行状态、数据库连通性与在线用户概览</p>
          </div>
          <div class="action-section">
            <el-button type="primary" @click="refreshData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>

        <div class="status-overview">
          <div class="status-card glass-panel" :class="systemStatus?.status === 'UP' ? 'status-up' : 'status-degraded'">
            <div class="status-icon-wrapper">
              <el-icon :size="48" class="status-icon">
                <CircleCheckFilled v-if="systemStatus?.status === 'UP'" />
                <WarningFilled v-else />
              </el-icon>
            </div>
            <div class="status-info">
              <div class="status-label">服务状态</div>
              <div class="status-value">{{ systemStatus?.status === 'UP' ? '运行正常' : '服务降级' }}</div>
              <div class="status-sub">{{ systemStatus?.applicationName }} v{{ systemStatus?.version }}</div>
            </div>
          </div>

          <div class="status-card glass-panel" :class="systemStatus?.database?.status === 'UP' ? 'status-up' : 'status-down'">
            <div class="status-icon-wrapper">
              <el-icon :size="48" class="status-icon">
                <CircleCheckFilled v-if="systemStatus?.database?.status === 'UP'" />
                <CircleCloseFilled v-else />
              </el-icon>
            </div>
            <div class="status-info">
              <div class="status-label">数据库状态</div>
              <div class="status-value">{{ systemStatus?.database?.status === 'UP' ? '连接正常' : '连接异常' }}</div>
              <div class="status-sub">
                {{ systemStatus?.database?.type }} · 
                <span v-if="systemStatus?.database?.status === 'UP'">
                  响应 {{ systemStatus?.database?.responseTimeMs }}ms
                </span>
                <span v-else class="error-text">{{ systemStatus?.database?.error }}</span>
              </div>
            </div>
          </div>

          <div class="status-card glass-panel status-info-card">
            <div class="status-icon-wrapper">
              <el-icon :size="48" class="status-icon info"><UserFilled /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-label">在线用户</div>
              <div class="status-value highlight">{{ systemStatus?.onlineUsers?.count || 0 }}</div>
              <div class="status-sub">
                总用户 {{ systemStatus?.onlineUsers?.totalUsers || 0 }} · 
                今日活跃 {{ systemStatus?.onlineUsers?.activeUsersToday || 0 }}
              </div>
            </div>
          </div>
        </div>

        <div class="detail-cards">
          <div class="detail-card glass-panel">
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><Cpu /></el-icon>
                内存使用
              </h3>
            </div>
            <div class="memory-content">
              <div class="memory-stats">
                <div class="memory-stat-item">
                  <div class="stat-label">已使用</div>
                  <div class="stat-value used">{{ systemStatus?.memory?.usedMemoryMB || 0 }} MB</div>
                </div>
                <div class="memory-stat-item">
                  <div class="stat-label">总内存</div>
                  <div class="stat-value">{{ systemStatus?.memory?.totalMemoryMB || 0 }} MB</div>
                </div>
                <div class="memory-stat-item">
                  <div class="stat-label">空闲</div>
                  <div class="stat-value free">{{ systemStatus?.memory?.freeMemoryMB || 0 }} MB</div>
                </div>
              </div>
              <div class="progress-section">
                <div class="progress-label">
                  <span>使用率</span>
                  <span class="progress-value" :class="getUsageClass(systemStatus?.memory?.usagePercent)">
                    {{ systemStatus?.memory?.usagePercent?.toFixed(2) || '0.00' }}%
                  </span>
                </div>
                <el-progress 
                  :percentage="systemStatus?.memory?.usagePercent || 0" 
                  :color="getProgressColor(systemStatus?.memory?.usagePercent)"
                  :stroke-width="12"
                  :show-text="false"
                />
              </div>
            </div>
          </div>

          <div class="detail-card glass-panel">
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><Clock /></el-icon>
                运行信息
              </h3>
            </div>
            <div class="runtime-content">
              <div class="runtime-item">
                <div class="runtime-label">启动时间</div>
                <div class="runtime-value">{{ formatDateTime(systemStatus?.startTime) }}</div>
              </div>
              <div class="runtime-item">
                <div class="runtime-label">运行时长</div>
                <div class="runtime-value">{{ formatUptime(systemStatus?.uptimeSeconds) }}</div>
              </div>
              <div class="runtime-item">
                <div class="runtime-label">数据更新</div>
                <div class="runtime-value">{{ formatDateTime(systemStatus?.timestamp) }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="health-endpoints-card glass-panel">
          <div class="card-header">
            <h3 class="card-title">
              <el-icon><Link /></el-icon>
              健康检查端点
            </h3>
          </div>
          <div class="endpoints-list">
            <div class="endpoint-item">
              <div class="endpoint-info">
                <div class="endpoint-method">GET</div>
                <div class="endpoint-path">/actuator/health</div>
              </div>
              <div class="endpoint-desc">
                <el-tag type="success" size="small">免鉴权</el-tag>
                <span>Docker 探活端点，返回服务基本状态</span>
              </div>
            </div>
            <div class="endpoint-item">
              <div class="endpoint-info">
                <div class="endpoint-method">GET</div>
                <div class="endpoint-path">/api/health</div>
              </div>
              <div class="endpoint-desc">
                <el-tag type="success" size="small">免鉴权</el-tag>
                <span>简单健康检查，返回服务状态与时间戳</span>
              </div>
            </div>
            <div class="endpoint-item">
              <div class="endpoint-info">
                <div class="endpoint-method">GET</div>
                <div class="endpoint-path">/api/health/detail</div>
              </div>
              <div class="endpoint-desc">
                <el-tag type="warning" size="small">需鉴权</el-tag>
                <span>详细系统信息，包含数据库、内存、在线用户等</span>
              </div>
            </div>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  User, OfficeBuilding, Notebook, Lock, SwitchButton, ArrowDown, 
  Monitor, Refresh, CircleCheckFilled, WarningFilled, CircleCloseFilled,
  UserFilled, Cpu, Clock, Link
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const loading = ref(false)
const systemStatus = ref<any>(null)
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

const canViewAudit = computed(() => userStore.hasPermission('audit:list') || userStore.isAdmin)

const displayName = computed(() => {
  return userStore.userInfo?.nickname || userStore.userInfo?.username || userStore.jwtUsername || '用户'
})

const resolveAvatarUrl = (avatar?: string) => {
  if (!avatar) return defaultAvatar
  if (avatar.startsWith('http')) return avatar
  return avatar
}

const getUsageClass = (percent?: number) => {
  if (!percent) return ''
  if (percent >= 80) return 'danger'
  if (percent >= 60) return 'warning'
  return 'success'
}

const getProgressColor = (percent?: number) => {
  if (!percent) return '#10b981'
  if (percent >= 80) return '#ef4444'
  if (percent >= 60) return '#f59e0b'
  return '#10b981'
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatUptime = (seconds?: number) => {
  if (!seconds) return '-'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  const parts: string[] = []
  if (days > 0) parts.push(`${days}天`)
  if (hours > 0) parts.push(`${hours}小时`)
  if (minutes > 0) parts.push(`${minutes}分`)
  parts.push(`${secs}秒`)
  
  return parts.join(' ')
}

const fetchSystemStatus = async () => {
  try {
    const res: any = await request.get('/health/detail', { skipErrorToast: true } as any)
    systemStatus.value = res.data
  } catch (e: any) {
    if (e.type !== 'business' && e.type !== 'http') {
      ElMessage.error('获取系统状态失败')
    }
  }
}

const refreshData = async () => {
  loading.value = true
  try {
    await fetchSystemStatus()
    ElMessage.success('数据已刷新')
  } finally {
    loading.value = false
  }
}

const goHome = () => {
  router.push('/')
}

const goDept = () => {
  router.push('/dept')
}

const goAuditLog = () => {
  router.push('/audit-log')
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

onMounted(async () => {
  await fetchSystemStatus()
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  autoRefreshTimer = setInterval(fetchSystemStatus, 30000)
})

onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
  }
})
</script>

<style scoped>
.monitor-wrapper {
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

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.content-area {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  color: var(--el-color-primary);
}

.page-title {
  font-size: 1.875rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
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

.status-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

.status-card {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.status-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
}

.status-icon-wrapper {
  flex-shrink: 0;
}

.status-icon {
  color: #10b981;
}

.status-card.status-up .status-icon {
  color: #10b981;
}

.status-card.status-down .status-icon {
  color: #ef4444;
}

.status-card.status-degraded .status-icon {
  color: #f59e0b;
}

.status-card.status-info-card .status-icon.info {
  color: #3b82f6;
}

.status-info {
  flex: 1;
  min-width: 0;
}

.status-label {
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 4px;
}

.status-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.status-value.highlight {
  font-size: 2rem;
  color: #3b82f6;
}

.status-sub {
  font-size: 0.875rem;
  color: #94a3b8;
}

.error-text {
  color: #ef4444;
}

.detail-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

.detail-card {
  padding: 24px;
}

.card-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.card-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title .el-icon {
  color: var(--el-color-primary);
}

.memory-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.memory-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.memory-stat-item {
  text-align: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.stat-label {
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
}

.stat-value.used {
  color: #f59e0b;
}

.stat-value.free {
  color: #10b981;
}

.progress-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
  color: #64748b;
}

.progress-value {
  font-weight: 600;
}

.progress-value.success {
  color: #10b981;
}

.progress-value.warning {
  color: #f59e0b;
}

.progress-value.danger {
  color: #ef4444;
}

.runtime-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.runtime-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.runtime-label {
  font-size: 0.875rem;
  color: #64748b;
}

.runtime-value {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #0f172a;
  font-family: 'Monaco', 'Menlo', monospace;
}

.health-endpoints-card {
  padding: 24px;
}

.endpoints-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.endpoint-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  background: #f8fafc;
  border-radius: 12px;
  transition: background 0.2s;
}

.endpoint-item:hover {
  background: #f1f5f9;
}

.endpoint-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 320px;
}

.endpoint-method {
  padding: 4px 12px;
  background: #3b82f6;
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', monospace;
}

.endpoint-path {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 0.9375rem;
  color: #0f172a;
  font-weight: 500;
}

.endpoint-desc {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 0.875rem;
}

@media (max-width: 1024px) {
  .status-overview {
    grid-template-columns: 1fr;
  }
  
  .detail-cards {
    grid-template-columns: 1fr;
  }
  
  .endpoint-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .endpoint-info {
    min-width: auto;
  }
}
</style>
