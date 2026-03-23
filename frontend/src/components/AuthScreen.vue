<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { extractApiError, PROVIDER_CONFIG } from '../utils'
import type { Provider } from '../types'

const authStore = useAuthStore()
const emit = defineEmits(['login-success'])

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

const providers: Provider[] = ['OPENAI', 'QWEN', 'ERNIE', 'SPARK', 'CLAUDE', 'GEMINI', 'XIAOMI']

async function signIn() {
  if (!username.value || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await authStore.signIn(username.value, password.value)
    emit('login-success')
  } catch (e: unknown) {
    errorMsg.value = extractApiError(e, '登录失败')
  } finally {
    loading.value = false
  }
}

async function signUp() {
  if (!username.value || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await authStore.signUp(username.value, password.value)
    emit('login-success')
  } catch (e: unknown) {
    errorMsg.value = extractApiError(e, '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <h1 class="auth-title">GO-AI Platform</h1>
        <p class="auth-subtitle">多厂商 AI 对话平台，统一调用，自由切换</p>
      </div>
      
      <div class="auth-providers">
        <span v-for="p in providers" :key="p" class="provider-badge">
          {{ PROVIDER_CONFIG[p].icon }} {{ PROVIDER_CONFIG[p].name }}
        </span>
      </div>
      
      <form class="auth-form" @submit.prevent="signIn">
        <input v-model="username" class="auth-input" placeholder="用户名" :disabled="loading" autocomplete="username" />
        <input v-model="password" type="password" class="auth-input" placeholder="密码" :disabled="loading" autocomplete="current-password" @keyup.enter="signIn" />
        
        <div v-if="errorMsg" class="auth-error">{{ errorMsg }}</div>
        
        <div class="auth-actions">
          <button type="button" class="auth-btn secondary" :disabled="loading" @click="signUp">
            {{ loading ? '处理中...' : '注册' }}
          </button>
          <button type="submit" class="auth-btn primary" :disabled="loading">
            {{ loading ? '处理中...' : '登录' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
  padding: 20px;
  position: relative;
}

.auth-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(99, 102, 241, 0.15) 0%, transparent 60%),
    radial-gradient(ellipse 60% 40% at -10% 50%, rgba(6, 182, 212, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse 50% 35% at 110% 70%, rgba(236, 72, 153, 0.08) 0%, transparent 50%);
  pointer-events: none;
}

.auth-card {
  width: min(440px, 90vw);
  padding: 48px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-lg);
  position: relative;
  z-index: 1;
  animation: cardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}

.auth-logo {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
  background: var(--gradient-primary);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-glow);
  animation: logoFloat 4s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.auth-logo svg {
  width: 36px;
  height: 36px;
  stroke: white;
}

.auth-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-subtitle {
  font-size: 15px;
  color: var(--text-secondary);
}

.auth-providers {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 28px;
}

.provider-badge {
  padding: 8px 14px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: 20px;
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.provider-badge:hover {
  border-color: var(--border-emphasis);
  color: var(--text-primary);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-input {
  width: 100%;
  padding: 14px 16px;
  background: var(--bg-primary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 15px;
  transition: all var(--transition-fast);
}

.auth-input::placeholder {
  color: var(--text-muted);
}

.auth-input:hover {
  border-color: var(--border-emphasis);
}

.auth-input:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.auth-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-error {
  padding: 14px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: var(--radius-md);
  color: var(--accent-danger);
  font-size: 14px;
  text-align: center;
}

.auth-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.auth-btn {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.3px;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.auth-btn.primary {
  background: var(--gradient-primary);
  color: white;
  position: relative;
  overflow: hidden;
}

.auth-btn.primary::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.1);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.auth-btn.primary:hover:not(:disabled)::before {
  opacity: 1;
}

.auth-btn.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
}

.auth-btn.secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-normal);
}

.auth-btn.secondary:hover:not(:disabled) {
  background: var(--bg-hover);
  border-color: var(--border-emphasis);
  transform: translateY(-1px);
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
