import { defineStore } from 'pinia'
import { createConversation, listConversations, loadConversation, streamChat } from '../api'
import type { ChatMessage, Conversation, StreamPayload } from '../types'
import { LIMITS } from '../utils'

export const useChatStore = defineStore('chat', {
  state: () => ({
    conversations: [] as Conversation[],
    activeConversationId: 0,
    messages: [] as ChatMessage[],
    loading: false,
    queue: [] as Array<{ prompt: string; options: StreamPayload }>
  }),
  actions: {
    async init() {
      this.conversations = await listConversations()
      if (this.conversations.length > 0) {
        await this.openConversation(this.conversations[0].id)
      }
    },
    async newConversation(title: string) {
      const safeTitle = title.trim() || ('New Chat ' + Date.now())
      const conversation = await createConversation(safeTitle)
      this.conversations.unshift(conversation)
      this.activeConversationId = conversation.id
      this.messages = []
    },
    async openConversation(id: number) {
      this.activeConversationId = id
      const detail = await loadConversation(id)
      this.messages = detail.messages
    },
    async sendMessage(prompt: string, options: StreamPayload) {
      this.queue.push({ prompt, options })
      if (this.loading) {
        return
      }
      while (this.queue.length > 0) {
        const task = this.queue.shift()
        if (!task) {
          break
        }
        await this.processMessage(task.prompt, task.options)
      }
    },
    async processMessage(prompt: string, options: StreamPayload) {
      if (!this.activeConversationId) {
        await this.newConversation(prompt.slice(0, LIMITS.MAX_TITLE_LENGTH) || '新会话')
      }
      this.messages.push({ role: 'user', content: prompt })
      this.messages.push({ role: 'assistant', content: '' })
      this.loading = true
      try {
        await streamChat(this.activeConversationId, { ...options, prompt }, (chunk, done) => {
          const target = this.messages[this.messages.length - 1]
          target.content += chunk
          if (done) {
            this.loading = false
          }
        })
      } finally {
        this.loading = false
      }
    }
  }
})
