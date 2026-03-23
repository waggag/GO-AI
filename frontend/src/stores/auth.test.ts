import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const loginMock = vi.hoisted(() => vi.fn())
const registerMock = vi.hoisted(() => vi.fn())
const setTokenMock = vi.hoisted(() => vi.fn())

vi.mock('../api', () => ({
  login: loginMock,
  register: registerMock,
  setToken: setTokenMock
}))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('signIn should update token and username', async () => {
    const { useAuthStore } = await import('./auth')
    loginMock.mockResolvedValue({ token: 't1', username: 'alice', role: 'USER' })
    const store = useAuthStore()

    await store.signIn('alice', 'pwd')

    expect(store.token).toBe('t1')
    expect(store.username).toBe('alice')
    expect(setTokenMock).toHaveBeenCalledWith('t1')
  })

  it('signUp and logout should work', async () => {
    const { useAuthStore } = await import('./auth')
    registerMock.mockResolvedValue({ token: 't2', username: 'bob', role: 'USER' })
    const store = useAuthStore()

    await store.signUp('bob', 'pwd')
    store.logout()

    expect(store.token).toBe('')
    expect(localStorage.getItem('goai_token')).toBeNull()
  })

  it('restore should set request token', async () => {
    const { useAuthStore } = await import('./auth')
    localStorage.setItem('goai_token', 't3')
    localStorage.setItem('goai_user', 'neo')
    const store = useAuthStore()

    store.restore()

    expect(setTokenMock).toHaveBeenCalledWith('t3')
  })
})
