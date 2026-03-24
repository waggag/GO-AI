<script setup lang="ts">
import { ref } from 'vue'
import type { Provider } from '../types'
import { PROVIDER_CONFIG } from '../utils'

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', keys: Record<Provider, string>): void
}>()

const props = defineProps<{
  apiKeys: Record<Provider, string>
}>()

const providers: Provider[] = ['OPENAI', 'QWEN', 'ERNIE', 'SPARK', 'CLAUDE', 'GEMINI', 'XIAOMI']
const localKeys = ref<Record<Provider, string>>({ ...props.apiKeys })

function clearApiKey(provider: Provider) {
  localKeys.value[provider] = ''
}

function save() {
  emit('save', localKeys.value)
}
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-panel">
      <div class="modal-header">
        <h3>配置 API Keys</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
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
                v-model="localKeys[p]"
                type="password"
                class="api-key-input"
                :placeholder="`输入 ${PROVIDER_CONFIG[p].name} API Key`"
              />
              <button v-if="localKeys[p]" class="api-key-clear" @click="clearApiKey(p)">&times;</button>
            </div>
          </div>
        </div>
        
        <div class="modal-actions">
          <button class="modal-btn secondary" @click="$emit('close')">取消</button>
          <button class="modal-btn primary" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
</style>
