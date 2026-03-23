<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import MarkdownMessage from './components/MarkdownMessage.vue'
import AuthScreen from './components/AuthScreen.vue'
import { useAuthStore } from './stores/auth'
import { useChatStore } from './stores/chat'
import type { Provider, ThemeMode } from './types'
import { extractApiError, isThemeMode, isUnauthorized, resolveTheme, STORAGE_KEYS, PROVIDER_CONFIG, LIMITS } from './utils'

const authStore = useAuthStore()
const chatStore = useChatStore()
const { locale } = useI18n()

// Chat
const draft = ref('')
const provider = ref<Provider>('OPENAI')
const model = ref('gpt-4o-mini')
const temperature = ref(0.7)
const topP = ref(0.9)
const maxTokens = ref(1024)
const themeMode = ref<ThemeMode>('system')
const systemPrefersDark = ref(false)
const showApiKeys = ref(false)
const creatingConversation = ref(false)
const chatErrorMsg = ref('')
const errorMsg = ref('')

// API Keys
const apiKeys = ref<Record<Provider, string>>({
  OPENAI: '',
  QWEN: '',
  ERNIE: '',
  SPARK: '',
  CLAUDE: '',
  GEMINI: '',
  XIAOMI: ''
})

const isLoggedIn = computed(() => !!authStore.token)
const providers: Provider[] = ['OPENAI', 'QWEN', 'ERNIE', 'SPARK', 'CLAUDE', 'GEMINI', 'XIAOMI']

const currentProviderInfo = computed(() => PROVIDER_CONFIG[provider.value])
const resolvedTheme = computed<'light' | 'dark'>(() => resolveTheme(themeMode.value, systemPrefersDark.value))
const isDarkTheme = computed(() => resolvedTheme.value === 'dark')

let colorSchemeMediaQuery: MediaQueryList | null = null

function applyTheme(mode: 'light' | 'dark') {
  document.documentElement.classList.toggle('theme-light', mode === 'light')
}

function updateSystemTheme(event?: MediaQueryListEvent) {
  systemPrefersDark.value = event?.matches ?? colorSchemeMediaQuery?.matches ?? false
  applyTheme(resolvedTheme.value)
}

function setThemeMode(mode: ThemeMode) {
  themeMode.value = mode
  localStorage.setItem(STORAGE_KEYS.THEME_MODE, mode)
  localStorage.setItem(STORAGE_KEYS.THEME, resolveTheme(mode, systemPrefersDark.value))
  applyTheme(resolvedTheme.value)
}

async function handleLoginSuccess() {
  await chatStore.init()
}

async function send() {
  if (!draft.value.trim()) return
  
  const currentKey = apiKeys.value[provider.value]
  if (!currentKey) {
    showApiKeys.value = true
    errorMsg.value = `请先配置 ${PROVIDER_CONFIG[provider.value].name} 的 API Key`
    return
  }
  
  const prompt = draft.value
  draft.value = ''
  await chatStore.sendMessage(prompt, {
    provider: provider.value,
    model: model.value,
    prompt,
    temperature: temperature.value,
    topP: topP.value,
    maxTokens: maxTokens.value
  })
}

