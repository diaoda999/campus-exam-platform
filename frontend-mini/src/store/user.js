import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null,
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') || '',
    classId: localStorage.getItem('classId') ? Number(localStorage.getItem('classId')) : null
  }),
  actions: {
    setLogin(info) {
      this.token = info.token
      this.userId = info.userId
      this.username = info.username
      this.realName = info.realName
      this.role = info.role
      this.classId = info.classId
      localStorage.setItem('token', info.token)
      localStorage.setItem('userId', info.userId)
      localStorage.setItem('username', info.username)
      localStorage.setItem('realName', info.realName || '')
      localStorage.setItem('role', info.role)
      localStorage.setItem('classId', info.classId || '')
    },
    logout() {
      this.token = ''
      this.role = ''
      localStorage.clear()
    }
  }
})
