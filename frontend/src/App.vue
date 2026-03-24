<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AuthScreen from './components/AuthScreen.vue'
import Sidebar from './components/Sidebar.vue'
import ChatPanel from './components/ChatPanel.vue'
import ApiKeyModal from './components/ApiKeyModal.vue'
import { useAuthStore } from './stores/auth'
import { useChatStore } from './stores/chat'
import type { Provider, ThemeMode } from './types'
import { isThemeMode, isUnauthorized, resolveTheme, STORAGE_KEYS, PROVIDER_CONFIG } from './utils'

const authStore = useAuthStore()
const chatStore = useChatStore()
const { locale } = useI18n()

// State
const provider = ref<Provider>('OPENAI')
const model = ref('gpt-4o-mini')
const temperature = ref(0.7)
const topP = ref(0.9)
const maxTokens = ref(1024)
const themeMode = ref<ThemeMode>('system')
const systemPrefersDark = ref(false)
const showApiKeys = ref(false)
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

async function handleSend(prompt: string, p: Provider, m: string, temp: number, tp: number, mt: number) {
  await chatStore.sendMessage(prompt, {
    provider: p,
    model: m,
    prompt,
    temperature: temp,
    topP: tp,
    maxTokens: mt
  })
}

function saveApiKeys(keys: Record<Provider, string>) {
  apiKeys.value = keys
  localStorage.setItem(STORAGE_KEYS.API_KEYS, JSON.stringify(keys))
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
    <Sidebar
      :theme-mode="themeMode"
      :is-dark-theme="isDarkTheme"
      @theme-change="setThemeMode"
      @show-api-keys="showApiKeys = true"
    />

    <ChatPanel
      v-model:provider="provider"
      v-model:model="model"
      v-model:temperature="temperature"
      v-model:top-p="topP"
      v-model:max-tokens="maxTokens"
      :api-keys="apiKeys"
      @send="handleSend"
      @show-api-keys="showApiKeys = true"
    />
  </div>

  <!-- API Key Modal -->
  <ApiKeyModal
    v-if="showApiKeys"
    :api-keys="apiKeys"
    @close="showApiKeys = false"
    @save="saveApiKeys"
  />
</template>

<style scoped>
.app {
  display: grid;
  grid-template-columns: 280px 1fr;
  height: 100vh;
  position: relative;
  z-index: 1;
}

@media (max-width: 768px) {
  .app {
    grid-template-columns: 1fr;
  }
}
</style>