async function createNewConversation() {
  creatingConversation.value = true
  chatErrorMsg.value = ''
  try {
    await chatStore.newConversation('新对话')
  } catch (e: unknown) {
    if (isUnauthorized(e)) {
      authStore.logout()
      errorMsg.value = '登录已过期，请重新登录'
      return
    }
    chatErrorMsg.value = extractApiError(e, '创建新对话失败')
  } finally {
    creatingConversation.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

function logout() {
  authStore.logout()
}

function saveApiKeys() {
  localStorage.setItem(STORAGE_KEYS.API_KEYS, JSON.stringify(apiKeys.value))
  showApiKeys.value = false
}

function loadApiKeys() {
  const saved = localStorage.getItem(STORAGE_KEYS.API_KEYS)
  if (saved) {
    try {
      const parsed = JSON.parse(saved) as Record<Provider, string>
      apiKeys.value = { ...apiKeys.value, ...parsed }
    } catch {
      // ignore
    }
  }
}

function clearApiKey(provider: Provider) {
  apiKeys.value[provider] = ''
  localStorage.setItem(STORAGE_KEYS.API_KEYS, JSON.stringify(apiKeys.value))
}

onMounted(async () => {
  colorSchemeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  systemPrefersDark.value = colorSchemeMediaQuery.matches

  const savedThemeMode = localStorage.getItem(STORAGE_KEYS.THEME_MODE)
  const legacyTheme = localStorage.getItem(STORAGE_KEYS.THEME)

  if (isThemeMode(savedThemeMode)) {
    themeMode.value = savedThemeMode
  } else if (legacyTheme === 'dark' || legacyTheme === 'light') {
    themeMode.value = legacyTheme
  } else {
    themeMode.value = 'system'
  }

  applyTheme(resolvedTheme.value)
  colorSchemeMediaQuery.addEventListener('change', updateSystemTheme)
  
  loadApiKeys()
  
  authStore.restore()
  if (authStore.token) {
    try {
      await chatStore.init()
    } catch (e: unknown) {
      if (isUnauthorized(e)) {
        authStore.logout()
        errorMsg.value = '登录已过期，请重新登录'
      }
    }
  }
})

onBeforeUnmount(() => {
  colorSchemeMediaQuery?.removeEventListener('change', updateSystemTheme)
})
</script>

<template>
  <!-- Auth Screen -->
  <AuthScreen v-if="!isLoggedIn" @login-success="handleLoginSuccess" />

  <!-- Main App -->
  <div v-else class="app">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-top">
        <button class="new-chat-btn" :disabled="creatingConversation" @click="createNewConversation">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14"/>
          </svg>
          <span>新对话</span>
        </button>
        <div v-if="chatErrorMsg" class="chat-error-banner">{{ chatErrorMsg }}</div>
      </div>
      
      <div class="sidebar-content">
        <div class="sidebar-section-title">历史会话</div>
        <div class="conversation-list">
          <button
            v-for="conv in chatStore.conversations"
            :key="conv.id"
            :class="['conversation', { active: conv.id === chatStore.activeConversationId }]"
            @click="chatStore.openConversation(conv.id)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <span>{{ conv.title }}</span>
          </button>
        </div>
      </div>
      
      <div class="sidebar-footer">
        <div class="theme-mode-switcher" role="group" aria-label="主题模式切换">
          <button
            :class="['theme-mode-btn', { active: themeMode === 'light' }]"
            title="浅色模式"
            @click="setThemeMode('light')"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="5"/>
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
            </svg>
          </button>
          <button
            :class="['theme-mode-btn', { active: themeMode === 'system' }]"
            :title="`跟随系统（当前${isDarkTheme ? '深色' : '浅色'}）`"
            @click="setThemeMode('system')"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="14" rx="2"/>
              <path d="M8 20h8"/>
              <path d="M12 18v2"/>
            </svg>
          </button>
          <button
            :class="['theme-mode-btn', { active: themeMode === 'dark' }]"
            title="深色模式"
            @click="setThemeMode('dark')"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </button>
        </div>
        <button @click="showApiKeys = true" title="配置 API Keys">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
        </button>
        <button @click="logout" title="退出登录">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </button>
      </div>
    </aside>

    <!-- Chat Panel -->
    <main class="chat-panel">
      <!-- Provider Bar -->
      <div class="provider-bar">
        <button
          v-for="p in providers"
          :key="p"
          :class="['provider-tab', { active: p === provider, configured: apiKeys[p] }]"
          @click="provider = p"
        >
          <span class="provider-icon">{{ PROVIDER_CONFIG[p].icon }}</span>
          <span class="provider-name">{{ PROVIDER_CONFIG[p].name }}</span>
          <span v-if="apiKeys[p]" class="provider-status">✓</span>
        </button>
      </div>

      <!-- Model Bar -->
      <div class="model-bar">
        <select v-model="model" class="model-select">
          <option v-for="m in currentProviderInfo.models" :key="m" :value="m">{{ m }}</option>
        </select>
        <div class="model-params">
          <div class="param">
            <label>Temperature</label>
            <input v-model.number="temperature" type="range" min="0" max="2" step="0.1" />
            <span>{{ temperature }}</span>
          </div>
          <div class="param">
            <label>Top P</label>
            <input v-model.number="topP" type="range" min="0" max="1" step="0.05" />
            <span>{{ topP }}</span>
          </div>
          <div class="param">
            <label>Max Tokens</label>
            <input v-model.number="maxTokens" type="number" min="1" max="8192" class="param-input" />
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="chatStore.messages.length === 0" class="empty-state">
        <div class="empty-state-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <h2>开始对话</h2>
        <p>选择 AI 提供商，配置 API Key，开启智能对话</p>
        
        <div class="quick-providers">
          <button
            v-for="p in providers"
            :key="p"
            :class="['quick-provider', { configured: apiKeys[p] }]"
            @click="provider = p; showApiKeys = !apiKeys[p]"
          >
            <span class="quick-icon">{{ PROVIDER_CONFIG[p].icon }}</span>
            <span class="quick-name">{{ PROVIDER_CONFIG[p].name }}</span>
            <span class="quick-status">{{ apiKeys[p] ? '已配置' : '点击配置' }}</span>
          </button>
        </div>
      </div>

      <!-- Messages -->
      <div v-else class="messages">
        <div v-for="(msg, idx) in chatStore.messages" :key="idx" :class="['message', msg.role]">
          <div class="message-content">
            <div class="message-icon">
              {{ msg.role === 'user' ? '👤' : PROVIDER_CONFIG[provider].icon }}
            </div>
            <div class="message-text">
              <MarkdownMessage v-if="msg.role === 'assistant'" :content="msg.content" />
              <span v-else>{{ msg.content }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Composer -->
      <div class="composer-wrapper">
        <div class="composer">
          <textarea
            v-model="draft"
            placeholder="输入消息..."
            :disabled="chatStore.loading"
            @keydown="handleKeydown"
          ></textarea>
          <div class="composer-footer">
            <div class="composer-actions">
              <button 
                :class="['settings-trigger', { active: showApiKeys }]"
                @click="showApiKeys = !showApiKeys"
                title="API Key 配置"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="3"/>
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                </svg>
              </button>
              <span class="composer-hint">Enter 发送 · Shift+Enter 换行</span>
            </div>
            <button class="send-btn" :disabled="!draft.trim() || chatStore.loading" @click="send">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
              <span>发送</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>

  <!-- API Key Modal -->
  <div v-if="showApiKeys" class="modal-overlay" @click.self="showApiKeys = false">
    <div class="modal-panel">
      <div class="modal-header">
        <h3>配置 API Keys</h3>
        <button class="modal-close" @click="showApiKeys = false">&times;</button>
      </div>
      <div class="modal-body">
        <p class="modal-hint">配置各个 AI 提供商的 API Key，用于调用对应的模型服务。密钥仅保存在浏览器本地。</p>
        
        <div class="api-key-list">
          <div v-for="p in providers" :key="p" class="api-key-item">
            <div class="api-key-header">
              <span class="api-key-icon">{{ PROVIDER_CONFIG[p].icon }}</span>
              <span class="api-key-name">{{ PROVIDER_CONFIG[p].name }}</span>
              <span class="api-key-url">{{ PROVIDER_CONFIG[p].baseUrl }}</span>
            </div>
            <div class="api-key-input-wrapper">
              <input
                v-model="apiKeys[p]"
                type="password"
                class="api-key-input"
                :placeholder="`输入 ${PROVIDER_CONFIG[p].name} API Key`"
              />
              <button v-if="apiKeys[p]" class="api-key-clear" @click="clearApiKey(p)">&times;</button>
            </div>
          </div>
        </div>
        
        <div class="modal-actions">
          <button class="modal-btn secondary" @click="showApiKeys = false">取消</button>
          <button class="modal-btn primary" @click="saveApiKeys">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 厂商选择栏 ==================== */
.provider-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-secondary);
  overflow-x: auto;
}

.provider-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: transparent;
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  position: relative;
  overflow: hidden;
}

