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
            <h2 class="page-title">人员名单</h2>
            <p class="page-desc">管理系统中的所有用户信息及权限设置</p>
          </div>
          <div class="action-section">
            <el-button type="primary" class="add-btn" @click="handleAdd">
              <el-icon><Plus /></el-icon> 新增用户
            </el-button>
          </div>
        </div>

        <div class="filters-card glass-panel">
          <el-input
            v-model="searchQuery"
            placeholder="用户名"
            clearable
            @clear="fetchData"
            class="search-bar"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button @click="fetchData" class="search-btn">查询</el-button>
        </div>

        <div class="table-container glass-panel">
          <el-table 
            :data="userList" 
            v-loading="loading" 
            style="width: 100%" 
            class="custom-table"
            :header-cell-style="{ background: '#f8fafc', color: '#64748b', fontWeight: 'bold' }"
          >
            <el-table-column prop="username" label="用户名" min-width="120">
              <template #default="{ row }">
                <span class="user-identity">{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="姓名" min-width="120" />
            <el-table-column prop="email" label="电子邮箱" min-width="180" />
            <el-table-column label="入职时间" width="180">
              <template #default="{ row }">
                <span class="time-stamp">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
                  <el-divider direction="vertical" />
                  <el-button 
                    link 
                    type="danger" 
                    @click="confirmDelete(row)"
                    :disabled="row.username === 'admin'"
                  >删除</el-button>
                </div>
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
            />
          </div>
        </div>
      </el-main>
    </el-container>

    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="480px" 
      destroy-on-close
      class="custom-dialog"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!userForm.id" placeholder="登录使用的唯一账号" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password" :rules="userForm.id ? [] : userRules.password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="长度需在 6-20 位之间" />
        </el-form-item>
        <el-form-item label="姓名 / 昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="显示的真实姓名" />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="example@domain.com" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" round>取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm" round>确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const displayName = computed(() => {
  return userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
})

const userList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const userFormRef = ref()
const userForm = ref<any>({
  id: undefined,
  username: '',
  password: '',
  nickname: '',
  email: ''
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/user/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        username: searchQuery.value
      }
    })
    userList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增用户档案'
  userForm.value = { id: undefined, username: '', password: '', nickname: '', email: '' }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑用户档案'
  userForm.value = { ...row, password: '' }
  dialogVisible.value = true
}

const submitForm = async () => {
  await userFormRef.value.validate()
  submitLoading.value = true
  try {
    if (userForm.value.id) {
      await request.put('/user', userForm.value)
      ElMessage.success('档案更新成功')
    } else {
      await request.post('/user', userForm.value)
      ElMessage.success('成员添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

const confirmDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定要永久删除用户 "${row.nickname}" 吗？此操作不可撤销。`,
    '安全确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
      roundButton: true
    }
  ).then(async () => {
    await request.delete(`/user/${row.id}`)
    ElMessage.success('已移出成员')
    fetchData()
  })
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
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  fetchData()
  if (!userStore.userInfo) {
    userStore.fetchUserInfo()
  }
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
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
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

.add-btn {
  height: 44px;
  padding: 0 24px;
  border-radius: 10px;
  font-weight: 600;
  background: var(--primary-gradient);
  border: none;
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
  margin-bottom: 24px;
}

.search-bar {
  max-width: 400px;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.search-btn {
  border-radius: 10px;
  padding: 0 24px;
}

.table-container {
  padding: 1px;
  overflow: hidden;
}

.custom-table :deep(.el-table__row) {
  transition: background 0.2s;
}

.user-identity {
  font-weight: 600;
  color: #1e293b;
}

.time-stamp {
  color: #64748b;
  font-size: 0.875rem;
}

.row-actions {
  display: flex;
  align-items: center;
}

.pagination-footer {
  padding: 24px;
  display: flex;
  justify-content: center;
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

.custom-dialog :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 10px;
}
</style>
