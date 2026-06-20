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
        <div class="page-header">
          <div class="title-section">
            <h2 class="page-title">人员名单</h2>
            <p class="page-desc">管理系统中的所有用户信息及权限设置</p>
          </div>
          <div class="action-section">
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
          >
            <el-table-column label="头像" width="70" align="center">
              <template #default="{ row }">
                <el-avatar :size="36" :src="resolveAvatarUrl(row.avatar)" />
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" min-width="120">
              <template #default="{ row }">
                <span class="user-identity">{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="姓名" min-width="120" />
            <el-table-column prop="email" label="电子邮箱" min-width="180" />
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
            <el-table-column label="入职时间" width="180">
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Notebook, Loading, CircleCheckFilled, CircleCloseFilled, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

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

const userList = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const statusFilter = ref<number | undefined>(undefined)
const loading = ref(false)

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
  roleIds: [] as number[]
})

const avatarPreview = ref('')
const avatarFile = ref<File | null>(null)

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

const loadUserRoles = async (userId: number) => {
  try {
    const res: any = await request.get(`/user/${userId}/roles`)
    return res.data || []
  } catch (e) {
    return []
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
    const res: any = await request.get('/user/list', { params })
    const list = res.data.records || []
    const roleMap = new Map<number, RoleOption[]>()
    for (const role of allRoles.value) {
      roleMap.set(role.id, [])
    }
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

const handleAdd = () => {
  dialogMode.value = 'add'
  dialogTitle.value = '新增用户档案'
  userForm.value = { id: undefined, username: '', password: '', confirmPassword: '', nickname: '', email: '', avatar: '', status: 1, roleIds: [] }
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
  if (canEditRole.value) {
    roleIds = await loadUserRoles(row.id)
  }
  if (dialogMode.value !== 'edit') {
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

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  await fetchAllRoles()
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
</style>
