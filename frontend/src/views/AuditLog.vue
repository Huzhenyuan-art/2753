<template>
  <div class="dashboard-wrapper">
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
            <h2 class="page-title">操作审计日志</h2>
            <p class="page-desc">记录和追溯系统中的所有关键操作行为</p>
          </div>
        </div>

        <div class="filters-card glass-panel">
          <el-input
            v-model="usernameFilter"
            placeholder="操作人用户名/昵称"
            clearable
            @clear="fetchData"
            class="filter-item"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
            v-model="operationFilter"
            placeholder="操作类型"
            clearable
            @clear="fetchData"
            class="filter-item"
          >
            <el-option label="登录" value="LOGIN" />
            <el-option label="新增" value="CREATE" />
            <el-option label="编辑" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="状态变更" value="STATUS" />
            <el-option label="分配角色" value="ASSIGN_ROLE" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="filter-item date-range"
          />
          <el-button @click="fetchData" type="primary" class="search-btn">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="resetFilters" class="reset-btn">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </div>

        <div class="table-container glass-panel">
          <el-table 
            :data="logList" 
            v-loading="loading" 
            style="width: 100%" 
            class="custom-table"
            :header-cell-style="{ background: '#f8fafc', color: '#64748b', fontWeight: 'bold' }"
          >
            <el-table-column label="序号" width="70" align="center">
              <template #default="{ $index }">
                {{ (pageNum - 1) * pageSize + $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column label="操作人" min-width="140">
              <template #default="{ row }">
                <div class="operator-info">
                  <span class="operator-name">{{ row.nickname || row.username || '-' }}</span>
                  <span v-if="row.username" class="operator-username">({{ row.username }})</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getOperationTagType(row.operation)" effect="light" round>
                  {{ getOperationText(row.operation) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="module" label="所属模块" width="120" />
            <el-table-column prop="description" label="操作描述" min-width="150" />
            <el-table-column prop="ip" label="IP地址" width="140" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" round size="small">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="100" align="center">
              <template #default="{ row }">
                {{ row.costTime != null ? row.costTime + 'ms' : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作时间" width="180">
              <template #default="{ row }">
                <span class="time-stamp">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-footer">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              @current-change="fetchData"
              @size-change="fetchData"
            />
          </div>
        </div>
      </el-main>
    </el-container>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="600px" class="custom-dialog">
      <el-descriptions :column="1" border v-if="currentLog">
        <el-descriptions-item label="操作人">
          {{ currentLog.nickname || currentLog.username || '-' }}
          <span v-if="currentLog.username" class="detail-sub">({{ currentLog.username }})</span>
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="getOperationTagType(currentLog.operation)" effect="light" round>
            {{ getOperationText(currentLog.operation) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属模块">{{ currentLog.module || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ currentLog.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">
          <span class="method-text">{{ currentLog.method || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" effect="light" round>
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.costTime != null ? currentLog.costTime + 'ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDate(currentLog.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" v-if="currentLog.params">
          <pre class="param-pre">{{ currentLog.params }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" v-if="currentLog.result">
          <pre class="param-pre">{{ currentLog.result }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentLog.errorMsg">
          <span class="error-text">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, SwitchButton, Search, Refresh, ArrowDown } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const resolveAvatarUrl = (avatar?: string) => {
  if (!avatar) return defaultAvatar
  if (avatar.startsWith('http')) return avatar
  return avatar
}

const displayName = computed(() => {
  return userStore.userInfo?.nickname || userStore.userInfo?.username || userStore.jwtUsername || '用户'
})

interface AuditLogItem {
  id: number
  userId: number
  username: string
  nickname: string
  operation: string
  module: string
  description: string
  method: string
  params: string
  result: string
  ip: string
  status: number
  errorMsg: string
  costTime: number
  createTime: string
}

const logList = ref<AuditLogItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const usernameFilter = ref('')
const operationFilter = ref('')
const dateRange = ref<string[]>([])
const loading = ref(false)

const detailVisible = ref(false)
const currentLog = ref<AuditLogItem | null>(null)

const getOperationText = (op: string) => {
  const map: Record<string, string> = {
    LOGIN: '登录',
    CREATE: '新增',
    UPDATE: '编辑',
    DELETE: '删除',
    STATUS: '状态变更',
    ASSIGN_ROLE: '分配角色'
  }
  return map[op] || op
}

const getOperationTagType = (op: string) => {
  const map: Record<string, string> = {
    LOGIN: 'primary',
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    STATUS: 'info',
    ASSIGN_ROLE: ''
  }
  return map[op] || ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (usernameFilter.value.trim()) {
      params.username = usernameFilter.value.trim()
    }
    if (operationFilter.value) {
      params.operation = operationFilter.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const res: any = await request.get('/audit-log/list', { params })
    logList.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  usernameFilter.value = ''
  operationFilter.value = ''
  dateRange.value = []
  pageNum.value = 1
  fetchData()
}

const showDetail = (row: AuditLogItem) => {
  currentLog.value = row
  detailVisible.value = true
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

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (!userStore.hasPermission('audit:list') && !userStore.isAdmin) {
    ElMessage.warning('您没有查看审计日志的权限')
    router.push('/')
    return
  }
  fetchData()
})
</script>

<style scoped>
.dashboard-wrapper {
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
  max-width: 1400px;
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

.filters-card {
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.filter-item {
  max-width: 240px;
}

.date-range {
  width: 360px;
  max-width: 100%;
}

.filter-item :deep(.el-input__wrapper),
.filter-item :deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.search-btn,
.reset-btn {
  border-radius: 10px;
  padding: 0 20px;
}

.table-container {
  padding: 1px;
  overflow: hidden;
}

.custom-table :deep(.el-table__row) {
  transition: background 0.2s;
}

.operator-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.operator-name {
  font-weight: 600;
  color: #1e293b;
}

.operator-username {
  color: #94a3b8;
  font-size: 0.85rem;
}

.time-stamp {
  color: #64748b;
  font-size: 0.875rem;
}

.pagination-footer {
  padding: 24px;
  display: flex;
  justify-content: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.custom-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.custom-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.detail-sub {
  color: #94a3b8;
  font-size: 0.85rem;
  margin-left: 4px;
}

.method-text {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.85rem;
  color: #475569;
  word-break: break-all;
}

.param-pre {
  margin: 0;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.8rem;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.error-text {
  color: #ef4444;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.85rem;
}
</style>