.provider-tab::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--gradient-primary);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.provider-tab:hover::before {
  opacity: 0.08;
}

.provider-tab:hover {
  border-color: var(--border-emphasis);
  color: var(--text-primary);
  transform: translateY(-1px);
}

.provider-tab.active {
  background: var(--bg-elevated);
  border-color: var(--accent-primary);
  color: var(--text-primary);
}

.provider-tab.active::before {
  opacity: 0.1;
}

.provider-tab.configured .provider-status {
  color: var(--accent-success);
}

.provider-icon {
  font-size: 16px;
  position: relative;
  z-index: 1;
}

.provider-name {
  position: relative;
  z-index: 1;
}

.provider-status {
  font-size: 11px;
  opacity: 0.7;
  position: relative;
  z-index: 1;
}

/* ==================== 模型参数栏 ==================== */
.model-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-secondary);
}

.model-select {
  min-width: 160px;
  padding: 8px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.model-select:hover {
  border-color: var(--border-emphasis);
}

.model-select:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.model-params {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.param {
  display: flex;
  align-items: center;
  gap: 10px;
}

.param label {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  white-space: nowrap;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.param input[type="range"] {
  width: 80px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--bg-tertiary);
  border-radius: 2px;
  outline: none;
}

.param input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px;
  height: 14px;
  background: var(--accent-primary);
  border-radius: 50%;
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.param input[type="range"]::-webkit-slider-thumb:hover {
  transform: scale(1.2);
}

.param span {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-primary);
  min-width: 35px;
}

.param-input {
  width: 80px;
  padding: 6px 10px;
  background: var(--bg-primary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-xs);
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 13px;
  transition: all var(--transition-fast);
}

.param-input:hover {
  border-color: var(--border-emphasis);
}

.param-input:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

/* ==================== 空状态 ==================== */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  text-align: center;
}

.empty-state-icon {
  width: 88px;
  height: 88px;
  margin-bottom: 28px;
  background: var(--gradient-primary);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-glow);
  animation: pulse 3s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: var(--shadow-glow); }
  50% { box-shadow: 0 0 32px rgba(99, 102, 241, 0.4), 0 0 64px rgba(6, 182, 212, 0.2); }
}

