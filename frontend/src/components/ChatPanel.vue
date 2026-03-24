<script setup lang="ts">
import { ref, computed } from 'vue'
import MarkdownMessage from './MarkdownMessage.vue'
import { useChatStore } from '../stores/chat'
import type { Provider } from '../types'
import { PROVIDER_CONFIG } from '../utils'

const chatStore = useChatStore()

const emit = defineEmits<{
  (e: 'send', prompt: string, provider: Provider, model: string, temperature: number, topP: number, maxTokens: number): void
  (e: 'show-api-keys'): void
}>()

const props = defineProps<{
  provider: Provider
  model: string
  temperature: number
  topP: number
  maxTokens: number
  apiKeys: Record<Provider, string>
}>()

const draft = ref('')
const providers: Provider[] = ['OPENAI', 'QWEN', 'ERNIE', 'SPARK', 'CLAUDE', 'GEMINI', 'XIAOMI']

const currentProviderInfo = computed(() => PROVIDER_CONFIG[props.provider])

function send() {
  if (!draft.value.trim()) return
  
  const currentKey = props.apiKeys[props.provider]
  if (!currentKey) {
    emit('show-api-keys')
    return
  }
  
  const prompt = draft.value
  draft.value = ''
  emit('send', prompt, props.provider, props.model, props.temperature, props.topP, props.maxTokens)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}
</script>

<template>
  <main class="chat-panel">
    <!-- Provider Bar -->
    <div class="provider-bar">
      <button
        v-for="p in providers"
        :key="p"
        :class="['provider-tab', { active: p === provider, configured: apiKeys[p] }]"
        @click="$emit('update:provider', p)"
      >
        <span class="provider-icon">{{ PROVIDER_CONFIG[p].icon }}</span>
        <span class="provider-name">{{ PROVIDER_CONFIG[p].name }}</span>
        <span v-if="apiKeys[p]" class="provider-status">✓</span>
      </button>
    </div>

    <!-- Model Bar -->
    <div class="model-bar">
      <select :value="model" @change="$emit('update:model', ($event.target as HTMLSelectElement).value)" class="model-select">
        <option v-for="m in currentProviderInfo.models" :key="m" :value="m">{{ m }}</option>
      </select>
      <div class="model-params">
        <div class="param">
          <label>Temperature</label>
          <input :value="temperature" @input="$emit('update:temperature', Number(($event.target as HTMLInputElement).value))" type="range" min="0" max="2" step="0.1" />
          <span>{{ temperature }}</span>
        </div>
        <div class="param">
          <label>Top P</label>
          <input :value="topP" @input="$emit('update:topP', Number(($event.target as HTMLInputElement).value))" type="range" min="0" max="1" step="0.05" />
          <span>{{ topP }}</span>
        </div>
        <div class="param">
          <label>Max Tokens</label>
          <input :value="maxTokens" @input="$emit('update:maxTokens', Number(($event.target as HTMLInputElement).value))" type="number" min="1" max="8192" class="param-input" />
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
          @click="$emit('update:provider', p); if (!apiKeys[p]) $emit('show-api-keys')"
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
              class="settings-trigger"
              @click="$emit('show-api-keys')"
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
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: transparent;
  overflow: hidden;
  position: relative;
}

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
</style>
