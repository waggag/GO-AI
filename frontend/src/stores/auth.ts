import { defineStore } from 'pinia'
import { login, register, setToken } from '../api'
import { STORAGE_KEYS } from '../utils'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(STORAGE_KEYS.TOKEN) ?? '',
    username: localStorage.getItem(STORAGE_KEYS.USER) ?? ''
  }),
  actions: {
    async signIn(username: string, password: string) {
      const data = await login(username, password)
      this.update(data.token, data.username)
    },
    async signUp(username: string, password: string) {
      const data = await register(username, password)
      this.update(data.token, data.username)
    },
    restore() {
      if (this.token) {
        setToken(this.token)
      }
    },
    update(token: string, username: string) {
      this.token = token
      this.username = username
      localStorage.setItem(STORAGE_KEYS.TOKEN, token)
      localStorage.setItem(STORAGE_KEYS.USER, username)
      setToken(token)
    },
    logout() {
      this.token = ''
      this.username = ''
      localStorage.removeItem(STORAGE_KEYS.TOKEN)
      localStorage.removeItem(STORAGE_KEYS.USER)
      setToken('')
    }
  }
})
