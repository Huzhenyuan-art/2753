import { defineStore } from 'pinia'
import request from '@/utils/request'

interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  status: number
  createTime: string
  updateTime: string
}

function parseJwtSubject(token: string): string | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = JSON.parse(atob(parts[1]))
    return payload.sub || null
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
      return parseJwtSubject(state.token)
    }
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
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