.empty-state-icon svg {
  width: 44px;
  height: 44px;
  stroke: white;
  stroke-width: 1.5;
}

.empty-state h2 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.empty-state p {
  font-size: 15px;
  color: var(--text-secondary);
  margin-bottom: 36px;
  max-width: 400px;
}

.quick-providers {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  max-width: 600px;
  width: 100%;
}

.quick-provider {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.quick-provider::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--gradient-primary);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.quick-provider:hover::before {
  opacity: 0.08;
}

.quick-provider:hover {
  border-color: var(--border-emphasis);
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.quick-provider.configured {
  border-color: var(--accent-primary);
}

.quick-provider.configured::after {
  content: '✓';
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  background: var(--accent-success);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: white;
  font-weight: bold;
}

.quick-icon {
  font-size: 28px;
  position: relative;
  z-index: 1;
}

.quick-name {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  position: relative;
  z-index: 1;
}

.quick-status {
  font-size: 11px;
  color: var(--text-muted);
  position: relative;
  z-index: 1;
}

.quick-provider.configured .quick-status {
  color: var(--accent-success);
}

/* ==================== 消息列表 ==================== */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  animation: messageIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.message-content {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.message.user .message-content {
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}

.message.assistant .message-content {
  background: transparent;
}

.message.assistant:hover .message-content {
  background: var(--bg-secondary);
}

.message-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: var(--gradient-cyber);
  box-shadow: var(--shadow-sm);
}

.message.user .message-icon {
  background: var(--gradient-primary);
}

.message-text {
  flex: 1;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-primary);
}

/* ==================== 输入区域 ==================== */
.composer-wrapper {
  padding: 16px 20px 20px;
  background: linear-gradient(180deg, transparent 0%, var(--bg-primary) 30%);
}

.composer {
  max-width: 800px;
  margin: 0 auto;
  background: var(--bg-secondary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-normal);
}

.composer:focus-within {
  border-color: var(--accent-primary);
  box-shadow: var(--shadow-glow);
}

.composer textarea {
  width: 100%;
  padding: 16px 20px;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-family: var(--font-body);
  font-size: 15px;
  line-height: 1.6;
  resize: none;
  min-height: 56px;
}

.composer textarea::placeholder {
  color: var(--text-muted);
}

.composer textarea:focus {
  outline: none;
}

.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border-subtle);
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.composer-hint {
  font-family: var(--font-display);
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.02em;
}

.settings-trigger {
  padding: 8px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.settings-trigger:hover {
  background: var(--bg-hover);
  color: var(--accent-primary);
  border-color: var(--accent-primary);
  transform: translateY(-1px);
}

.settings-trigger.active {
  background: var(--accent-primary);
  color: white;
  border-color: var(--accent-primary);
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.3);
}

.settings-trigger svg {
  width: 16px;
  height: 16px;
}

.send-btn {
  padding: 10px 20px;
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: white;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.3px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.send-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.1);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.send-btn:hover:not(:disabled)::before {
  opacity: 1;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn svg {
  width: 16px;
  height: 16px;
  position: relative;
  z-index: 1;
}

/* ==================== 弹窗 ==================== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-panel {
  width: min(560px, calc(100% - 40px));
  max-height: 80vh;
  background: var(--bg-secondary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-lg);
  animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid var(--border-subtle);
}

.modal-header h3 {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.modal-close {
  width: 36px;
  height: 36px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.modal-close:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
  border-color: var(--border-emphasis);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  max-height: calc(80vh - 80px);
}

.modal-hint {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  line-height: 1.6;
}

.api-key-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 28px;
}

.api-key-item {
  padding: 20px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.api-key-item:hover {
  border-color: var(--border-normal);
}

.api-key-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.api-key-icon {
  font-size: 22px;
}

.api-key-name {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.api-key-url {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
  margin-left: auto;
}

.api-key-input-wrapper {
  display: flex;
  gap: 10px;
}

.api-key-input {
  flex: 1;
  padding: 12px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 13px;
  transition: all var(--transition-fast);
}

.api-key-input::placeholder {
  color: var(--text-muted);
}

.api-key-input:hover {
  border-color: var(--border-emphasis);
}

.api-key-input:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.api-key-clear {
  width: 40px;
  background: var(--bg-primary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.api-key-clear:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.4);
  color: var(--accent-danger);
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.modal-btn {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.modal-btn.secondary {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-normal);
  color: var(--text-primary);
}

.modal-btn.secondary:hover {
  background: var(--bg-hover);
  border-color: var(--border-emphasis);
}

.modal-btn.primary {
  background: var(--gradient-primary);
  border: none;
  color: white;
}

.modal-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.35);
}

/* ==================== 登录页面 ==================== */
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
