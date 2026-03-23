import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import { setToken } from './api'
import App from './App.vue'
import './styles.css'

// 恢复 token 到 axios headers（在应用初始化前执行）
const savedToken = localStorage.getItem('goai_token')
if (savedToken) {
  setToken(savedToken)
}

const messages = {
  zh: {
    newChat: '新建会话',
    send: '发送',
    placeholder: '输入你的问题...',
    provider: '厂商',
    model: '模型',
    temperature: '温度',
    topP: 'Top P',
    maxTokens: '最大Tokens'
  },
  en: {
    newChat: 'New Chat',
    send: 'Send',
    placeholder: 'Ask anything...',
    provider: 'Provider',
    model: 'Model',
    temperature: 'Temperature',
    topP: 'Top P',
    maxTokens: 'Max Tokens'
  }
}

const app = createApp(App)
const i18n = createI18n({
  locale: 'zh',
  fallbackLocale: 'en',
  messages
})

app.use(createPinia())
app.use(i18n)
app.mount('#app')
