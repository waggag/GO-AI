<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import type { ThemeMode } from '../types'
import { extractApiError, isUnauthorized } from '../utils'

const authStore = useAuthStore()
const chatStore = useChatStore()

const emit = defineEmits<{
  (e: 'theme-change', mode: ThemeMode): void
  (e: 'show-api-keys'): void
}>()

const props = defineProps<{
  themeMode: ThemeMode
  isDarkTheme: boolean
}>()

const creatingConversation = ref(false)
const chatErrorMsg = ref('')

async function createNewConversation() {
  creatingConversation.value = true
  chatErrorMsg.value = ''
  try {
    await chatStore.newConversation('新对话')
  } catch (e: unknown) {
    if (isUnauthorized(e)) {
      authStore.logout()
      return
    }
    chatErrorMsg.value = extractApiError(e, '创建新对话失败')
  } finally {
    creatingConversation.value = false
  }
}

function setThemeMode(mode: ThemeMode) {
  emit('theme-change', mode)
}

function logout() {
  authStore.logout()
}
</script>

<template>
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
      <button @click="$emit('show-api-keys')" title="配置 API Keys">
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
</template>

<style scoped>
.sidebar {
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-normal);
  display: flex;
  flex-direction: column;
  height: 100vh;
  backdrop-filter: blur(12px);
  position: relative;
}

.sidebar-top {
  padding: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.new-chat-btn {
  width: 100%;
  padding: 14px 18px;
  background: var(--gradient-glass);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.3px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.new-chat-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--gradient-primary);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.new-chat-btn:hover::before {
  opacity: 0.12;
}

.new-chat-btn:hover {
  border-color: var(--accent-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-glow);
}

.new-chat-btn svg {
  width: 18px;
  height: 18px;
  position: relative;
  z-index: 1;
}

.new-chat-btn span {
  position: relative;
  z-index: 1;
}

.new-chat-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.new-chat-btn:disabled:hover::before {
  opacity: 0;
}

.chat-error-banner {
  margin-top: 12px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.08);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  font-size: 12px;
  font-family: var(--font-display);
  color: var(--text-primary);
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.sidebar-section-title {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 10px 10px 6px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conversation {
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

.conversation:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
  transform: translateX(2px);
}

.conversation.active {
  background: var(--bg-elevated);
  border-color: var(--border-emphasis);
  color: var(--text-primary);
}

.conversation.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 50%;
  background: var(--gradient-primary);
  border-radius: 0 2px 2px 0;
}

.conversation svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  opacity: 0.6;
}

.conversation span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.sidebar-footer {
  padding: 14px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  gap: 8px;
}

.sidebar-footer button {
  flex: 1;
  padding: 10px;
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

.sidebar-footer button:hover {
  background: var(--bg-hover);
  border-color: var(--border-emphasis);
  color: var(--text-primary);
  transform: translateY(-1px);
}

.sidebar-footer button svg {
  width: 16px;
  height: 16px;
}

.theme-mode-switcher {
  flex: 1.6;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 4px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-normal);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.sidebar-footer .theme-mode-btn {
  flex: initial;
  min-width: 0;
  padding: 8px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
}

.sidebar-footer .theme-mode-btn:hover {
  background: var(--bg-hover);
  border-color: var(--border-subtle);
  color: var(--text-primary);
  transform: none;
}

.sidebar-footer .theme-mode-btn.active {
  background: var(--gradient-glass);
  border-color: var(--border-emphasis);
  color: var(--text-primary);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.12), 0 4px 12px rgba(99, 102, 241, 0.16);
}

.sidebar-footer .theme-mode-btn.active svg {
  filter: drop-shadow(0 0 10px rgba(99, 102, 241, 0.25));
}
</style>
