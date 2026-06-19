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

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null as UserInfo | null
  }),
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
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})
