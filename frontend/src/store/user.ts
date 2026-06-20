import { defineStore } from 'pinia'
import request from '@/utils/request'

interface RoleInfo {
  id: number
  name: string
  code: string
  description: string
  status: number
}

interface PermissionInfo {
  id: number
  name: string
  code: string
  type: string
  description: string
  status: number
}

interface UserInfo {
  userId: number
  username: string
  nickname: string
  email: string
  avatar: string
  status: number
  createTime: string
  updateTime: string
  roles: RoleInfo[]
  permissions: PermissionInfo[]
  roleCodes: string[]
  permissionCodes: string[]
}

function parseJwtPayload(token: string): any {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    return JSON.parse(atob(parts[1]))
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null as UserInfo | null
  }),
  getters: {
    jwtUsername(state): string | null {
      if (!state.token) return null
      const payload = parseJwtPayload(state.token)
      return payload?.sub || null
    },
    jwtUserId(state): number | null {
      if (!state.token) return null
      const payload = parseJwtPayload(state.token)
      return payload?.userId || null
    },
    roleCodes(state): string[] {
      if (state.userInfo?.roleCodes) return state.userInfo.roleCodes
      if (!state.token) return []
      const payload = parseJwtPayload(state.token)
      return payload?.roles || []
    },
    permissionCodes(state): string[] {
      if (state.userInfo?.permissionCodes) return state.userInfo.permissionCodes
      if (!state.token) return []
      const payload = parseJwtPayload(state.token)
      return payload?.permissions || []
    },
    isAdmin(): boolean {
      return this.roleCodes.includes('ADMIN')
    },
    hasRole: (state) => (roleCode: string): boolean => {
      if (state.userInfo?.roleCodes) {
        return state.userInfo.roleCodes.includes(roleCode)
      }
      return false
    },
    hasPermission: (state) => (permissionCode: string): boolean => {
      if (state.userInfo?.permissionCodes) {
        return state.userInfo.permissionCodes.includes(permissionCode)
      }
      return false
    }
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setLoginData(data: any) {
      this.token = data.token
      localStorage.setItem('token', data.token)
      this.userInfo = {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        email: data.email,
        avatar: data.avatar,
        status: data.status,
        createTime: data.createTime || '',
        updateTime: data.updateTime || '',
        roles: data.roles || [],
        permissions: data.permissions || [],
        roleCodes: data.roleCodes || [],
        permissionCodes: data.permissionCodes || []
      }
    },
    async fetchUserInfo() {
      try {
        const res: any = await request.get('/user/info')
        this.userInfo = res.data
        return res.data
      } catch (error) {
        return null
      }
    },
    setUserInfo(info: Partial<UserInfo>) {
      if (!this.userInfo) return
      this.$patch({
        userInfo: {
          ...this.userInfo,
          ...info
        }
      })
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})
