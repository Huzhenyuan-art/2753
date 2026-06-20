import { defineStore } from 'pinia'
import request from '@/utils/request'
import {
  getAccessToken,
  setTokens,
  clearAllTokens,
  parseJwt,
  JwtPayload,
} from '@/utils/token'

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

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null as UserInfo | null,
  }),
  getters: {
    token(): string {
      return getAccessToken()
    },
    jwtPayload(): JwtPayload | null {
      const token = getAccessToken()
      if (!token) return null
      return parseJwt(token)
    },
    jwtUsername(): string | null {
      return this.jwtPayload?.sub || null
    },
    jwtUserId(): number | null {
      return this.jwtPayload?.userId || null
    },
    roleCodes(state): string[] {
      if (state.userInfo?.roleCodes) return state.userInfo.roleCodes
      return this.jwtPayload?.roles || []
    },
    permissionCodes(state): string[] {
      if (state.userInfo?.permissionCodes) return state.userInfo.permissionCodes
      return this.jwtPayload?.permissions || []
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
    },
    isLoggedIn(): boolean {
      return !!getAccessToken()
    },
  },
  actions: {
    setLoginData(data: any) {
      const {
        token,
        refreshToken,
        accessTokenExpiresIn,
        userId,
        username,
        nickname,
        email,
        avatar,
        status,
        createTime,
        updateTime,
        roles,
        permissions,
        roleCodes,
        permissionCodes,
      } = data

      setTokens(token, refreshToken, accessTokenExpiresIn)

      this.userInfo = {
        userId,
        username,
        nickname,
        email,
        avatar,
        status,
        createTime: createTime || '',
        updateTime: updateTime || '',
        roles: roles || [],
        permissions: permissions || [],
        roleCodes: roleCodes || [],
        permissionCodes: permissionCodes || [],
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
          ...info,
        },
      })
    },
    logout() {
      this.userInfo = null
      clearAllTokens()
    },
  },
})
