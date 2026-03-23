import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AuthScreen from './AuthScreen.vue'
import { useAuthStore } from '../stores/auth'

vi.mock('../stores/auth', () => ({
  useAuthStore: vi.fn()
}))

describe('AuthScreen', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders login form with title and subtitle', () => {
    const mockAuthStore = {
      signIn: vi.fn(),
      signUp: vi.fn()
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)

    expect(wrapper.find('.auth-title').text()).toBe('GO-AI Platform')
    expect(wrapper.find('.auth-subtitle').text()).toContain('多厂商 AI 对话平台')
    expect(wrapper.find('input[placeholder="用户名"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="密码"]').exists()).toBe(true)
  })

  it('shows all provider badges', () => {
    const mockAuthStore = {
      signIn: vi.fn(),
      signUp: vi.fn()
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)
    const providerBadges = wrapper.findAll('.provider-badge')

    expect(providerBadges.length).toBe(7)
    expect(providerBadges[0].text()).toContain('OpenAI')
    expect(providerBadges[1].text()).toContain('通义千问')
  })

  it('calls signIn when form is submitted with username and password', async () => {
    const mockAuthStore = {
      signIn: vi.fn().mockResolvedValue(undefined),
      signUp: vi.fn()
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)

    await wrapper.find('input[placeholder="用户名"]').setValue('testuser')
    await wrapper.find('input[placeholder="密码"]').setValue('testpass')
    await wrapper.find('form').trigger('submit')

    expect(mockAuthStore.signIn).toHaveBeenCalledWith('testuser', 'testpass')
  })

  it('calls signUp when register button is clicked', async () => {
    const mockAuthStore = {
      signIn: vi.fn(),
      signUp: vi.fn().mockResolvedValue(undefined)
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)

    await wrapper.find('input[placeholder="用户名"]').setValue('newuser')
    await wrapper.find('input[placeholder="密码"]').setValue('newpass')
    await wrapper.find('button.auth-btn.secondary').trigger('click')

    expect(mockAuthStore.signUp).toHaveBeenCalledWith('newuser', 'newpass')
  })

  it('shows error message when login fails', async () => {
    const mockAuthStore = {
      signIn: vi.fn().mockRejectedValue(new Error('Invalid credentials')),
      signUp: vi.fn()
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)

    await wrapper.find('input[placeholder="用户名"]').setValue('wronguser')
    await wrapper.find('input[placeholder="密码"]').setValue('wrongpass')
    await wrapper.find('form').trigger('submit')

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.auth-error').exists()).toBe(true)
    expect(wrapper.find('.auth-error').text()).toContain('Invalid credentials')
  })

  it('validates empty username and password', async () => {
    const mockAuthStore = {
      signIn: vi.fn(),
      signUp: vi.fn()
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)

    await wrapper.find('form').trigger('submit')

    expect(mockAuthStore.signIn).not.toHaveBeenCalled()
    expect(wrapper.find('.auth-error').text()).toBe('请输入用户名和密码')
  })

  it('disables form inputs when loading', async () => {
    const mockAuthStore = {
      signIn: vi.fn().mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100))),
      signUp: vi.fn()
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)

    await wrapper.find('input[placeholder="用户名"]').setValue('testuser')
    await wrapper.find('input[placeholder="密码"]').setValue('testpass')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('input[placeholder="用户名"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('input[placeholder="密码"]').attributes('disabled')).toBeDefined()
  })

  it('emits login-success after successful sign in', async () => {
    const mockAuthStore = {
      signIn: vi.fn().mockResolvedValue(undefined),
      signUp: vi.fn()
    }
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any)

    const wrapper = mount(AuthScreen)

    await wrapper.find('input[placeholder="用户名"]').setValue('testuser')
    await wrapper.find('input[placeholder="密码"]').setValue('testpass')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('login-success')).toBeTruthy()
  })
})
