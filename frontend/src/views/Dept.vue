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
            <h2 class="page-title">部门组织架构</h2>
            <p class="page-desc">管理公司的部门组织架构及层级关系</p>
          </div>
          <div class="action-section">
            <el-button v-if="canAdd" type="primary" class="add-btn" @click="handleAddRoot">
              <el-icon><Plus /></el-icon> 新增根部门
            </el-button>
          </div>
        </div>

        <div class="dept-container glass-panel">
          <el-table 
            :data="deptTree" 
            v-loading="loading" 
            style="width: 100%" 
            class="custom-table"
            row-key="id"
            :header-cell-style="{ background: '#f8fafc', color: '#64748b', fontWeight: 'bold' }"
            default-expand-all
            :tree-props="{ children: 'children' }"
          >
            <el-table-column prop="name" label="部门名称" min-width="200">
              <template #default="{ row }">
                <div class="dept-name-cell">
                  <el-icon v-if="row.status === 1" class="dept-icon active"><OfficeBuilding /></el-icon>
                  <el-icon v-else class="dept-icon inactive"><OfficeBuilding /></el-icon>
                  <span :class="{ 'text-muted': row.status === 0 }">{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="code" label="部门编码" width="140">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.code }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="leader" label="负责人" width="120" />
            <el-table-column prop="phone" label="联系电话" width="150" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" round>
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button v-if="canAdd" link type="primary" @click="handleAddChild(row)">
                    <el-icon><Plus /></el-icon>子部门
                  </el-button>
                  <el-divider direction="vertical" />
                  <el-button v-if="canEdit" link type="primary" @click="handleEdit(row)">编辑</el-button>
                  <el-divider direction="vertical" />
                  <el-button 
                    v-if="canDelete" 
                    link 
                    type="danger" 
                    @click="confirmDelete(row)"
                    :disabled="row.children && row.children.length > 0"
                  >删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-main>
    </el-container>

    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="520px" 
      destroy-on-close
      class="custom-dialog"
    >
      <el-form :model="deptForm" :rules="deptRules" ref="deptFormRef" label-position="top">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="deptForm.code" placeholder="请输入部门编码" :disabled="!!deptForm.id" />
        </el-form-item>
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id', children: 'children', disabled: (d: any) => d.status === 0 }"
            placeholder="请选择上级部门（不选为根部门）"
            clearable
            check-strictly
            :disabled="!!deptForm.id && deptForm.parentId === 0"
          />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="deptForm.leader" placeholder="请输入负责人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="deptForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="deptForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="deptForm.sortOrder" :min="0" :max="999" placeholder="数字越小越靠前" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="deptForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" round>取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm" round>确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="460px"
      destroy-on-close
      class="delete-dialog"
    >
      <div class="delete-warning">
        <div class="warning-icon">
          <el-icon :size="40" color="#ef4444"><WarningFilled /></el-icon>
        </div>
        <div class="warning-content">
          <h4 class="warning-title">确定要删除该部门吗？</h4>
          <p class="warning-desc">
            您即将删除部门 <span class="target-dept">"{{ deleteTargetRow?.name }}"</span>，
            删除后数据将无法恢复。
          </p>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="deleteDialogVisible = false" round>取 消</el-button>
          <el-button type="danger" :loading="deleteSubmitting" @click="confirmDeleteSubmit" round>确 认 删 除</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Notebook, Lock, WarningFilled, User, SwitchButton, ArrowDown, OfficeBuilding } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import type { DeptInfo } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const canList = computed(() => userStore.hasPermission('dept:list') || userStore.isAdmin)
const canAdd = computed(() => userStore.hasPermission('dept:add') || userStore.isAdmin)
const canEdit = computed(() => userStore.hasPermission('dept:edit') || userStore.isAdmin)
const canDelete = computed(() => userStore.hasPermission('dept:delete') || userStore.isAdmin)
const canViewAudit = computed(() => userStore.hasPermission('audit:list') || userStore.isAdmin)

const resolveAvatarUrl = (avatar?: string) => {
  if (!avatar) return defaultAvatar
  if (avatar.startsWith('http')) return avatar
  return avatar
}

const displayName = computed(() => {
  return userStore.userInfo?.nickname || userStore.userInfo?.username || userStore.jwtUsername || '用户'
})

const deptTree = ref<DeptInfo[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
type DialogMode = 'addRoot' | 'addChild' | 'edit' | null
const dialogMode = ref<DialogMode>(null)
const deptFormRef = ref()
const deptForm = ref<any>({
  id: undefined,
  name: '',
  code: '',
  parentId: 0,
  leader: '',
  phone: '',
  email: '',
  sortOrder: 0,
  status: 1
})

const deleteDialogVisible = ref(false)
const deleteTargetRow = ref<any>(null)
const deleteSubmitting = ref(false)

const deptRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }]
}

const fetchDeptTree = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/dept/tree')
    deptTree.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleAddRoot = () => {
  dialogMode.value = 'addRoot'
  dialogTitle.value = '新增根部门'
  deptForm.value = { id: undefined, name: '', code: '', parentId: 0, leader: '', phone: '', email: '', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleAddChild = (row: any) => {
  dialogMode.value = 'addChild'
  dialogTitle.value = `新增子部门 - ${row.name}`
  deptForm.value = { id: undefined, name: '', code: '', parentId: row.id, leader: '', phone: '', email: '', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogMode.value = 'edit'
  dialogTitle.value = '编辑部门'
  deptForm.value = { ...row }
  if (deptForm.value.parentId === null || deptForm.value.parentId === undefined) {
    deptForm.value.parentId = 0
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  await deptFormRef.value.validate()
  submitLoading.value = true
  try {
    if (deptForm.value.id) {
      await request.put('/dept', deptForm.value)
      ElMessage.success('部门更新成功')
    } else {
      await request.post('/dept', deptForm.value)
      ElMessage.success('部门添加成功')
    }
    dialogVisible.value = false
    fetchDeptTree()
  } finally {
    submitLoading.value = false
  }
}

const confirmDelete = (row: any) => {
  if (row.children && row.children.length > 0) {
    ElMessage.warning('该部门下存在子部门，不允许删除')
    return
  }
  deleteTargetRow.value = row
  deleteDialogVisible.value = true
}

const confirmDeleteSubmit = async () => {
  deleteSubmitting.value = true
  try {
    await request.delete(`/dept/${deleteTargetRow.value.id}`)
    ElMessage.success(`已删除部门 "${deleteTargetRow.value.name}"`)
    fetchDeptTree()
    deleteDialogVisible.value = false
    deleteTargetRow.value = null
  } finally {
    deleteSubmitting.value = false
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

const goAuditLog = () => {
  router.push('/audit-log')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  if (canList.value) {
    fetchDeptTree()
  }
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
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

.dept-container {
  padding: 1px;
  overflow: hidden;
}

.custom-table :deep(.el-table__row) {
  transition: background 0.2s;
}

.dept-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dept-icon {
  font-size: 18px;
}

.dept-icon.active {
  color: #3b82f6;
}

.dept-icon.inactive {
  color: #94a3b8;
}

.text-muted {
  color: #94a3b8;
}

.row-actions {
  display: flex;
  align-items: center;
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

.delete-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.delete-warning {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #fef2f2 0%, #fff1f2 100%);
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid #fecaca;
}

.warning-icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

.warning-content {
  flex: 1;
  min-width: 0;
}

.warning-title {
  margin: 0 0 8px 0;
  color: #dc2626;
  font-size: 16px;
  font-weight: 700;
}

.warning-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.target-dept {
  color: #dc2626;
  font-weight: 600;
}
</style>
