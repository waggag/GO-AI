import axios from 'axios'
import type { AuthResponse, Conversation, ConversationDetail, StreamPayload } from './types'
import { STORAGE_KEYS } from './utils'

const client = axios.create({
  baseURL: '/api',
  allowAbsoluteUrls: false
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN)
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function setToken(token: string) {
  if (!token) {
    delete client.defaults.headers.common.Authorization
    return
  }
  client.defaults.headers.common.Authorization = `Bearer ${token}`
}

export async function register(username: string, password: string): Promise<AuthResponse> {
  const { data } = await client.post('/auth/register', { username, password })
  return data as AuthResponse
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const { data } = await client.post('/auth/login', { username, password })
  return data as AuthResponse
}

export async function createConversation(title: string): Promise<Conversation> {
  const { data } = await client.post('/chat/conversations', { title })
  return data as Conversation
}

export async function listConversations(): Promise<Conversation[]> {
  const { data } = await client.get('/chat/conversations')
  return data as Conversation[]
}

export async function loadConversation(id: number): Promise<ConversationDetail> {
  const { data } = await client.get(`/chat/conversations/${id}`)
  return data as ConversationDetail
}

export async function streamChat(
  conversationId: number,
  payload: StreamPayload,
  onChunk: (content: string, done: boolean) => void
): Promise<void> {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN) ?? ''
  const response = await fetch(`/api/chat/stream?conversationId=${conversationId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  })

  if (!response.ok) {
    const errorText = await response.text().catch(() => 'Unknown error')
    throw new Error(`Stream failed (${response.status}): ${errorText}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('No response body')
  }

  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() ?? ''

      for (const event of events) {
        const line = event.split('\n').find((entry) => entry.startsWith('data:'))
        if (!line) {
          continue
        }
        try {
          const payloadObject = JSON.parse(line.replace('data:', '').trim()) as { delta: string; done: boolean }
          onChunk(payloadObject.delta, payloadObject.done)
        } catch (parseError) {
          console.warn('Failed to parse SSE event:', line, parseError)
        }
      }
    }
  } finally {
    reader.releaseLock()
  }
}
