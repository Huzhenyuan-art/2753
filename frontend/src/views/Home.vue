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
          <el-button type="primary" text @click="goDept">
            <el-icon><OfficeBuilding /></el-icon>部门管理
          </el-button>
          <div class="current-roles" v-if="userStore.userInfo?.roles?.length">
            <el-tag v-for="role in userStore.userInfo.roles" :key="role.id" type="primary" effect="light" round class="role-tag">
              {{ role.name }}
            </el-tag>
          </div>
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
        <div class="main-content">
          <div class="dept-sidebar glass-panel">
            <div class="sidebar-header">
              <h3 class="sidebar-title">
                <el-icon><OfficeBuilding /></el-icon>
                组织架构
              </h3>
              <el-button 
                type="primary" 
                text 
                size="small" 
                @click="deptFilter = undefined"
                :disabled="deptFilter === undefined"
              >
                全部
              </el-button>
            </div>
            <el-tree
              :data="deptTree"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              default-expand-all
              highlight-current
              :current-node-key="deptFilter"
              @node-click="handleDeptNodeClick"
              class="dept-tree"
            >
              <template #default="{ node, data }">
                <div class="tree-node-content">
                  <el-icon v-if="data.status === 1" class="node-icon active"><OfficeBuilding /></el-icon>
                  <el-icon v-else class="node-icon inactive"><OfficeBuilding /></el-icon>
                  <span :class="{ 'text-muted': data.status === 0 }">{{ data.name }}</span>
                </div>
              </template>
            </el-tree>
          </div>

          <div class="user-main">
            <div class="page-header">
              <div class="title-section">
                <h2 class="page-title">
                  {{ currentDeptName ? currentDeptName + ' - ' : '' }}人员名单
                </h2>
                <p class="page-desc">管理系统中的所有用户信息及权限设置</p>
              </div>
              <div class="action-section">
                <el-button v-if="canAdd || canEdit" type="primary" plain class="action-btn" @click="handleDownloadTemplate">
                  <el-icon><Download /></el-icon> 下载模板
                </el-button>
                <el-upload
                  v-if="canAdd"
                  :show-file-list="false"
                  :before-upload="handleBeforeImport"
                  accept=".xlsx,.xls"
                  style="display: inline-block"
                >
                  <el-button type="success" plain class="action-btn">
                    <el-icon><Upload /></el-icon> 批量导入
                  </el-button>
                </el-upload>
                <el-button v-if="canList" type="warning" plain class="action-btn" @click="handleExport">
                  <el-icon><Top /></el-icon> 导出筛选结果
                </el-button>
                <el-button v-if="canAdd" type="primary" class="add-btn" @click="handleAdd">
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
              <el-select
                v-model="statusFilter"
                placeholder="状态"
                clearable
                @clear="fetchData"
                class="status-filter"
              >
                <el-option label="正常" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
              <el-button @click="fetchData" class="search-btn">查询</el-button>
            </div>

        <div class="table-container glass-panel">
          <el-table 
            :data="userList" 
            v-loading="loading" 
            style="width: 100%" 
            class="custom-table"
            :header-cell-style="{ background: '#f8fafc', color: '#64748b', fontWeight: 'bold' }"
            @sort-change="handleSortChange"
            :default-sort="{ prop: sortField, order: sortOrder }"
          >
            <el-table-column label="头像" width="70" align="center">
              <template #default="{ row }">
                <el-avatar :size="36" :src="resolveAvatarUrl(row.avatar)" />
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" min-width="120" sortable="custom">
              <template #default="{ row }">
                <span class="user-identity">{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="姓名" min-width="120" />
            <el-table-column prop="email" label="电子邮箱" min-width="180" />
            <el-table-column label="部门" min-width="180">
              <template #default="{ row }">
                <el-tag v-if="row.depts && row.depts.length" v-for="dept in row.depts" :key="dept.id" type="info" effect="light" round class="role-tag-inline">
                  {{ dept.name }}
                </el-tag>
                <span v-else class="no-role">未分配</span>
              </template>
            </el-table-column>
            <el-table-column label="角色" min-width="160">
              <template #default="{ row }">
                <el-tag v-if="row._roles && row._roles.length" v-for="role in row._roles" :key="role.id" type="success" effect="light" round class="role-tag-inline">
                  {{ role.name }}
                </el-tag>
                <span v-else class="no-role">未分配</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" round>
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="入职时间" width="180" sortable="custom">
              <template #default="{ row }">
                <span class="time-stamp">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="hasAnyAction" label="操作" :width="actionColumnWidth" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <template v-if="canChangeStatus">
                    <el-button 
                      link 
                      :type="row.status === 1 ? 'warning' : 'success'" 
                      @click="confirmToggleStatus(row)"
                      :disabled="row.username === 'admin'"
                    >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
                  </template>
                  <template v-if="canChangeStatus && canEdit">
                    <el-divider direction="vertical" />
                  </template>
                  <template v-if="canEdit">
                    <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
                  </template>
                  <template v-if="(canEdit || canChangeStatus) && canDelete">
                    <el-divider direction="vertical" />
                  </template>
                  <template v-if="canDelete">
                    <el-button 
                      link 
                      type="danger" 
                      @click="confirmDelete(row)"
                      :disabled="row.username === 'admin'"
                    >删除</el-button>
                  </template>
                  <template v-if="canEditRole && (canEdit || canDelete || canChangeStatus)">
                    <el-divider direction="vertical" />
                  </template>
                  <template v-if="canEditRole">
                    <el-button link type="info" @click="handleAssignRole(row)">角色</el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-footer">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              @current-change="fetchData"
              @size-change="handleSizeChange"
            />
          </div>
        </div>
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
        <el-form-item label="头像">
          <div class="avatar-upload-area">
            <el-upload
              class="avatar-uploader"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarChange"
              accept="image/*"
            >
              <el-avatar :size="64" :src="avatarPreview || resolveAvatarUrl(userForm.avatar)" class="avatar-preview" />
              <div class="avatar-upload-overlay">
                <el-icon :size="20"><Upload /></el-icon>
              </div>
            </el-upload>
            <span class="avatar-upload-tip">点击更换头像</span>
          </div>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="userForm.username" 
            :disabled="!!userForm.id" 
            placeholder="登录使用的唯一账号"
            @blur="onUsernameBlur"
            name="new-username"
            autocomplete="new-password"
            data-lpignore="true"
            data-1p-ignore="true"
          >
            <template #suffix>
              <span v-if="usernameCheckStatus === 'checking'" class="username-check-indicator">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span class="check-text checking">检查中...</span>
              </span>
              <span v-else-if="usernameCheckStatus === 'available'" class="username-check-indicator">
                <el-icon color="#10b981"><CircleCheckFilled /></el-icon>
                <span class="check-text available">用户名可用</span>
              </span>
              <span v-else-if="usernameCheckStatus === 'unavailable'" class="username-check-indicator">
                <el-icon color="#ef4444"><CircleCloseFilled /></el-icon>
                <span class="check-text unavailable">用户名已被占用</span>
              </span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="登录密码" prop="password" :rules="userForm.id ? [] : userRules.password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="长度需在 6-20 位之间" name="new-password" autocomplete="new-password" data-lpignore="true" data-1p-ignore="true" />
          <div v-if="userForm.password" class="password-strength">
            <div class="strength-bar">
              <div 
                class="strength-fill" 
                :class="passwordStrength.level"
                :style="{ width: passwordStrength.percent + '%' }"
              ></div>
            </div>
            <span class="strength-text" :class="passwordStrength.level">
              {{ passwordStrength.text }}
            </span>
          </div>
        </el-form-item>
        <el-form-item v-if="!userForm.id" label="确认密码" prop="confirmPassword">
          <el-input v-model="userForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" name="confirm-password" autocomplete="new-password" data-lpignore="true" data-1p-ignore="true" />
        </el-form-item>
        <el-form-item label="姓名 / 昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="显示的真实姓名" />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="example@domain.com" />
        </el-form-item>
        <el-form-item label="所属部门">
          <div class="dept-select-wrapper">
            <el-tree-select
              v-model="userForm.deptIds"
              :data="deptTree"
              :props="{ label: 'name', value: 'id', children: 'children', disabled: (d: any) => d.status === 0 }"
              placeholder="请选择所属部门"
              clearable
              multiple
              collapse-tags
              :collapse-tags-tooltip="true"
              style="width: 100%"
            />
          </div>
          <div v-if="userForm.deptIds && userForm.deptIds.length" class="dept-hierarchy">
            <div class="hierarchy-title">
              <el-icon><Connection /></el-icon>
              组织层级：
            </div>
            <div class="hierarchy-paths">
              <div v-for="(path, idx) in deptHierarchyPaths" :key="idx" class="hierarchy-path">
                <el-tag v-for="(item, i) in path" :key="item.id" :type="i === path.length - 1 ? 'primary' : 'info'" effect="light" size="small">
                  {{ item.name }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="账号状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="canEditRole" label="角色">
          <el-checkbox-group v-model="userForm.roleIds">
            <el-checkbox v-for="role in allRoles" :key="role.id" :value="role.id">
              {{ role.name }}
            </el-checkbox>
          </el-checkbox-group>
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
      title="安全确认 - 删除用户"
      width="460px"
      destroy-on-close
      class="delete-dialog"
      :close-on-click-modal="false"
    >
      <div class="delete-warning">
        <div class="warning-icon">
          <el-icon :size="40" color="#ef4444"><WarningFilled /></el-icon>
        </div>
        <div class="warning-content">
          <h4 class="warning-title">此操作将永久删除用户，不可恢复！</h4>
          <p class="warning-desc">
            您即将删除用户 <span class="target-user">"{{ deleteTargetRow?.nickname || deleteTargetRow?.username }}"</span>，
            该用户的所有相关数据将被清除。
          </p>
        </div>
      </div>
      <div class="delete-verify-section">
        <el-alert
          title="防误删验证"
          type="error"
          :closable="false"
          show-icon
          class="verify-alert"
        >
          <template #default>
            为防止误操作，请手动输入目标用户的
            <span class="verify-username-highlight">用户名</span>
            （登录账号）以确认删除：
            <span class="verify-target-name">{{ deleteTargetRow?.username }}</span>
          </template>
        </el-alert>
        <el-form :model="deleteVerifyForm" :rules="deleteVerifyRules" ref="deleteVerifyFormRef" label-position="top">
          <el-form-item label="请输入用户名" prop="confirmUsername">
            <el-input
              v-model="deleteVerifyForm.confirmUsername"
              :placeholder="`请输入用户名：${deleteTargetRow?.username}`"
              clearable
              @input="onDeleteVerifyInput"
              name="delete-confirm-username"
              autocomplete="off"
              data-lpignore="true"
              data-1p-ignore="true"
            />
          </el-form-item>
        </el-form>
        <div v-if="deleteVerifyError" class="verify-error-tip">
          <el-icon><CircleCloseFilled /></el-icon>
          <span>{{ deleteVerifyError }}</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelDelete" round>取 消</el-button>
          <el-button
            type="danger"
            :loading="deleteSubmitting"
            :disabled="!isDeleteVerified"
            @click="confirmDeleteSubmit"
            round
          >
            <el-icon><Delete /></el-icon>
            确认永久删除
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importResultVisible"
      title="批量导入结果"
      width="640px"
      destroy-on-close
      class="import-result-dialog"
    >
      <div v-if="importResult" class="import-summary">
        <div class="summary-item total">
          <span class="summary-label">总计</span>
          <span class="summary-value">{{ importResult.totalCount }}</span>
        </div>
        <div class="summary-item success">
          <span class="summary-label">成功</span>
          <span class="summary-value">{{ importResult.successCount }}</span>
        </div>
        <div class="summary-item fail" :class="{ highlight: importResult.failCount > 0 }">
          <span class="summary-label">失败</span>
          <span class="summary-value">{{ importResult.failCount }}</span>
        </div>
      </div>
      <div v-if="importResult && importResult.errors && importResult.errors.length" class="import-errors">
        <div class="errors-header">
          <el-icon color="#ef4444"><WarningFilled /></el-icon>
          <span class="errors-title">失败详情（共 {{ importResult.errors.length }} 条）</span>
        </div>
        <el-table :data="importResult.errors" style="width: 100%" max-height="360" class="error-table">
          <el-table-column prop="rowNum" label="行号" width="80" align="center" />
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="errorMessage" label="错误原因" min-width="300">
            <template #default="{ row }">
              <span class="error-text">{{ row.errorMessage }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else-if="importResult && importResult.failCount === 0" class="import-all-success">
        <el-icon color="#10b981" :size="48"><CircleCheckFilled /></el-icon>
        <span>全部导入成功！</span>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="importResultVisible = false" round>确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Notebook, Loading, CircleCheckFilled, CircleCloseFilled, Lock, WarningFilled, Delete, OfficeBuilding, Connection, Download, Top, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import type { DeptInfo } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const canList = computed(() => userStore.hasPermission('user:list') || userStore.isAdmin)
const canAdd = computed(() => userStore.hasPermission('user:add') || userStore.isAdmin)
const canEdit = computed(() => userStore.hasPermission('user:edit') || userStore.isAdmin)
const canDelete = computed(() => userStore.hasPermission('user:delete') || userStore.isAdmin)
const canChangeStatus = computed(() => userStore.hasPermission('user:status') || userStore.isAdmin)
const canEditRole = computed(() => userStore.isAdmin)
const canViewAudit = computed(() => userStore.hasPermission('audit:list') || userStore.isAdmin)

const hasAnyAction = computed(() => canEdit.value || canDelete.value || canChangeStatus.value || canEditRole.value)
const actionColumnWidth = computed(() => {
  let count = 0
  if (canChangeStatus.value) count++
  if (canEdit.value) count++
  if (canDelete.value) count++
  if (canEditRole.value) count++
  return Math.max(count * 60 + 60, 180)
})

const resolveAvatarUrl = (avatar?: string) => {
  if (!avatar) return defaultAvatar
  if (avatar.startsWith('http')) return avatar
  return avatar
}

const displayName = computed(() => {
  return userStore.userInfo?.nickname || userStore.userInfo?.username || userStore.jwtUsername || '用户'
})

interface RoleOption {
  id: number
  name: string
  code: string
}

const allRoles = ref<RoleOption[]>([])

const deptTree = ref<DeptInfo[]>([])
const deptFilter = ref<number | undefined>(undefined)
const deptLoading = ref(false)

const currentDeptName = computed(() => {
  if (deptFilter.value === undefined) return ''
  const findDept = (list: DeptInfo[], id: number): DeptInfo | null => {
    for (const dept of list) {
      if (dept.id === id) return dept
      if (dept.children) {
        const found = findDept(dept.children, id)
        if (found) return found
      }
    }
    return null
  }
  const dept = findDept(deptTree.value, deptFilter.value)
  return dept?.name || ''
})

const deptMap = computed(() => {
  const map = new Map<number, DeptInfo>()
  const buildMap = (list: DeptInfo[]) => {
    for (const dept of list) {
      map.set(dept.id, dept)
      if (dept.children) {
        buildMap(dept.children)
      }
    }
  }
  buildMap(deptTree.value)
  return map
})

const deptHierarchyPaths = computed(() => {
  if (!userForm.value.deptIds || !userForm.value.deptIds.length) return []
  const paths: DeptInfo[][] = []
  for (const deptId of userForm.value.deptIds) {
    const path: DeptInfo[] = []
    let currentId: number | undefined = deptId
    while (currentId) {
      const dept = deptMap.value.get(currentId)
      if (!dept) break
      path.unshift(dept)
      currentId = dept.parentId || undefined
    }
    if (path.length) {
      paths.push(path)
    }
  }
  return paths
})

const userList = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const PAGE_SIZE_KEY = 'user_list_page_size'
const getSavedPageSize = (): number => {
  const saved = localStorage.getItem(PAGE_SIZE_KEY)
  if (saved) {
    const size = parseInt(saved, 10)
    if ([10, 20, 50, 100].includes(size)) {
      return size
    }
  }
  return 10
}
const pageSize = ref(getSavedPageSize())
const searchQuery = ref('')
const statusFilter = ref<number | undefined>(undefined)
const loading = ref(false)
const sortField = ref('createTime')
const sortOrder = ref('descending')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
type DialogMode = 'add' | 'edit' | 'assign' | null
const dialogMode = ref<DialogMode>(null)
const userFormRef = ref()
const userForm = ref<any>({
  id: undefined,
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  avatar: '',
  status: 1,
  roleIds: [] as number[],
  deptIds: [] as number[]
})

const avatarPreview = ref('')
const avatarFile = ref<File | null>(null)

const deleteDialogVisible = ref(false)
const deleteTargetRow = ref<any>(null)
const deleteSubmitting = ref(false)
const deleteVerifyError = ref('')
const deleteVerifyFormRef = ref()
const deleteVerifyForm = ref({
  confirmUsername: ''
})

const importResultVisible = ref(false)
const importResult = ref<any>(null)
const importing = ref(false)
const isDeleteVerified = computed(() => {
  if (!deleteTargetRow.value?.username) return false
  return deleteVerifyForm.value.confirmUsername.trim() === deleteTargetRow.value.username
})
const validateConfirmUsername = (_rule: any, value: string, callback: any) => {
  const trimmed = value?.trim()
  if (!trimmed) {
    callback(new Error('请输入目标用户名'))
    return
  }
  if (trimmed !== deleteTargetRow.value?.username) {
    callback(new Error('用户名不匹配，请重新输入'))
    return
  }
  callback()
}
const deleteVerifyRules = {
  confirmUsername: [
    { required: true, message: '请输入目标用户名', trigger: 'blur' },
    { validator: validateConfirmUsername, trigger: ['blur', 'input'] }
  ]
}
const onDeleteVerifyInput = () => {
  deleteVerifyError.value = ''
}

type UsernameCheckStatus = 'idle' | 'checking' | 'available' | 'unavailable'
const usernameCheckStatus = ref<UsernameCheckStatus>('idle')
let usernameDebounceTimer: ReturnType<typeof setTimeout> | null = null
let usernameCheckAbortController: AbortController | null = null

const checkUsernameAvailable = async (username: string, excludeId?: number): Promise<boolean> => {
  if (usernameCheckAbortController) {
    usernameCheckAbortController.abort()
  }
  usernameCheckAbortController = new AbortController()
  try {
    const params: any = { username }
    if (excludeId) {
      params.excludeId = excludeId
    }
    const res: any = await request.get('/user/check-username', {
      params,
      signal: usernameCheckAbortController.signal,
      skipErrorToast: true
    } as any)
    return !!res.data
  } catch (e: any) {
    if (e.name === 'AbortError' || e.code === 'ERR_CANCELED' || e.code === 'ECONNABORTED') {
      throw e
    }
    return true
  }
}

const debouncedCheckUsername = (username: string, excludeId?: number) => {
  if (usernameDebounceTimer) {
    clearTimeout(usernameDebounceTimer)
  }
  if (!username) {
    usernameCheckStatus.value = 'idle'
    return
  }
  usernameCheckStatus.value = 'checking'
  usernameDebounceTimer = setTimeout(async () => {
    try {
      const available = await checkUsernameAvailable(username, excludeId)
      if (dialogMode.value !== 'add' || !dialogVisible.value) return
      usernameCheckStatus.value = available ? 'available' : 'unavailable'
    } catch (e: any) {
      if (e.name === 'AbortError' || e.code === 'ERR_CANCELED') {
        return
      }
      usernameCheckStatus.value = 'idle'
    }
  }, 500)
}

const onUsernameBlur = () => {
  if (userForm.value.id) return
  if (dialogMode.value !== 'add') return
  if (!dialogVisible.value) return
  const username = userForm.value.username?.trim()
  if (!username) return
  usernameCheckStatus.value = 'checking'
  if (usernameDebounceTimer) {
    clearTimeout(usernameDebounceTimer)
    usernameDebounceTimer = null
  }
  ;(async () => {
    try {
      const available = await checkUsernameAvailable(username)
      if (dialogMode.value !== 'add' || !dialogVisible.value) return
      usernameCheckStatus.value = available ? 'available' : 'unavailable'
    } catch (e: any) {
      if (e.name === 'AbortError' || e.code === 'ERR_CANCELED') {
        return
      }
      usernameCheckStatus.value = 'idle'
    }
  })()
}

const passwordStrength = computed(() => {
  const pwd = userForm.value.password || ''
  let score = 0
  if (pwd.length >= 6) score += 1
  if (pwd.length >= 10) score += 1
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score += 1
  if (/\d/.test(pwd)) score += 1
  if (/[!@#$%^&*(),.?":{}|<>_\-+=\[\]\/'`~]/.test(pwd)) score += 1

  if (score <= 1) return { level: 'weak', text: '弱', percent: 25 }
  if (score <= 2) return { level: 'medium', text: '中', percent: 50 }
  if (score <= 3) return { level: 'strong', text: '强', percent: 75 }
  return { level: 'very-strong', text: '非常强', percent: 100 }
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== userForm.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateUsername = async (_rule: any, value: string, callback: any) => {
  const trimmed = value?.trim()
  if (!trimmed) {
    callback(new Error('请输入用户名'))
    return
  }
  if (userForm.value.id) {
    callback()
    return
  }
  if (dialogMode.value !== 'add') {
    callback()
    return
  }
  try {
    if (usernameCheckStatus.value === 'checking') {
      const available = await checkUsernameAvailable(trimmed)
      if (dialogMode.value !== 'add' || !dialogVisible.value) {
        callback()
        return
      }
      usernameCheckStatus.value = available ? 'available' : 'unavailable'
      if (!available) {
        callback(new Error('用户名已被占用，请更换'))
        return
      }
    } else if (usernameCheckStatus.value === 'unavailable') {
      callback(new Error('用户名已被占用，请更换'))
      return
    }
    callback()
  } catch (e: any) {
    if (e.name === 'AbortError' || e.code === 'ERR_CANCELED') {
      callback(new Error('请等待校验完成'))
    } else {
      callback()
    }
  }
}

const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
}

const fetchAllRoles = async () => {
  try {
    const res: any = await request.get('/role/list')
    allRoles.value = res.data
  } catch (e) {
    allRoles.value = []
  }
}

const fetchDeptTree = async () => {
  deptLoading.value = true
  try {
    const res: any = await request.get('/dept/tree')
    deptTree.value = res.data || []
  } catch (e) {
    deptTree.value = []
  } finally {
    deptLoading.value = false
  }
}

const loadUserRoles = async (userId: number) => {
  try {
    const res: any = await request.get(`/user/${userId}/roles`)
    return res.data || []
  } catch (e) {
    return []
  }
}

const loadUserDepts = async (userId: number) => {
  try {
    const res: any = await request.get(`/dept/user/${userId}/ids`)
    return res.data || []
  } catch (e) {
    return []
  }
}

const handleDeptNodeClick = (data: any) => {
  deptFilter.value = data.id
  pageNum.value = 1
  fetchData()
}

const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  if (prop && order) {
    sortField.value = prop
    sortOrder.value = order
    pageNum.value = 1
    fetchData()
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: searchQuery.value
    }
    if (statusFilter.value !== undefined && statusFilter.value !== null) {
      params.status = statusFilter.value
    }
    if (deptFilter.value !== undefined) {
      params.deptId = deptFilter.value
    }
    if (sortField.value && sortOrder.value) {
      params.sortField = sortField.value
      params.sortOrder = sortOrder.value === 'ascending' ? 'asc' : 'desc'
    }
    const res: any = await request.get('/user/list', { params })
    const list = res.data.records || []
    for (const row of list) {
      row._roles = []
      try {
        const roleIds: number[] = await loadUserRoles(row.id)
        row._roles = allRoles.value.filter((r: RoleOption) => roleIds.includes(r.id))
      } catch (e) {
        row._roles = []
      }
    }
    userList.value = list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size: number) => {
  localStorage.setItem(PAGE_SIZE_KEY, String(size))
  pageNum.value = 1
  fetchData()
}

const handleAdd = () => {
  dialogMode.value = 'add'
  dialogTitle.value = '新增用户档案'
  userForm.value = { 
    id: undefined, 
    username: '', 
    password: '', 
    confirmPassword: '', 
    nickname: '', 
    email: '', 
    avatar: '', 
    status: 1, 
    roleIds: [],
    deptIds: deptFilter.value ? [deptFilter.value] : []
  }
  avatarPreview.value = ''
  avatarFile.value = null
  usernameCheckStatus.value = 'idle'
  if (usernameDebounceTimer) {
    clearTimeout(usernameDebounceTimer)
    usernameDebounceTimer = null
  }
  if (usernameCheckAbortController) {
    usernameCheckAbortController.abort()
    usernameCheckAbortController = null
  }
  dialogVisible.value = true
  nextTick(() => {
    if (dialogMode.value !== 'add') return
    if (userForm.value.username && !userForm.value.id) {
      userForm.value.username = ''
      userForm.value.password = ''
      userForm.value.confirmPassword = ''
      usernameCheckStatus.value = 'idle'
    }
    userFormRef.value?.clearValidate()
  })
}

const confirmToggleStatus = (row: any) => {
  if (!canChangeStatus.value) {
    ElMessage.warning('您没有切换用户状态的权限')
    return
  }
  const targetStatus = row.status === 1 ? 0 : 1
  const actionText = targetStatus === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(
    `确定要${actionText}用户 "${row.nickname}" 吗？`,
    '操作确认',
    {
      confirmButtonText: `确定${actionText}`,
      cancelButtonText: '取消',
      type: targetStatus === 1 ? 'success' : 'warning',
      roundButton: true
    }
  ).then(async () => {
    const res: any = await request.put(`/user/${row.id}/status`, null, {
      params: { status: targetStatus }
    })
    ElMessage.success(res.message || `已${actionText}用户`)
    fetchData()
  })
}

const handleEdit = async (row: any) => {
  if (!canEdit.value) {
    ElMessage.warning('您没有编辑用户的权限')
    return
  }
  dialogMode.value = 'edit'
  dialogTitle.value = '编辑用户档案'
  let roleIds: number[] = []
  let deptIds: number[] = []
  if (canEditRole.value) {
    roleIds = await loadUserRoles(row.id)
  }
  deptIds = await loadUserDepts(row.id)
  if (dialogMode.value !== 'edit') {
    return
  }
  userForm.value = { ...row, password: '', confirmPassword: '', roleIds, deptIds }
  avatarPreview.value = ''
  avatarFile.value = null
  usernameCheckStatus.value = 'idle'
  if (usernameDebounceTimer) {
    clearTimeout(usernameDebounceTimer)
    usernameDebounceTimer = null
  }
  if (usernameCheckAbortController) {
    usernameCheckAbortController.abort()
    usernameCheckAbortController = null
  }
  dialogVisible.value = true
}

watch(() => userForm.value.password, () => {
  if (userForm.value.confirmPassword) {
    userFormRef.value?.validateField('confirmPassword')
  }
})

watch(() => userForm.value.username, (newVal, oldVal) => {
  if (userForm.value.id) return
  if (dialogMode.value !== 'add') return
  if (!dialogVisible.value) return
  if (!newVal?.trim()) {
    usernameCheckStatus.value = 'idle'
    return
  }
  debouncedCheckUsername(newVal.trim())
})

watch(() => dialogVisible.value, (val) => {
  if (!val) {
    dialogMode.value = null
    if (usernameDebounceTimer) {
      clearTimeout(usernameDebounceTimer)
      usernameDebounceTimer = null
    }
    if (usernameCheckAbortController) {
      usernameCheckAbortController.abort()
      usernameCheckAbortController = null
    }
  }
})

const handleAssignRole = async (row: any) => {
  if (!canEditRole.value) {
    ElMessage.warning('您没有分配角色的权限')
    return
  }
  dialogMode.value = 'assign'
  dialogTitle.value = `分配角色 - ${row.nickname || row.username}`
  const roleIds = await loadUserRoles(row.id)
  if (dialogMode.value !== 'assign') {
    return
  }
  userForm.value = { ...row, password: '', confirmPassword: '', roleIds }
  avatarPreview.value = ''
  avatarFile.value = null
  usernameCheckStatus.value = 'idle'
  if (usernameDebounceTimer) {
    clearTimeout(usernameDebounceTimer)
    usernameDebounceTimer = null
  }
  if (usernameCheckAbortController) {
    usernameCheckAbortController.abort()
    usernameCheckAbortController = null
  }
  dialogVisible.value = true
}

const handleAvatarChange = (uploadFile: any) => {
  const raw = uploadFile.raw
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('仅支持上传图片文件（如 JPG、PNG、GIF 等）')
    return
  }
  if (raw.size > 20 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过20MB，请压缩后重试')
    return
  }
  avatarFile.value = raw
  avatarPreview.value = URL.createObjectURL(raw)
}

const submitForm = async () => {
  await userFormRef.value.validate()
  submitLoading.value = true
  try {
    if (avatarFile.value) {
      const formData = new FormData()
      formData.append('file', avatarFile.value)
      const uploadRes: any = await request.post('/file/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000,
      })
      userForm.value.avatar = uploadRes.data
    }
    if (userForm.value.id) {
      await request.put('/user', userForm.value)
      if (canEditRole.value && userForm.value.roleIds !== undefined) {
        await request.put(`/user/${userForm.value.id}/roles`, { roleIds: userForm.value.roleIds })
      }
      ElMessage.success('档案更新成功')
    } else {
      const addRes: any = await request.post('/user', userForm.value)
      if (canEditRole.value && userForm.value.roleIds && userForm.value.roleIds.length) {
        const newId = addRes.data?.id || userForm.value.id
        if (newId) {
          await request.put(`/user/${newId}/roles`, { roleIds: userForm.value.roleIds })
        }
      }
      ElMessage.success('成员添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

const confirmDelete = (row: any) => {
  if (!canDelete.value) {
    ElMessage.warning('您没有删除用户的权限')
    return
  }
  deleteTargetRow.value = row
  deleteVerifyForm.value = { confirmUsername: '' }
  deleteVerifyError.value = ''
  deleteDialogVisible.value = true
  nextTick(() => {
    deleteVerifyFormRef.value?.clearValidate()
  })
}

const cancelDelete = () => {
  deleteDialogVisible.value = false
  deleteTargetRow.value = null
  deleteVerifyForm.value = { confirmUsername: '' }
  deleteVerifyError.value = ''
  deleteSubmitting.value = false
}

const confirmDeleteSubmit = async () => {
  if (!isDeleteVerified.value) {
    deleteVerifyError.value = '请输入正确的用户名后再执行删除操作'
    return
  }
  try {
    await deleteVerifyFormRef.value.validate()
  } catch (e) {
    deleteVerifyError.value = '用户名验证失败，请重新输入'
    return
  }
  deleteSubmitting.value = true
  try {
    await request.delete(`/user/${deleteTargetRow.value.id}`)
    ElMessage.success(`已永久删除用户 "${deleteTargetRow.value.nickname || deleteTargetRow.value.username}"`)
    fetchData()
    cancelDelete()
  } finally {
    deleteSubmitting.value = false
  }
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

const goDept = () => {
  router.push('/dept')
}

const EXCEL_CONTENT_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

const readBlobAsText = (blob: Blob): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error)
    reader.readAsText(blob, 'utf-8')
  })
}

const triggerBlobDownload = (blob: Blob, filename: string) => {
  const downloadUrl = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = downloadUrl
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(downloadUrl)
}

const handleBlobDownloadResponse = async (response: any, fallbackFilename: string) => {
  const data = response.data
  if (!(data instanceof Blob)) {
    ElMessage.error('响应格式异常')
    return
  }

  if (data.type === EXCEL_CONTENT_TYPE || data.size > 0 && !data.type.includes('json')) {
    let filename = fallbackFilename
    try {
      const contentDisposition = (response.headers as any)?.['content-disposition']
      if (contentDisposition) {
        const match = contentDisposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/i)
        if (match && match[1]) {
          filename = decodeURIComponent(match[1].trim().replace(/^["']|["']$/g, ''))
        }
      }
    } catch {}
    triggerBlobDownload(data, filename)
    ElMessage.success('下载成功')
    return
  }

  try {
    const text = await readBlobAsText(data)
    const json = JSON.parse(text)
    if (json.message) {
      ElMessage.error(json.message)
    } else {
      ElMessage.error('下载失败')
    }
  } catch {
    ElMessage.error('下载失败')
  }
}

const handleDownloadTemplate = async () => {
  try {
    const response = await request.get('/user/template', {
      responseType: 'blob',
      skipErrorToast: true,
    } as any)
    await handleBlobDownloadResponse(response, '用户导入模板.xlsx')
  } catch (e: any) {
    if (e.type !== 'business' && e.type !== 'http') {
      ElMessage.error(e.message || '网络异常，模板下载失败')
    }
  }
}

const handleBeforeImport = async (file: File) => {
  const validTypes = ['.xlsx', '.xls']
  const fileName = file.name.toLowerCase()
  const isValid = validTypes.some(type => fileName.endsWith(type))
  if (!isValid) {
    ElMessage.error('仅支持 Excel 文件格式（.xlsx 或 .xls）')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }

  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res: any = await request.post('/user/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
    importResult.value = res.data
    importResultVisible.value = true
    if (res.data && res.data.successCount > 0) {
      fetchData()
    }
  } catch (e: any) {
    if (e.type !== 'business') {
      ElMessage.error(e.message || '导入失败，请稍后重试')
    }
  } finally {
    importing.value = false
  }
  return false
}

const handleExport = async () => {
  const params: any = {}
  if (searchQuery.value) {
    params.username = searchQuery.value
  }
  if (statusFilter.value !== undefined && statusFilter.value !== null) {
    params.status = statusFilter.value
  }
  if (deptFilter.value !== undefined) {
    params.deptId = deptFilter.value
  }
  if (sortField.value && sortOrder.value) {
    params.sortField = sortField.value
    params.sortOrder = sortOrder.value === 'ascending' ? 'asc' : 'desc'
  }

  try {
    const response = await request.get('/user/export', {
      params,
      responseType: 'blob',
      skipErrorToast: true,
    } as any)
    const now = new Date()
    const ts = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`
    await handleBlobDownloadResponse(response, `用户列表_${ts}.xlsx`)
  } catch (e: any) {
    if (e.type !== 'business' && e.type !== 'http') {
      ElMessage.error(e.message || '网络异常，导出失败')
    }
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  await fetchAllRoles()
  await fetchDeptTree()
  if (canList.value) {
    fetchData()
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

.action-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-btn {
  height: 44px;
  padding: 0 18px;
  border-radius: 10px;
  font-weight: 500;
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

.status-filter {
  width: 140px;
}

.search-bar :deep(.el-input__wrapper),
.status-filter :deep(.el-select__wrapper) {
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

.avatar-upload-area {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
}

.avatar-preview {
  border: 2px solid #e2e8f0;
}

.avatar-upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-uploader:hover .avatar-upload-overlay {
  opacity: 1;
}

.avatar-upload-tip {
  color: #94a3b8;
  font-size: 0.85rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-roles {
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-tag {
  font-size: 12px;
}

.role-tag-inline {
  margin-right: 4px;
  font-size: 12px;
}

.no-role {
  color: #94a3b8;
  font-size: 13px;
}

.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
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
  transition: width 0.3s ease, background-color 0.3s ease;
  border-radius: 3px;
}

.strength-fill.weak {
  background: #ef4444;
}

.strength-fill.medium {
  background: #f59e0b;
}

.strength-fill.strong {
  background: #10b981;
}

.strength-fill.very-strong {
  background: #059669;
}

.strength-text {
  font-size: 12px;
  font-weight: 600;
  min-width: 50px;
}

.strength-text.weak {
  color: #ef4444;
}

.strength-text.medium {
  color: #f59e0b;
}

.strength-text.strong {
  color: #10b981;
}

.strength-text.very-strong {
  color: #059669;
}

.username-check-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.check-text {
  font-size: 12px;
  font-weight: 500;
}

.check-text.checking {
  color: #64748b;
}

.check-text.available {
  color: #10b981;
}

.check-text.unavailable {
  color: #ef4444;
}

.delete-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.delete-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.delete-dialog :deep(.el-dialog__title) {
  color: #0f172a;
  font-weight: 700;
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

.target-user {
  color: #dc2626;
  font-weight: 600;
}

.delete-verify-section {
  margin-bottom: 8px;
}

.verify-alert {
  margin-bottom: 20px;
  border-radius: 10px;
}

.verify-alert :deep(.el-alert__description) {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.verify-username-highlight {
  color: #dc2626;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.verify-target-name {
  display: inline-block;
  margin-left: 6px;
  padding: 2px 10px;
  background: #fff;
  border: 1px dashed #fca5a5;
  border-radius: 6px;
  color: #dc2626;
  font-weight: 700;
  font-family: 'SF Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
}

.delete-verify-section :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}

.delete-verify-section :deep(.el-input__wrapper) {
  border-radius: 10px;
  transition: all 0.2s;
}

.delete-verify-section :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2) inset;
}

.verify-error-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 13px;
  font-weight: 500;
  margin-top: -4px;
}

.main-content {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.dept-sidebar {
  width: 280px;
  flex-shrink: 0;
  padding: 20px;
  max-height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.dept-tree {
  flex: 1;
  overflow-y: auto;
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}

.node-icon {
  font-size: 16px;
}

.node-icon.active {
  color: #3b82f6;
}

.node-icon.inactive {
  color: #94a3b8;
}

.user-main {
  flex: 1;
  min-width: 0;
}

.dept-select-wrapper {
  margin-bottom: 8px;
}

.dept-hierarchy {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.hierarchy-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.hierarchy-paths {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hierarchy-path {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.text-muted {
  color: #94a3b8;
}

@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }
  
  .dept-sidebar {
    width: 100%;
    max-height: 300px;
  }
}

.import-result-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.import-result-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.import-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.summary-item {
  flex: 1;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.summary-item.total {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-color: #bfdbfe;
}

.summary-item.success {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-color: #a7f3d0;
}

.summary-item.fail {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-color: #e2e8f0;
}

.summary-item.fail.highlight {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border-color: #fecaca;
}

.summary-label {
  display: block;
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  margin-bottom: 8px;
}

.summary-value {
  display: block;
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
}

.summary-item.success .summary-value {
  color: #059669;
}

.summary-item.fail.highlight .summary-value {
  color: #dc2626;
}

.import-errors {
  margin-top: 8px;
}

.errors-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #dc2626;
}

.errors-title {
  font-size: 14px;
}

.error-table {
  border-radius: 10px;
  overflow: hidden;
}

.error-text {
  color: #dc2626;
  font-size: 13px;
}

.import-all-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 20px;
  font-size: 18px;
  font-weight: 600;
  color: #059669;
}
</style>
